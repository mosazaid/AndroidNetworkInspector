package com.mousa.networkinspector.domain

data class NetworkCallDetails(
    val id: Long,
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

