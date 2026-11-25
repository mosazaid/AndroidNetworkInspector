package com.iiab.networkinspector.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.iiab.networkinspector.domain.ClearNetworkCallsUseCase
import com.iiab.networkinspector.domain.GetNetworkCallDetailsUseCase
import com.iiab.networkinspector.domain.NetworkCall
import com.iiab.networkinspector.domain.NetworkCallDetails
import com.iiab.networkinspector.domain.ObserveNetworkCallsUseCase
import com.iiab.networkinspector.ui.details.NetworkInspectorDetailsScreen
import com.iiab.networkinspector.ui.list.NetworkInspectorListScreen
import com.iiab.networkinspector.ui.metrics.NetworkInspectorMetricsScreen
import com.iiab.networkinspector.ui.metrics.computeNetworkMetrics
import com.iiab.networkinspector.ui.model.ExportResult
import com.iiab.networkinspector.ui.model.RootTab
import com.iiab.networkinspector.ui.timeline.NetworkInspectorTimelineScreen
import com.iiab.networkinspector.ui.timeline.computeTimelineItems
import com.iiab.networkinspector.ui.util.exportNetworkCallToPdf
import com.iiab.networkinspector.ui.util.openDownloadsFolder
import com.iiab.networkinspector.ui.util.openPdf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class NetworkInspectorActivity : ComponentActivity() {

    private val observeCallsUseCase: ObserveNetworkCallsUseCase by inject()
    private val getDetailsUseCase: GetNetworkCallDetailsUseCase by inject()
    private val clearUseCase: ClearNetworkCallsUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkInspectorRoot(
                observeCallsUseCase = observeCallsUseCase,
                getDetailsUseCase = getDetailsUseCase,
                clearUseCase = clearUseCase
            )
        }
    }
}

@Composable
private fun NetworkInspectorRoot(
    observeCallsUseCase: ObserveNetworkCallsUseCase,
    getDetailsUseCase: GetNetworkCallDetailsUseCase,
    clearUseCase: ClearNetworkCallsUseCase
) {
    val context = LocalContext.current
    val listState = remember { MutableStateFlow<List<NetworkCall>>(emptyList()) }
    val list by listState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(RootTab.LIST.ordinal) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var lastExportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        observeCallsUseCase().collectLatest { calls ->
            listState.value = calls
        }
    }

    val metrics by remember(list) {
        derivedStateOf { computeNetworkMetrics(list) }
    }
    val timelineItems by remember(list) {
        derivedStateOf { computeTimelineItems(list) }
    }

    if (selectedId == null) {
        val currentTab = RootTab.entries.getOrNull(selectedTab) ?: RootTab.LIST
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(text = "Network inspector") },
                        actions = {
                            IconButton(onClick = {
                                scope.launch { clearUseCase() }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear all"
                                )
                            }
                        }
                    )
                    TabRow(selectedTabIndex = currentTab.ordinal) {
                        Tab(
                            selected = currentTab == RootTab.LIST,
                            onClick = { selectedTab = RootTab.LIST.ordinal },
                            text = { Text("List") }
                        )
                        Tab(
                            selected = currentTab == RootTab.METRICS,
                            onClick = { selectedTab = RootTab.METRICS.ordinal },
                            text = { Text("Metrics") }
                        )
                        Tab(
                            selected = currentTab == RootTab.TIMELINE,
                            onClick = { selectedTab = RootTab.TIMELINE.ordinal },
                            text = { Text("Timeline") }
                        )
                    }
                }
            }
        ) { padding ->
            when (currentTab) {
                RootTab.LIST -> NetworkInspectorListScreen(
                    calls = list,
                    onCallClick = { id -> selectedId = id },
                    modifier = Modifier.padding(padding)
                )
                RootTab.METRICS -> NetworkInspectorMetricsScreen(
                    metrics = metrics,
                    modifier = Modifier.padding(padding)
                )
                RootTab.TIMELINE -> NetworkInspectorTimelineScreen(
                    items = timelineItems,
                    onItemClick = { id -> selectedId = id },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    } else {
        val detailsState = remember { MutableStateFlow<NetworkCallDetails?>(null) }
        val details by detailsState.collectAsState()

        LaunchedEffect(selectedId) {
            val id = selectedId
            if (id != null) {
                getDetailsUseCase(id).collectLatest { value ->
                    detailsState.value = value
                }
            } else {
                detailsState.value = null
            }
        }

        NetworkInspectorDetailsScreen(
            details = details,
            onBack = { selectedId = null },
            onExport = {
                val current = details ?: return@NetworkInspectorDetailsScreen
                scope.launch {
                    val result = exportNetworkCallToPdf(context, current)
                    if (result != null) {
                        lastExportResult = result
                        showExportDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "Export failed – check Logcat for details",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    ExportDialog(
        showDialog = showExportDialog,
        exportResult = lastExportResult,
        onOpenFile = { openPdf(context, it.uri) },
        onOpenFolder = { openDownloadsFolder(context) },
        onDismiss = { showExportDialog = false }
    )
}

@Composable
private fun ExportDialog(
    showDialog: Boolean,
    exportResult: ExportResult?,
    onOpenFile: (ExportResult) -> Unit,
    onOpenFolder: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!showDialog || exportResult == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "File saved (PDF)") },
        text = {
            Text(
                text = "Location:\n${exportResult.locationHint}",
                style = MaterialTheme.typography.body2
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = { onOpenFile(exportResult) }) {
                    Text(text = "Open File")
                }
                TextButton(onClick = onOpenFolder) {
                    Text(text = "Open Folder")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}
