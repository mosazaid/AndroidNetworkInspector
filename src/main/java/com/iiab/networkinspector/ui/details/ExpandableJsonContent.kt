package com.iiab.networkinspector.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.iiab.networkinspector.ui.model.JsonNode
import com.iiab.networkinspector.ui.util.flattenJsonNode

@Composable
internal fun ExpandableJsonContent(
    node: JsonNode,
    modifier: Modifier = Modifier,
    rawText: String? = null
) {
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Body",
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(rawText ?: flattenJsonNode(node)))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy body"
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            ExpandableJsonNode(
                node = node,
                path = "root",
                indent = 0,
                expandedPaths = expandedPaths
            )
        }
    }
}

@Composable
private fun ExpandableJsonNode(
    node: JsonNode,
    path: String,
    indent: Int,
    expandedPaths: MutableMap<String, Boolean>
) {
    val isExpanded =
        expandedPaths[path] ?: (node is JsonNode.ObjectNode || node is JsonNode.ArrayNode)
    val toggle: () -> Unit = { expandedPaths[path] = !isExpanded }

    when (node) {
        is JsonNode.ObjectNode -> {
            val summary = "{ ${node.entries.size} keys }"
            ExpandableRow(
                prefix = "{",
                suffix = "}",
                summary = summary,
                isExpanded = isExpanded,
                onToggle = toggle,
                indent = indent
            ) {
                if (isExpanded) {
                    node.entries.forEach { (key, value) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (indent + 1) * 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\"$key\": ",
                                    style = MaterialTheme.typography.body2.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                if (value is JsonNode.ValueNode) {
                                    Text(
                                        text = value.value,
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                            if (value !is JsonNode.ValueNode) {
                                ExpandableJsonNode(
                                    node = value,
                                    path = "$path.$key",
                                    indent = indent + 1,
                                    expandedPaths = expandedPaths
                                )
                            }
                        }
                    }
                }
            }
        }
        is JsonNode.ArrayNode -> {
            val summary = "[ ${node.items.size} items ]"
            ExpandableRow(
                prefix = "[",
                suffix = "]",
                summary = summary,
                isExpanded = isExpanded,
                onToggle = toggle,
                indent = indent
            ) {
                if (isExpanded) {
                    node.items.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (indent + 1) * 16.dp)
                        ) {
                            if (item is JsonNode.ValueNode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$index: ",
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$index: ",
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                                ExpandableJsonNode(
                                    node = item,
                                    path = "$path[$index]",
                                    indent = indent + 1,
                                    expandedPaths = expandedPaths
                                )
                            }
                        }
                    }
                }
            }
        }
        is JsonNode.ValueNode -> {
            Text(
                text = node.value,
                style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(start = indent * 16.dp)
            )
        }
    }
}

@Composable
private fun ExpandableRow(
    prefix: String,
    suffix: String,
    summary: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    indent: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(top = 2.dp, bottom = 2.dp, start = indent * 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = if (isExpanded) prefix else "$prefix $summary $suffix",
                style = MaterialTheme.typography.body2.copy(fontFamily = FontFamily.Monospace)
            )
        }
        content()
    }
}
