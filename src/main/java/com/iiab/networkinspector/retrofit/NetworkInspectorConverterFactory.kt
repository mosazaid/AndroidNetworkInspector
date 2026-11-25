package com.iiab.networkinspector.retrofit

import com.iiab.networkinspector.interceptor.DebugNetworkInspectorInterceptor
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Wraps a Retrofit [Converter.Factory] so post-OkHttp response parsing failures
 * (e.g., MalformedJsonException, JsonSyntaxException, EOFException) are attached
 * to the corresponding call in [DebugNetworkInspectorInterceptor].
 */
class NetworkInspectorConverterFactory(
    private val delegate: Converter.Factory,
    private val networkInspector: DebugNetworkInspectorInterceptor?
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val converter = delegate.responseBodyConverter(type, annotations, retrofit) ?: return null
        val inspector = networkInspector ?: return converter
        return Converter<ResponseBody, Any?> { value ->
            try {
                converter.convert(value)
            } catch (t: Throwable) {
                inspector.recordConverterFailure(t)
                throw t
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? =
        delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? =
        delegate.stringConverter(type, annotations, retrofit)
}

fun Converter.Factory.withNetworkInspector(
    networkInspector: DebugNetworkInspectorInterceptor?
): Converter.Factory {
    return if (networkInspector != null) {
        NetworkInspectorConverterFactory(delegate = this, networkInspector = networkInspector)
    } else {
        this
    }
}

