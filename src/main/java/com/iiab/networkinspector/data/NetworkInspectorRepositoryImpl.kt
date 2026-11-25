package com.iiab.networkinspector.data

import com.iiab.networkinspector.domain.NetworkCall
import com.iiab.networkinspector.domain.NetworkCallDetails
import com.iiab.networkinspector.domain.NetworkInspectorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DEFAULT_MAX_SIZE = 200

class NetworkInspectorRepositoryImpl(
    private val dao: NetworkCallDao
) : NetworkInspectorRepository {

    override suspend fun saveCall(details: NetworkCallDetails): Long {
        val entity = NetworkCallEntity(
            id = 0L,
            method = details.method,
            url = details.url,
            host = details.host,
            path = details.path,
            scheme = details.scheme,
            requestHeaders = details.requestHeaders,
            responseHeaders = details.responseHeaders,
            requestBody = details.requestBody,
            responseBody = details.responseBody,
            statusCode = details.statusCode,
            isSuccessful = details.isSuccessful,
            errorType = details.errorType,
            errorMessage = details.errorMessage,
            requestTimeMillis = details.requestTimeMillis,
            responseTimeMillis = details.responseTimeMillis,
            durationMillis = details.durationMillis,
            requestSizeBytes = details.requestSizeBytes,
            responseSizeBytes = details.responseSizeBytes
        )
        val insertedId = dao.insert(entity)
        dao.trimToLatest(DEFAULT_MAX_SIZE)
        return insertedId
    }

    override suspend fun updateCallError(id: Long, errorType: String?, errorMessage: String?) {
        dao.updateCallError(id = id, errorType = errorType, errorMessage = errorMessage)
    }

    override fun observeCalls(limit: Int): Flow<List<NetworkCall>> {
        return dao.observeLatest(limit).map { list ->
            list.map { entity ->
                NetworkCall(
                    id = entity.id,
                    method = entity.method,
                    url = entity.url,
                    host = entity.host,
                    path = entity.path,
                    scheme = entity.scheme,
                    statusCode = entity.statusCode,
                    isSuccessful = entity.isSuccessful,
                    errorMessage = entity.errorMessage,
                    requestTimeMillis = entity.requestTimeMillis,
                    responseTimeMillis = entity.responseTimeMillis,
                    durationMillis = entity.durationMillis
                )
            }
        }
    }

    override fun observeCallDetails(id: Long): Flow<NetworkCallDetails?> {
        return dao.observeById(id).map { entity ->
            entity?.let {
                NetworkCallDetails(
                    id = it.id,
                    method = it.method,
                    url = it.url,
                    host = it.host,
                    path = it.path,
                    scheme = it.scheme,
                    requestHeaders = it.requestHeaders,
                    responseHeaders = it.responseHeaders,
                    requestBody = it.requestBody,
                    responseBody = it.responseBody,
                    statusCode = it.statusCode,
                    isSuccessful = it.isSuccessful,
                    errorType = it.errorType,
                    errorMessage = it.errorMessage,
                    requestTimeMillis = it.requestTimeMillis,
                    responseTimeMillis = it.responseTimeMillis,
                    durationMillis = it.durationMillis,
                    requestSizeBytes = it.requestSizeBytes,
                    responseSizeBytes = it.responseSizeBytes
                )
            }
        }
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}

