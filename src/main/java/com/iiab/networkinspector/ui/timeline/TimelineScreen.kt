package com.iiab.networkinspector.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun NetworkInspectorTimelineScreen(
    items: List<TimelineItem>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "No captured calls yet.")
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(items, key = { it.id }) { item ->
            TimelineRow(
                item = item,
                onClick = { onItemClick(item.id) }
            )
            Divider()
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.method} ${item.path}",
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.statusCode?.toString() ?: "-",
                style = MaterialTheme.typography.caption
            )
        }
        Text(
            text = item.host,
            style = MaterialTheme.typography.caption
        )
        Text(
            text = "${item.durationMillis} ms",
            style = MaterialTheme.typography.caption
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clipToBounds()
        ) {
            val startWeight = item.startFraction.coerceIn(0f, 1f)
            val endWeight = item.endFraction.coerceIn(startWeight, 1f)
            val barWeight = (endWeight - startWeight).coerceAtLeast(0.02f)

            if (startWeight > 0f) {
                Spacer(modifier = Modifier.weight(startWeight))
            }
            Box(
                modifier = Modifier
                    .weight(barWeight)
                    .fillMaxHeight()
                    .background(
                        when {
                            item.statusCode == null -> MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            item.statusCode in 200..299 -> MaterialTheme.colors.primary
                            item.statusCode in 400..499 -> MaterialTheme.colors.secondary
                            else -> MaterialTheme.colors.error
                        }
                    )
            )
            if (endWeight < 1f) {
                Spacer(modifier = Modifier.weight(1f - endWeight))
            }
        }
    }
}
