package com.iiab.networkinspector.ui.details

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.iiab.networkinspector.ui.util.tryFormatJson
import com.iiab.networkinspector.ui.util.tryParseJsonToTree

private const val MAX_LINES_FOR_LAZY = 5000
private const val LARGE_BODY_THRESHOLD = 50_000

@Composable
internal fun BodyTabContent(
    content: String?,
    modifier: Modifier = Modifier
) {
    val raw = content?.trim() ?: "—"
    val isLarge = remember(raw) { raw.length > LARGE_BODY_THRESHOLD }
    val jsonNode = remember(raw) { if (!isLarge) tryParseJsonToTree(raw) else null }

    if (jsonNode != null && !isLarge) {
        val formatted = remember(raw) { tryFormatJson(raw) }
        ExpandableJsonContent(
            node = jsonNode,
            modifier = modifier,
            rawText = formatted
        )
    } else {
        PlainTextBodyContent(raw = raw, modifier = modifier)
    }
}

@Composable
private fun PlainTextBodyContent(
    raw: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val formatted = remember(raw) { tryFormatJson(raw) }
    val isLarge = remember(formatted) { formatted.length > LARGE_BODY_THRESHOLD }

    val copyAction = {
        clipboardManager.setText(AnnotatedString(formatted))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    if (!isLarge) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            BodyHeaderRow(onCopy = copyAction)
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.body2.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    val lines = remember(formatted) {
        formatted.split("\n").take(MAX_LINES_FOR_LAZY)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val boxMaxHeight = maxHeight
        val listState = rememberLazyListState()
        val scrollProgress by remember {
            derivedStateOf {
                val info = listState.layoutInfo
                val total = info.totalItemsCount
                val visible = info.visibleItemsInfo.size
                val first = info.visibleItemsInfo.firstOrNull()?.index ?: 0
                if (total > visible) {
                    first.toFloat() / (total - visible).coerceAtLeast(1)
                } else 0f
            }
        }

        val visibleFraction by remember {
            derivedStateOf {
                val info = listState.layoutInfo
                val total = info.totalItemsCount
                val visible = info.visibleItemsInfo.size
                (visible.toFloat() / total.coerceAtLeast(1)).coerceIn(0.1f, 1f)
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                BodyHeaderRow(onCopy = copyAction)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { index, line -> "line_${index}_${line.hashCode()}" }
                    ) { _, line ->
                        SelectionContainer {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.body2.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (lines.size > 10) {
                val trackHeight = boxMaxHeight - 16.dp
                val thumbHeight = trackHeight * visibleFraction
                val thumbOffset = (trackHeight - thumbHeight) * scrollProgress

                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .clipToBounds()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(thumbHeight)
                            .offset(y = thumbOffset)
                            .background(
                                MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                                MaterialTheme.shapes.small
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyHeaderRow(onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Body",
            style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
        )
        TextButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy body"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Copy")
        }
    }
}
