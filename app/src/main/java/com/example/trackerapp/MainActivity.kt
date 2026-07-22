package com.example.trackerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.trackerapp.data.repository.EventRepository
import com.example.trackerapp.data.settings.SettingsRepository
import com.example.trackerapp.sync.SyncScheduler
import com.example.trackerapp.ui.addentry.AddEntryScreen
import com.example.trackerapp.ui.overview.OverviewScreen
import com.example.trackerapp.ui.settings.SettingsScreen
import com.example.trackerapp.ui.theme.TrackerAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TrackerApplication

        // "On app open" is one of the three sync triggers alongside tap/add and
        // manual sync — the primary catch-up path after a long offline gap.
        SyncScheduler.scheduleOneTime(this)

        setContent {
            TrackerAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TrackerAppRoot(app.repository, app.settingsRepository)
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Overview : Screen
    data object AddEntry : Screen
    data object Settings : Screen
}

@Composable
private fun TrackerAppRoot(repository: EventRepository, settingsRepository: SettingsRepository) {
    var screen by remember { mutableStateOf<Screen>(Screen.Overview) }

    when (screen) {
        Screen.Overview -> OverviewScreen(
            repository = repository,
            settingsRepository = settingsRepository,
            onAddEntry = { screen = Screen.AddEntry },
            onSettings = { screen = Screen.Settings }
        )

        Screen.AddEntry -> AddEntryScreen(
            repository = repository,
            onDone = { screen = Screen.Overview }
        )

        Screen.Settings -> SettingsScreen(
            settingsRepository = settingsRepository,
            onDone = { screen = Screen.Overview }
        )
    }
}
