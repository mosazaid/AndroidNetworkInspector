package com.iiab.networkinspector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkCallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(call: NetworkCallEntity): Long

    @Query("UPDATE network_calls SET isSuccessful = 0, errorType = :errorType, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateCallError(id: Long, errorType: String?, errorMessage: String?)

    @Query("SELECT * FROM network_calls ORDER BY id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<NetworkCallEntity>>

    @Query("DELETE FROM network_calls WHERE id NOT IN (SELECT id FROM network_calls ORDER BY id DESC LIMIT :limit)")
    suspend fun trimToLatest(limit: Int)

    @Query("SELECT * FROM network_calls WHERE id = :id")
    fun observeById(id: Long): Flow<NetworkCallEntity?>

    @Query("DELETE FROM network_calls")
    suspend fun clearAll()
}

