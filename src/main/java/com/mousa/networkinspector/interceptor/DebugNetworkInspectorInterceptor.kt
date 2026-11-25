package com.mousa.networkinspector.interceptor

import android.util.Log
import com.mousa.networkinspector.domain.NetworkCallDetails
import com.mousa.networkinspector.domain.NetworkInspectorRepository
import com.mousa.networkinspector.notification.NetworkInspectorNotifier
import java.nio.charset.Charset
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

private const val MAX_BODY_BYTES: Long = 250_000L
private const val MAX_ERROR_MESSAGE_LENGTH = 500

class DebugNetworkInspectorInterceptor(
        private val repository: NetworkInspectorRepository,
        private val notifier: NetworkInspectorNotifier
) : Interceptor {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeCalls = ThreadLocal<ArrayDeque<CallCaptureState>>()
    private val lastSavedIdByThread = ThreadLocal<Deferred<Long?>?>()

    /**
     * Attaches both the outermost guard (index 0, capturing pre-network/early-interceptor failures)
     * and this inner interceptor (end of chain, capturing final headers and raw responses) to
     * [builder].
     */
    fun attachTo(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.interceptors().add(0, outerInterceptor())
        builder.addInterceptor(this)
        return builder
    }

    /**
     * Outermost interceptor placed before header/auth/response interceptors so failures that
     * short-circuit the chain or throw after proceeding are still captured with full context.
     */
    fun outerInterceptor(): Interceptor = Interceptor { chain ->
        val stack = activeCalls.get() ?: ArrayDeque<CallCaptureState>().also { activeCalls.set(it) }
        val initialRequest = chain.request()
        val state =
                CallCaptureState(
                        startTimeNanos = System.nanoTime(),
                        startTimeMillis = System.currentTimeMillis(),
                        initialRequest = initialRequest
                )
        stack.addLast(state)
        lastSavedIdByThread.remove()

        var caught: Throwable? = null
        try {
            chain.proceed(initialRequest)
        } catch (t: Throwable) {
            caught = t
            throw t
        } finally {
            stack.pollLast()
            if (stack.isEmpty()) {
                activeCalls.remove()
            }
            val finalDetails = buildFinalDetails(state, caught)
            val saveDeferred = persistAsync(finalDetails)
            lastSavedIdByThread.set(saveDeferred)
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val currentState = activeCalls.get()?.lastOrNull()
        if (currentState == null) {
            lastSavedIdByThread.remove()
        }

        val requestTimeNanos = currentState?.startTimeNanos ?: System.nanoTime()
        val requestTimeMillis = currentState?.startTimeMillis ?: System.currentTimeMillis()
        val baseDetails = createBaseDetails(request, requestTimeMillis)

        return try {
            val response = chain.proceed(request)
            val responseTimeNanos = System.nanoTime()
            val responseTimeMillis = System.currentTimeMillis()
            val durationNanos = responseTimeNanos - requestTimeNanos
            val responseBodyString = response.safePeekBodyAsString(MAX_BODY_BYTES)
            val responseHeadersString =
                    response.headers.joinToString(separator = "\n") { header ->
                        "${header.first}: ${header.second}"
                    }

            val completedDetails =
                    baseDetails.copy(
                            responseHeaders = responseHeadersString,
                            responseBody = responseBodyString,
                            statusCode = response.code,
                            isSuccessful = response.isSuccessful,
                            requestTimeMillis = requestTimeMillis,
                            responseTimeMillis = responseTimeMillis,
                            durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos),
                            responseSizeBytes = response.body?.contentLength()?.takeIf { it >= 0L }
                                            ?: responseBodyString?.toByteArray()?.size?.toLong()
                    )

            if (currentState != null) {
                currentState.details = completedDetails
            } else {
                lastSavedIdByThread.set(persistAsync(completedDetails))
            }

            response
        } catch (t: Throwable) {
            val responseTimeNanos = System.nanoTime()
            val responseTimeMillis = System.currentTimeMillis()
            val durationNanos = responseTimeNanos - requestTimeNanos
            val errorDetails =
                    baseDetails.copy(
                            responseBody = buildHttpFailureResponseBody(t),
                            statusCode = null,
                            isSuccessful = false,
                            errorType = t::class.java.simpleName.ifEmpty { t::class.java.name },
                            errorMessage = t.message,
                            responseTimeMillis = responseTimeMillis,
                            durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos)
                    )
            if (currentState != null) {
                currentState.details = errorDetails
            } else {
                lastSavedIdByThread.set(persistAsync(errorDetails))
            }
            throw t
        }
    }

    /**
     * Marks the most recently recorded call on the current thread as failed when post-OkHttp
     * response parsing (e.g., Gson MalformedJsonException / JsonSyntaxException) throws.
     */
    fun recordConverterFailure(throwable: Throwable) {
        val saveDeferred = lastSavedIdByThread.get() ?: return
        lastSavedIdByThread.remove()
        ioScope.launch {
            val id = runCatching { saveDeferred.await() }.getOrNull() ?: return@launch
            val errorType = throwable::class.java.simpleName.ifEmpty { throwable::class.java.name }
            val errorMessage = throwable.message?.take(MAX_ERROR_MESSAGE_LENGTH) ?: errorType
            runCatching {
                repository.updateCallError(
                        id = id,
                        errorType = errorType,
                        errorMessage = errorMessage
                )
            }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to update converter error on call id=$id", error)
                    }
        }
    }

    private fun buildFinalDetails(state: CallCaptureState, caught: Throwable?): NetworkCallDetails {
        val existing = state.details
        if (existing == null) {
            val nowNanos = System.nanoTime()
            val nowMillis = System.currentTimeMillis()
            val base = createBaseDetails(state.initialRequest, state.startTimeMillis)
            val failure = caught ?: IllegalStateException("Network call aborted before completion")
            val errorType = failure::class.java.simpleName.ifEmpty { failure::class.java.name }
            return base.copy(
                    responseBody = buildHttpFailureResponseBody(failure),
                    statusCode = null,
                    isSuccessful = false,
                    errorType = errorType,
                    errorMessage = failure.message?.take(MAX_ERROR_MESSAGE_LENGTH) ?: errorType,
                    responseTimeMillis = nowMillis,
                    durationMillis = TimeUnit.NANOSECONDS.toMillis(nowNanos - state.startTimeNanos)
            )
        }

        if (caught == null) {
            return existing
        }

        val errorType = caught::class.java.simpleName.ifEmpty { caught::class.java.name }
        val rawMsg = caught.message?.trim()
        val conciseMessage =
                when {
                    rawMsg.isNullOrEmpty() -> errorType
                    existing.responseBody != null && rawMsg == existing.responseBody.trim() ->
                            "$errorType (HTTP ${existing.statusCode ?: "-"})"
                    rawMsg.length > MAX_ERROR_MESSAGE_LENGTH ->
                            rawMsg.take(MAX_ERROR_MESSAGE_LENGTH) + "…"
                    else -> rawMsg
                }
        val preservedBody =
                existing.responseBody?.takeIf { it.isNotBlank() }
                        ?: caught.message?.takeIf { it.isNotBlank() }
                                ?: buildHttpFailureResponseBody(caught)

        return existing.copy(
                responseBody = preservedBody,
                isSuccessful = false,
                errorType = existing.errorType ?: errorType,
                errorMessage = existing.errorMessage ?: conciseMessage
        )
    }

    private fun createBaseDetails(request: Request, requestTimeMillis: Long): NetworkCallDetails {
        val requestBodyString = request.safeBodyAsString()
        val requestHeadersString =
                request.headers.joinToString(separator = "\n") { header ->
                    "${header.first}: ${header.second}"
                }
        val url = request.url
        return NetworkCallDetails(
                id = 0L,
                method = request.method,
                url = url.toString(),
                host = url.host,
                path = url.encodedPath,
                scheme = url.scheme,
                requestHeaders = requestHeadersString,
                responseHeaders = "",
                requestBody = requestBodyString,
                responseBody = null,
                statusCode = null,
                isSuccessful = null,
                errorType = null,
                errorMessage = null,
                requestTimeMillis = requestTimeMillis,
                responseTimeMillis = null,
                durationMillis = null,
                requestSizeBytes = request.body?.contentLength()?.takeIf { it >= 0L },
                responseSizeBytes = null
        )
    }

    private fun persistAsync(details: NetworkCallDetails): Deferred<Long?> {
        return ioScope.async {
            val insertedId =
                    runCatching { repository.saveCall(details) }
                            .onFailure { error ->
                                Log.e(
                                        TAG,
                                        "Failed to persist network call: ${details.method} ${details.url}",
                                        error
                                )
                            }
                            .getOrNull()

            runCatching { notifier.notifyNewCall(details) }.onFailure { error ->
                Log.w(TAG, "Failed to post network inspector notification", error)
            }
            insertedId
        }
    }

    private class CallCaptureState(
            val startTimeNanos: Long,
            val startTimeMillis: Long,
            val initialRequest: Request,
            var details: NetworkCallDetails? = null
    )

    companion object {
        private const val TAG = "NetworkInspector"
    }
}

private fun Request.safeBodyAsString(): String? {
    val body = body ?: return null
    return try {
        val buffer = Buffer()
        body.writeTo(buffer)
        val charset: Charset = body.contentType().safeCharset()
        val truncated = Buffer()
        val byteCount = buffer.size.coerceAtMost(MAX_BODY_BYTES)
        buffer.read(truncated, byteCount)
        truncated.readString(charset)
    } catch (_: Throwable) {
        null
    }
}

private fun Response.safePeekBodyAsString(maxBytes: Long): String? {
    val body = body ?: return null
    return try {
        val peekedSource = body.source().peek()
        val buffer = Buffer()
        var totalRead = 0L
        var readError: Throwable? = null
        try {
            while (totalRead < maxBytes) {
                val toRead = minOf(8192L, maxBytes - totalRead)
                val read = peekedSource.read(buffer, toRead)
                if (read == -1L) break
                totalRead += read
            }
        } catch (t: Throwable) {
            readError = t
        }
        val charset = body.contentType().safeCharset()
        val text = if (buffer.size > 0L) buffer.readString(charset) else ""
        when {
            text.isNotEmpty() && readError != null ->
                    "$text\n\n[Body truncated due to ${readError::class.java.simpleName}: ${readError.message.orEmpty()}]"
            text.isNotEmpty() -> text
            readError != null ->
                    "[Body read failed: ${readError::class.java.simpleName}: ${readError.message.orEmpty()}]"
            else -> null
        }
    } catch (t: Throwable) {
        "[Body unavailable: ${t::class.java.simpleName}: ${t.message.orEmpty()}]"
    }
}

private fun MediaType?.safeCharset(): Charset {
    return try {
        this?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
    } catch (_: Exception) {
        Charset.forName("UTF-8")
    }
}

/**
 * Mirrors OkHttp log style (`<-- HTTP FAILED: …`) so the Response tab shows why no status/body
 * arrived.
 */
private fun buildHttpFailureResponseBody(throwable: Throwable): String {
    val typeName = throwable::class.java.name
    val message = throwable.message.orEmpty()
    return buildString {
        append("HTTP FAILED: ")
        append(typeName)
        if (message.isNotEmpty()) {
            append(": ")
            append(message)
        }
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 10) {
            appendLine()
            append("Caused by: ")
            append(cause::class.java.name)
            val causeMessage = cause.message
            if (!causeMessage.isNullOrEmpty()) {
                append(": ")
                append(causeMessage)
            }
            cause = cause.cause
            depth++
        }
    }
}
