package com.iiab.networkinspector.ui

import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iiab.networkinspector.domain.NetworkCall
import com.iiab.networkinspector.domain.NetworkCallDetails
import com.iiab.networkinspector.ui.details.BodyTabContent
import com.iiab.networkinspector.ui.details.ExpandableJsonContent
import com.iiab.networkinspector.ui.details.HeadersTabContent
import com.iiab.networkinspector.ui.details.NetworkInspectorDetailsScreen
import com.iiab.networkinspector.ui.list.NetworkInspectorListScreen
import com.iiab.networkinspector.ui.metrics.EndpointMetrics
import com.iiab.networkinspector.ui.metrics.NetworkInspectorMetricsScreen
import com.iiab.networkinspector.ui.metrics.NetworkMetrics
import com.iiab.networkinspector.ui.metrics.NetworkSummaryMetrics
import com.iiab.networkinspector.ui.model.JsonNode
import com.iiab.networkinspector.ui.timeline.NetworkInspectorTimelineScreen
import com.iiab.networkinspector.ui.timeline.TimelineItem

private object PreviewSamples {

    val networkCalls: List<NetworkCall> = listOf(
        sampleCall(
            id = 1L,
            method = "GET",
            path = "/v1/users",
            statusCode = 200,
            durationMillis = 95L,
        ),
        sampleCall(
            id = 2L,
            method = "POST",
            path = "/v1/auth/token",
            statusCode = 401,
            durationMillis = 210L,
        ),
    )

    val networkCallDetails: NetworkCallDetails = NetworkCallDetails(
        id = 1L,
        method = "POST",
        url = "https://api.example.com/v1/login",
        host = "api.example.com",
        path = "/v1/login",
        scheme = "https",
        requestHeaders = "Content-Type: application/json\nAccept: application/json",
        responseHeaders = "Content-Type: application/json\nX-Request-Id: req-123",
        requestBody = """{"username":"demo"}""",
        responseBody = """{"access_token":"…","expires_in":3600}""",
        statusCode = 200,
        isSuccessful = true,
        errorType = null,
        errorMessage = null,
        requestTimeMillis = 1_700_000_000_000L,
        responseTimeMillis = 1_700_000_000_120L,
        durationMillis = 120L,
        requestSizeBytes = 24L,
        responseSizeBytes = 48L,
    )

    val metrics: NetworkMetrics = NetworkMetrics(
        summary = NetworkSummaryMetrics(
            totalCalls = 48,
            successCount = 45,
            errorCount = 3,
            errorRatePercent = 6.25f,
            averageLatencyMillis = 142L,
            p95LatencyMillis = 380L,
        ),
        perHost = listOf(
            EndpointMetrics(
                key = "api.example.com",
                host = "api.example.com",
                method = "GET",
                path = "/v1/items",
                count = 12,
                errorCount = 1,
                errorRatePercent = 8.3f,
                averageLatencyMillis = 205L,
                p95LatencyMillis = 410L,
            ),
        ),
        perEndpoint = listOf(
            EndpointMetrics(
                key = "GET /v1/health",
                host = "api.example.com",
                method = "GET",
                path = "/v1/health",
                count = 24,
                errorCount = 0,
                errorRatePercent = 0f,
                averageLatencyMillis = 18L,
                p95LatencyMillis = 32L,
            ),
        ),
    )

    val timelineItems: List<TimelineItem> = listOf(
        TimelineItem(
            id = 1L,
            method = "GET",
            path = "/v1/profile",
            host = "api.example.com",
            statusCode = 200,
            startTimeMillis = 0L,
            endTimeMillis = 80L,
            durationMillis = 80L,
            startFraction = 0.05f,
            endFraction = 0.35f,
        ),
        TimelineItem(
            id = 2L,
            method = "POST",
            path = "/v1/orders",
            host = "api.example.com",
            statusCode = 500,
            startTimeMillis = 100L,
            endTimeMillis = 340L,
            durationMillis = 240L,
            startFraction = 0.4f,
            endFraction = 0.85f,
        ),
    )

    val jsonTree: JsonNode = JsonNode.ObjectNode(
        mapOf(
            "user" to JsonNode.ObjectNode(
                mapOf(
                    "id" to JsonNode.ValueNode("42"),
                    "active" to JsonNode.ValueNode("true"),
                ),
            ),
            "tags" to JsonNode.ArrayNode(
                listOf(
                    JsonNode.ValueNode("alpha"),
                    JsonNode.ValueNode("beta"),
                ),
            ),
        ),
    )

    private fun sampleCall(
        id: Long,
        method: String,
        path: String,
        statusCode: Int?,
        durationMillis: Long?,
    ): NetworkCall = NetworkCall(
        id = id,
        method = method,
        url = "https://api.example.com$path",
        host = "api.example.com",
        path = path,
        scheme = "https",
        statusCode = statusCode,
        isSuccessful = statusCode?.let { it in 200..299 },
        errorMessage = null,
        requestTimeMillis = 1_700_000_000_000L + id * 1000L,
        responseTimeMillis = durationMillis?.let { 1_700_000_000_000L + id * 1000L + it },
        durationMillis = durationMillis,
    )
}

@Preview(name = "List — empty", showBackground = true)
@Composable
private fun PreviewNetworkInspectorListScreenEmpty() {
    MaterialTheme {
        Surface {
            NetworkInspectorListScreen(
                calls = emptyList(),
                onCallClick = {},
                modifier = Modifier.height(360.dp),
            )
        }
    }
}

@Preview(name = "List — with rows", showBackground = true)
@Composable
private fun PreviewNetworkInspectorListScreenFilled() {
    MaterialTheme {
        Surface {
            NetworkInspectorListScreen(
                calls = PreviewSamples.networkCalls,
                onCallClick = {},
                modifier = Modifier.height(420.dp),
            )
        }
    }
}

@Preview(name = "Details — loaded", showBackground = true, heightDp = 640, widthDp = 360)
@Composable
private fun PreviewNetworkInspectorDetailsScreenLoaded() {
    MaterialTheme {
        Surface {
            NetworkInspectorDetailsScreen(
                details = PreviewSamples.networkCallDetails,
                onBack = {},
                onExport = {},
            )
        }
    }
}

@Preview(name = "Details — HTTP failed (no status)", showBackground = true, heightDp = 640, widthDp = 360)
@Composable
private fun PreviewNetworkInspectorDetailsScreenHttpFailed() {
    MaterialTheme {
        Surface {
            NetworkInspectorDetailsScreen(
                details = PreviewSamples.networkCallDetails.copy(
                    statusCode = null,
                    isSuccessful = false,
                    responseBody = "HTTP FAILED: java.io.IOException: unexpected end of stream on com.android.okhttp.internal.http.Http1xStream@abc123",
                    errorType = "IOException",
                    errorMessage = "unexpected end of stream on com.android.okhttp.internal.http.Http1xStream@abc123",
                    durationMillis = 84L,
                ),
                onBack = {},
                onExport = {},
            )
        }
    }
}

@Preview(name = "Details — missing", showBackground = true, heightDp = 400, widthDp = 360)
@Composable
private fun PreviewNetworkInspectorDetailsScreenEmpty() {
    MaterialTheme {
        Surface {
            NetworkInspectorDetailsScreen(
                details = null,
                onBack = {},
                onExport = {},
            )
        }
    }
}

@Preview(name = "Headers tab", showBackground = true)
@Composable
private fun PreviewHeadersTabContent() {
    MaterialTheme {
        Surface {
            HeadersTabContent(
                requestHeaders = "Accept: application/json\nContent-Type: application/json",
                responseHeaders = "Content-Type: application/json\nCache-Control: no-store",
                modifier = Modifier.height(320.dp),
            )
        }
    }
}

@Preview(name = "Body tab — JSON (tree)", showBackground = true)
@Composable
private fun PreviewBodyTabContentJson() {
    MaterialTheme {
        Surface {
            BodyTabContent(
                content = """{"id":1,"name":"demo","ok":true}""",
                modifier = Modifier.height(360.dp),
            )
        }
    }
}

@Preview(name = "Body tab — HTTP failed", showBackground = true)
@Composable
private fun PreviewBodyTabContentHttpFailed() {
    MaterialTheme {
        Surface {
            BodyTabContent(
                content = "HTTP FAILED: java.io.IOException: unexpected end of stream on com.android.okhttp.internal.http.Http1xStream@abc123",
                modifier = Modifier.height(280.dp),
            )
        }
    }
}

@Preview(name = "Body tab — plain text", showBackground = true)
@Composable
private fun PreviewBodyTabContentPlain() {
    MaterialTheme {
        Surface {
            BodyTabContent(
                content = "Request payload is not JSON.",
                modifier = Modifier.height(280.dp),
            )
        }
    }
}

@Preview(name = "Expandable JSON", showBackground = true)
@Composable
private fun PreviewExpandableJsonContent() {
    MaterialTheme {
        Surface {
            ExpandableJsonContent(
                node = PreviewSamples.jsonTree,
                modifier = Modifier.height(400.dp),
            )
        }
    }
}

@Preview(name = "Metrics", showBackground = true)
@Composable
private fun PreviewNetworkInspectorMetricsScreen() {
    MaterialTheme {
        Surface {
            NetworkInspectorMetricsScreen(
                metrics = PreviewSamples.metrics,
                modifier = Modifier.height(520.dp),
            )
        }
    }
}

@Preview(name = "Timeline — empty", showBackground = true)
@Composable
private fun PreviewNetworkInspectorTimelineScreenEmpty() {
    MaterialTheme {
        Surface {
            NetworkInspectorTimelineScreen(
                items = emptyList(),
                onItemClick = {},
                modifier = Modifier.height(240.dp),
            )
        }
    }
}

@Preview(name = "Timeline — with bars", showBackground = true)
@Composable
private fun PreviewNetworkInspectorTimelineScreenFilled() {
    MaterialTheme {
        Surface {
            NetworkInspectorTimelineScreen(
                items = PreviewSamples.timelineItems,
                onItemClick = {},
                modifier = Modifier.height(360.dp),
            )
        }
    }
}
