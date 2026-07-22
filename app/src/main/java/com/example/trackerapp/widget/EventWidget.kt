package com.example.trackerapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

private val COMPACT_SIZE = DpSize(40.dp, 40.dp)
private val WIDE_SIZE = DpSize(250.dp, 40.dp)
private val COMPACT_WIDTH_THRESHOLD = 100.dp

class EventWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Responsive(setOf(COMPACT_SIZE, WIDE_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(currentState())
            }
        }
    }
}

@Composable
private fun WidgetContent(prefs: Preferences) {
    val typeKey = prefs[WidgetPrefsKeys.TYPE_KEY]
    if (typeKey == null) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Not configured")
        }
        return
    }

    val label = prefs[WidgetPrefsKeys.LABEL] ?: typeKey
    val emoji = prefs[WidgetPrefsKeys.EMOJI] ?: ""
    val colorArgb = prefs[WidgetPrefsKeys.COLOR_ARGB] ?: DEFAULT_COLOR_ARGB
    val shape = prefs[WidgetPrefsKeys.SHAPE]?.let { WidgetShape.valueOf(it) }
        ?: WidgetShape.OCCURRENCE_BUTTON
    val compact = LocalSize.current.width < COMPACT_WIDTH_THRESHOLD

    when (shape) {
        WidgetShape.OCCURRENCE_BUTTON -> {
            EventTile(
                emoji = emoji,
                label = label,
                colorArgb = colorArgb,
                params = actionParametersOf(LogEventAction.TYPE_KEY_PARAM to typeKey),
                compact = compact
            )
        }

        WidgetShape.SCORE_FIXED -> {
            val fixedValue = prefs[WidgetPrefsKeys.FIXED_VALUE] ?: 0
            EventTile(
                emoji = emoji,
                label = "$label = $fixedValue",
                colorArgb = colorArgb,
                params = actionParametersOf(
                    LogEventAction.TYPE_KEY_PARAM to typeKey,
                    LogEventAction.VALUE_PARAM to fixedValue
                ),
                compact = compact
            )
        }

        WidgetShape.SCORE_MULTI -> {
            val min = prefs[WidgetPrefsKeys.SCALE_MIN] ?: 1
            val max = prefs[WidgetPrefsKeys.SCALE_MAX] ?: 5
            if (compact) {
                // Not enough width for one button per scale value here — fall back to
                // a single tile logging the scale's middle value, so the widget stays
                // one-tap even when resized smaller than the multi-button layout needs.
                val middleValue = (min + max + 1) / 2
                EventTile(
                    emoji = emoji,
                    label = "$label = $middleValue",
                    colorArgb = colorArgb,
                    params = actionParametersOf(
                        LogEventAction.TYPE_KEY_PARAM to typeKey,
                        LogEventAction.VALUE_PARAM to middleValue
                    ),
                    compact = true
                )
            } else {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    for (v in min..max) {
                        ScoreCell(
                            value = v,
                            colorArgb = colorArgb,
                            params = actionParametersOf(
                                LogEventAction.TYPE_KEY_PARAM to typeKey,
                                LogEventAction.VALUE_PARAM to v
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTile(
    emoji: String,
    label: String,
    colorArgb: Int,
    params: ActionParameters,
    compact: Boolean
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(colorArgb))
            .cornerRadius(16.dp)
            .clickable(actionRunCallback<LogEventAction>(params)),
        contentAlignment = Alignment.Center
    ) {
        if (compact) {
            Text(text = emoji, style = TextStyle(fontSize = 24.sp, color = ColorProvider(Color.White)))
        } else {
            Row {
                Text(text = emoji, style = TextStyle(fontSize = 20.sp, color = ColorProvider(Color.White)))
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(text = label, style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.White)))
            }
        }
    }
}

@Composable
private fun ScoreCell(
    value: Int,
    colorArgb: Int,
    params: ActionParameters,
    modifier: GlanceModifier = GlanceModifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(Color(colorArgb))
            .cornerRadius(12.dp)
            .clickable(actionRunCallback<LogEventAction>(params)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            modifier = GlanceModifier.padding(horizontal = 6.dp, vertical = 8.dp),
            style = TextStyle(fontSize = 15.sp, color = ColorProvider(Color.White))
        )
    }
}

private const val DEFAULT_COLOR_ARGB = 0xFF607D8B.toInt()
