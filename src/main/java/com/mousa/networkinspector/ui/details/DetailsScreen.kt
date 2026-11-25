package com.mousa.networkinspector.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mousa.networkinspector.domain.NetworkCallDetails

@Composable
fun NetworkInspectorDetailsScreen(
    details: NetworkCallDetails?,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Request details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (details != null) {
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export to file"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (details == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No details available.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${details.method} ${details.url}",
                        style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        val isError = details.isSuccessful == false ||
                            details.errorType != null ||
                            details.errorMessage != null
                        Text(
                            text = when {
                                details.statusCode != null && isError -> "Status: ${details.statusCode} (Error)"
                                details.statusCode != null -> "Status: ${details.statusCode}"
                                details.errorMessage != null || details.errorType != null -> "Status: Failed"
                                else -> "Status: -"
                            },
                            color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.onSurface,
                            style = MaterialTheme.typography.body2
                        )
                        details.durationMillis?.let { duration ->
                            Text(
                                text = "  •  $duration ms",
                                style = MaterialTheme.typography.body2
                            )
                        }
                    }
                    if (details.errorType != null || details.errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val errorText = listOfNotNull(details.errorType, details.errorMessage)
                            .filter { it.isNotBlank() }
                            .joinToString(": ")
                        SelectionContainer {
                            Text(
                                text = errorText,
                                color = MaterialTheme.colors.error,
                                style = MaterialTheme.typography.caption
                            )
                        }
                    }
                }
                Divider()

                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Headers") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Request") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Response") }
                    )
                }

                when (selectedTab) {
                    0 -> HeadersTabContent(
                        requestHeaders = details.requestHeaders,
                        responseHeaders = details.responseHeaders,
                        modifier = Modifier.weight(1f)
                    )
                    1 -> BodyTabContent(
                        content = details.requestBody,
                        modifier = Modifier.weight(1f)
                    )
                    2 -> BodyTabContent(
                        content = details.responseBody?.takeIf { it.isNotBlank() }
                            ?: listOfNotNull(details.errorType, details.errorMessage)
                                .filter { it.isNotBlank() }
                                .joinToString(": ")
                                .takeIf { it.isNotBlank() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
