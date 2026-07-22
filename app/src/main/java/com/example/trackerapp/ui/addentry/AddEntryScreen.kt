package com.example.trackerapp.ui.addentry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.trackerapp.data.db.EventKind
import com.example.trackerapp.data.db.EventTypeEntity
import com.example.trackerapp.data.repository.EventRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(repository: EventRepository, onDone: () -> Unit) {
    val types by repository.observeEnabledTypes().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedType by remember { mutableStateOf<EventTypeEntity?>(null) }
    var selectedValue by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf(LocalDateTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onDone)

    val currentType = selectedType
    val canSave = currentType != null && (currentType.kind != EventKind.SCORE || selectedValue != null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add entry") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Type", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                types.forEach { type ->
                    FilterChip(
                        selected = selectedType?.id == type.id,
                        onClick = {
                            selectedType = type
                            selectedValue = null
                        },
                        label = { Text("${type.emoji} ${type.name}") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (currentType != null && currentType.kind == EventKind.SCORE) {
                Spacer(Modifier.height(16.dp))
                Text("Value", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val min = currentType.scaleMin ?: 1
                val max = currentType.scaleMax ?: 5
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    for (v in min..max) {
                        FilterChip(
                            selected = selectedValue == v,
                            onClick = { selectedValue = v },
                            label = { Text(v.toString()) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("When", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { dateTime = LocalDateTime.now() }) {
                    Text("Now")
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val type = currentType ?: return@Button
                    val epochMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val value = if (type.kind == EventKind.SCORE) selectedValue?.toDouble() else null
                    scope.launch {
                        repository.logEvent(
                            typeKey = type.backendKey,
                            value = value,
                            epochMillis = epochMillis,
                            note = note.ifBlank { null }
                        )
                        onDone()
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val pickedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dateTime = LocalDateTime.of(pickedDate, dateTime.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                dateTime = dateTime.withHour(timePickerState.hour).withMinute(timePickerState.minute)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
