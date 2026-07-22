package com.example.trackerapp.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.trackerapp.TrackerApplication
import com.example.trackerapp.data.db.EventKind
import com.example.trackerapp.data.db.EventTypeEntity
import com.example.trackerapp.ui.theme.TrackerAppTheme
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repository = (application as TrackerApplication).repository

        setContent {
            val types by repository.observeEnabledTypes()
                .collectAsStateWithLifecycle(initialValue = emptyList())
            TrackerAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(types = types, onFinish = ::finishConfiguration)
                }
            }
        }
    }

    private fun finishConfiguration(type: EventTypeEntity, shape: WidgetShape, fixedValue: Int?) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigurationActivity)
                .getGlanceIdBy(appWidgetId)

            updateAppWidgetState(
                context = this@WidgetConfigurationActivity,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId
            ) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetPrefsKeys.TYPE_KEY] = type.backendKey
                    this[WidgetPrefsKeys.LABEL] = type.name
                    this[WidgetPrefsKeys.EMOJI] = type.emoji
                    this[WidgetPrefsKeys.COLOR_ARGB] = type.colorArgb
                    this[WidgetPrefsKeys.SHAPE] = shape.name
                    if (shape == WidgetShape.SCORE_MULTI) {
                        this[WidgetPrefsKeys.SCALE_MIN] = type.scaleMin ?: 1
                        this[WidgetPrefsKeys.SCALE_MAX] = type.scaleMax ?: 5
                    }
                    if (shape == WidgetShape.SCORE_FIXED && fixedValue != null) {
                        this[WidgetPrefsKeys.FIXED_VALUE] = fixedValue
                    }
                }
            }

            EventWidget().update(this@WidgetConfigurationActivity, glanceId)

            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}

private sealed interface ConfigStep {
    data object PickType : ConfigStep
    data class PickScoreShape(val type: EventTypeEntity) : ConfigStep
}

@Composable
private fun ConfigScreen(
    types: List<EventTypeEntity>,
    onFinish: (EventTypeEntity, WidgetShape, Int?) -> Unit
) {
    var step by remember { mutableStateOf<ConfigStep>(ConfigStep.PickType) }

    when (val currentStep = step) {
        is ConfigStep.PickType -> TypePickerStep(types = types) { picked ->
            if (picked.kind == EventKind.OCCURRENCE) {
                onFinish(picked, WidgetShape.OCCURRENCE_BUTTON, null)
            } else {
                step = ConfigStep.PickScoreShape(picked)
            }
        }

        is ConfigStep.PickScoreShape -> ScoreShapeStep(type = currentStep.type) { shape, fixedValue ->
            onFinish(currentStep.type, shape, fixedValue)
        }
    }
}

@Composable
private fun TypePickerStep(types: List<EventTypeEntity>, onPick: (EventTypeEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose what this widget logs", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(types) { type ->
                ListItem(
                    headlineContent = { Text(type.name) },
                    supportingContent = {
                        Text(
                            if (type.kind == EventKind.SCORE) {
                                "Score ${type.scaleMin}–${type.scaleMax}"
                            } else {
                                "Occurrence"
                            }
                        )
                    },
                    leadingContent = { Text(type.emoji, style = MaterialTheme.typography.headlineSmall) },
                    modifier = Modifier.clickable { onPick(type) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ScoreShapeStep(type: EventTypeEntity, onPick: (WidgetShape, Int?) -> Unit) {
    val min = type.scaleMin ?: 1
    val max = type.scaleMax ?: 5
    var fixedValue by remember(type.id) { mutableStateOf(min) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${type.emoji} ${type.name} — widget layout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Button(onClick = { onPick(WidgetShape.SCORE_MULTI, null) }, modifier = Modifier.fillMaxWidth()) {
            Text("Multiple buttons ($min–$max)")
        }

        Spacer(Modifier.height(24.dp))
        Text("Or always log one fixed value:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            for (v in min..max) {
                FilterChip(
                    selected = fixedValue == v,
                    onClick = { fixedValue = v },
                    label = { Text(v.toString()) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onPick(WidgetShape.SCORE_FIXED, fixedValue) }, modifier = Modifier.fillMaxWidth()) {
            Text("Use fixed value $fixedValue")
        }
    }
}
