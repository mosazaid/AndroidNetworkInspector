package com.iiab.networkinspector.domain

data class NetworkCall(
    val id: Long,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val scheme: String,
    val statusCode: Int?,
    val isSuccessful: Boolean?,
    val errorMessage: String?,
    val requestTimeMillis: Long,
    val responseTimeMillis: Long?,
    val durationMillis: Long?
)

