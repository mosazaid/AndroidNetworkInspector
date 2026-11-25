package com.iiab.networkinspector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_calls")
data class NetworkCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val scheme: String,
    val requestHeaders: String,
    val responseHeaders: String,
    val requestBody: String?,
    val responseBody: String?,
    val statusCode: Int?,
    val isSuccessful: Boolean?,
    val errorType: String?,
    val errorMessage: String?,
    val requestTimeMillis: Long,
    val responseTimeMillis: Long?,
    val durationMillis: Long?,
    val requestSizeBytes: Long?,
    val responseSizeBytes: Long?
)

