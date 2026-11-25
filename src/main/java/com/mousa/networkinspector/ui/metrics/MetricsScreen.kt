package com.mousa.networkinspector.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NetworkInspectorMetricsScreen(
    metrics: NetworkMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Summary",
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Total", style = MaterialTheme.typography.caption)
                Text(text = metrics.summary.totalCalls.toString())
            }
            Column {
                Text(text = "Errors", style = MaterialTheme.typography.caption)
                Text(
                    text = "${metrics.summary.errorCount} (${
                        String.format("%.1f", metrics.summary.errorRatePercent)
                    }%)"
                )
            }
            Column {
                Text(text = "Avg (ms)", style = MaterialTheme.typography.caption)
                Text(text = metrics.summary.averageLatencyMillis?.toString() ?: "-")
            }
            Column {
                Text(text = "P95 (ms)", style = MaterialTheme.typography.caption)
                Text(text = metrics.summary.p95LatencyMillis?.toString() ?: "-")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (metrics.perHost.isNotEmpty()) {
                item {
                    Text(text = "Per host", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(metrics.perHost, key = { "host_${it.key}" }) { hostMetrics ->
                    MetricsRow(hostMetrics)
                    Divider()
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            if (metrics.perEndpoint.isNotEmpty()) {
                item {
                    Text(text = "Per endpoint", style = MaterialTheme.typography.subtitle1)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(metrics.perEndpoint, key = { "ep_${it.key}" }) { epMetrics ->
                    MetricsRow(epMetrics)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun MetricsRow(metrics: EndpointMetrics) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "${metrics.method} ${metrics.path}",
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metrics.host,
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Count: ${metrics.count}",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "Errors: ${metrics.errorCount} (${
                    String.format("%.1f", metrics.errorRatePercent)
                }%)",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "Avg: ${metrics.averageLatencyMillis ?: "-"} ms",
                style = MaterialTheme.typography.caption
            )
            Text(
                text = "P95: ${metrics.p95LatencyMillis ?: "-"} ms",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
