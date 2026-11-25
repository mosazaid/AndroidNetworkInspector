package com.mousa.networkinspector.ui.timeline

import androidx.compose.runtime.Immutable
import com.mousa.networkinspector.domain.NetworkCall

@Immutable
data class TimelineItem(
    val id: Long,
    val method: String,
    val path: String,
    val host: String,
    val statusCode: Int?,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMillis: Long,
    val startFraction: Float,
    val endFraction: Float
)

fun computeTimelineItems(calls: List<NetworkCall>): List<TimelineItem> {
    if (calls.isEmpty()) return emptyList()

    val withTimes = calls.mapNotNull { call ->
        val start = call.requestTimeMillis
        val end = call.responseTimeMillis
            ?: call.durationMillis?.let { start + it }
            ?: start
        Triple(call, start, end)
    }
    if (withTimes.isEmpty()) return emptyList()

    val minStart = withTimes.minOf { it.second }
    val maxEnd = withTimes.maxOf { it.third }
    val span = (maxEnd - minStart).coerceAtLeast(1L).toFloat()

    return withTimes.map { (call, start, end) ->
        TimelineItem(
            id = call.id,
            method = call.method,
            path = call.path,
            host = call.host,
            statusCode = call.statusCode,
            startTimeMillis = start,
            endTimeMillis = end,
            durationMillis = (end - start).coerceAtLeast(0L),
            startFraction = (start - minStart) / span,
            endFraction = (end - minStart) / span
        )
    }.sortedBy { it.startTimeMillis }
}
