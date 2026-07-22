package com.example.trackerapp.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trackerapp.data.repository.EventRepository
import com.example.trackerapp.data.settings.SettingsRepository
import com.example.trackerapp.sync.SyncScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    repository: EventRepository,
    settingsRepository: SettingsRepository,
    onAddEntry: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val entries by repository.observeRecentEntries().collectAsStateWithLifecycle(initialValue = emptyList())
    val types by repository.observeAllTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    val unsyncedCount by repository.observeUnsyncedCount().collectAsStateWithLifecycle(initialValue = 0)
    val lastSyncAt by settingsRepository.lastSyncAt.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    val groupedByDay = remember(entries, types) {
        val typeByKey = types.associateBy { it.backendKey }
        entries.map { EntryListItem(it, typeByKey[it.typeKey]) }.groupByDay()
    }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TrackerApp") },
                actions = {
                    IconButton(onClick = { SyncScheduler.scheduleOneTime(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync now")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Default.Add, contentDescription = "Add entry")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SyncStatusRow(unsyncedCount = unsyncedCount, lastSyncAt = lastSyncAt)
            if (groupedByDay.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f).fillMaxWidth())
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    groupedByDay.forEach { (date, dayItems) ->
                        item(key = "header-$date") {
                            Text(
                                text = dayLabel(date),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(dayItems, key = { it.entry.clientEventId }) { entryItem ->
                            EntryRow(
                                item = entryItem,
                                onDelete = { pendingDeleteId = entryItem.entry.clientEventId }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete entry?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteEntry(id) }
                    pendingDeleteId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SyncStatusRow(unsyncedCount: Int, lastSyncAt: Long?) {
    val lastSyncText = lastSyncAt?.let {
        "Last synced " + Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    } ?: "Never synced"
    Text(
        text = "$unsyncedCount unsynced · $lastSyncText",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No events yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap a widget or add one manually to get started.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun List<EntryListItem>.groupByDay(): List<Pair<LocalDate, List<EntryListItem>>> =
    groupBy { Instant.ofEpochMilli(it.entry.epochMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
        .toList()

private fun dayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
}
