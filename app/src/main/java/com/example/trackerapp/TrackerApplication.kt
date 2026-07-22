package com.example.trackerapp

import android.app.Application
import com.example.trackerapp.data.db.AppDatabase
import com.example.trackerapp.data.db.seedEventTypes
import com.example.trackerapp.data.network.ApiClient
import com.example.trackerapp.data.repository.EventRepository
import com.example.trackerapp.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: EventRepository by lazy { EventRepository(database, this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val apiClient: ApiClient by lazy { ApiClient() }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Insert-if-missing via the unique index on backendKey (IGNORE on conflict):
            // existing types are left untouched, newly added defaults get picked up on
            // next launch without needing a dedicated "manage types" screen yet.
            database.eventTypeDao().insertAll(seedEventTypes(System.currentTimeMillis()))
        }
    }
}
