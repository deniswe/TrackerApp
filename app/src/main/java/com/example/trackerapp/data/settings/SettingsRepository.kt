package com.example.trackerapp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val baseUrl: Flow<String> = context.settingsDataStore.data.map { it[Keys.BASE_URL] ?: "" }

    val lastSyncAt: Flow<Long?> = context.settingsDataStore.data.map { it[Keys.LAST_SYNC_AT] }

    suspend fun setBaseUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.BASE_URL] = url.trim() }
    }

    suspend fun recordSyncSuccess(atMillis: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_SYNC_AT] = atMillis }
    }
}
