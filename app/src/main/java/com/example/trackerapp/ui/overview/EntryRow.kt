package com.example.trackerapp.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val FALLBACK_COLOR_ARGB = 0xFF757575.toInt()
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun EntryRow(item: EntryListItem, onDelete: () -> Unit) {
    val label = item.type?.name ?: item.entry.typeKey
    val emoji = item.type?.emoji ?: "❔"
    val colorArgb = item.type?.colorArgb ?: FALLBACK_COLOR_ARGB
    val time = Instant.ofEpochMilli(item.entry.epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TIME_FORMATTER)
    val valueText = item.entry.value?.let { " · ${it.toInt()}" } ?: ""
    val noteText = item.entry.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""

    ListItem(
        leadingContent = { EmojiChip(emoji = emoji, colorArgb = colorArgb) },
        headlineContent = { Text(label) },
        supportingContent = { Text("$time$valueText$noteText") },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete entry")
            }
        }
    )
}

@Composable
private fun EmojiChip(emoji: String, colorArgb: Int) {
    Surface(
        shape = CircleShape,
        color = Color(colorArgb),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(emoji)
        }
    }
}
