package com.iiab.networkinspector.ui.metrics

import androidx.compose.runtime.Immutable
import com.iiab.networkinspector.domain.NetworkCall

@Immutable
data class NetworkSummaryMetrics(
    val totalCalls: Int,
    val successCount: Int,
    val errorCount: Int,
    val errorRatePercent: Float,
    val averageLatencyMillis: Long?,
    val p95LatencyMillis: Long?
)

@Immutable
data class EndpointMetrics(
    val key: String,
    val host: String,
    val method: String,
    val path: String,
    val count: Int,
    val errorCount: Int,
    val errorRatePercent: Float,
    val averageLatencyMillis: Long?,
    val p95LatencyMillis: Long?
)

@Immutable
data class NetworkMetrics(
    val summary: NetworkSummaryMetrics,
    val perHost: List<EndpointMetrics>,
    val perEndpoint: List<EndpointMetrics>
)

fun computeNetworkMetrics(calls: List<NetworkCall>): NetworkMetrics {
    if (calls.isEmpty()) {
        return NetworkMetrics(
            summary = NetworkSummaryMetrics(0, 0, 0, 0f, null, null),
            perHost = emptyList(),
            perEndpoint = emptyList()
        )
    }

    val durations = calls.mapNotNull { it.durationMillis }
    val totalCalls = calls.size
    val successCount = calls.count { it.isSuccessful == true }
    val errorCount = calls.count { it.isSuccessful == false || (it.statusCode ?: 0) >= 400 }
    val errorRate = if (totalCalls > 0) errorCount.toFloat() * 100f / totalCalls else 0f
    val avgLatency = if (durations.isNotEmpty()) durations.sum() / durations.size else null
    val p95Latency = durations.percentile(0.95)

    val summary = NetworkSummaryMetrics(
        totalCalls = totalCalls,
        successCount = successCount,
        errorCount = errorCount,
        errorRatePercent = errorRate,
        averageLatencyMillis = avgLatency,
        p95LatencyMillis = p95Latency
    )

    val perHost = calls.toEndpointMetrics { it.host }
    val perEndpoint = calls.toEndpointMetrics { "${it.method} ${it.path}" }

    return NetworkMetrics(summary = summary, perHost = perHost, perEndpoint = perEndpoint)
}

private fun List<NetworkCall>.toEndpointMetrics(
    keySelector: (NetworkCall) -> String
): List<EndpointMetrics> {
    return this
        .groupBy(keySelector)
        .mapNotNull { (key, group) ->
            if (group.isEmpty()) return@mapNotNull null
            val gDurations = group.mapNotNull { it.durationMillis }
            val gCount = group.size
            val gErrorCount =
                group.count { it.isSuccessful == false || (it.statusCode ?: 0) >= 400 }
            val gErrorRate = if (gCount > 0) gErrorCount.toFloat() * 100f / gCount else 0f
            val gAvg = if (gDurations.isNotEmpty()) gDurations.sum() / gDurations.size else null
            val gP95 = gDurations.percentile(0.95)

            val sample = group.first()
            EndpointMetrics(
                key = key,
                host = sample.host,
                method = sample.method,
                path = sample.path,
                count = gCount,
                errorCount = gErrorCount,
                errorRatePercent = gErrorRate,
                averageLatencyMillis = gAvg,
                p95LatencyMillis = gP95
            )
        }
        .sortedWith(
            compareByDescending<EndpointMetrics> { it.errorRatePercent }
                .thenByDescending { it.averageLatencyMillis ?: 0L }
        )
}

private fun List<Long>.percentile(p: Double): Long? {
    if (isEmpty()) return null
    val sorted = this.sorted()
    val index = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
    return sorted[index]
}
