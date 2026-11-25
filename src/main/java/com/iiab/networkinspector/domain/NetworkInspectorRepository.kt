package com.iiab.networkinspector.domain

import kotlinx.coroutines.flow.Flow

interface NetworkInspectorRepository {
    suspend fun saveCall(details: NetworkCallDetails): Long
    suspend fun updateCallError(id: Long, errorType: String?, errorMessage: String?)
    fun observeCalls(limit: Int = 200): Flow<List<NetworkCall>>
    fun observeCallDetails(id: Long): Flow<NetworkCallDetails?>
    suspend fun clearAll()
}

