package com.iiab.networkinspector.ui.list

import androidx.compose.foundation.clickable
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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iiab.networkinspector.domain.NetworkCall
import com.iiab.networkinspector.ui.util.formatRequestTimestamp

@Composable
fun NetworkInspectorListScreen(
    calls: List<NetworkCall>,
    onCallClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCalls by remember(calls, searchQuery) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                calls
            } else {
                calls.filter { call ->
                    call.url.contains(query, ignoreCase = true) ||
                        call.path.contains(query, ignoreCase = true) ||
                        call.host.contains(query, ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            label = { Text(text = "Search by URL") },
            placeholder = { Text(text = "Filter by URL, path, or host") }
        )

        when {
            calls.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No captured calls yet.")
                }
            }
            filteredCalls.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No calls match your search.")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredCalls, key = { it.id }) { call ->
                        NetworkCallRow(
                            call = call,
                            onClick = { onCallClick(call.id) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkCallRow(
    call: NetworkCall,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = call.method,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold)
            )
            val isError = call.isSuccessful == false || call.errorMessage != null
            Text(
                text = when {
                    call.statusCode != null && isError -> "${call.statusCode} (Error)"
                    call.statusCode != null -> call.statusCode.toString()
                    call.errorMessage != null -> "Failed"
                    else -> "-"
                },
                color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.body2
            )
        }
        if (!call.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = call.errorMessage,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = call.path,
            style = MaterialTheme.typography.body2,
            minLines = 1,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = call.host,
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatRequestTimestamp(call.requestTimeMillis),
            style = MaterialTheme.typography.caption
        )
        call.durationMillis?.let { duration ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$duration ms",
                style = MaterialTheme.typography.caption
            )
        }
    }
}
