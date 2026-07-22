package com.example.trackerapp.data.repository

import android.content.Context
import com.example.trackerapp.data.db.AppDatabase
import com.example.trackerapp.data.db.EventEntryEntity
import com.example.trackerapp.data.db.EventTypeEntity
import com.example.trackerapp.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class EventRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val typeDao = database.eventTypeDao()
    private val entryDao = database.eventEntryDao()

    fun observeEnabledTypes(): Flow<List<EventTypeEntity>> = typeDao.observeEnabled()

    fun observeAllTypes(): Flow<List<EventTypeEntity>> = typeDao.observeAll()

    fun observeRecentEntries(limit: Int = 50): Flow<List<EventEntryEntity>> =
        entryDao.observeRecent(limit)

    fun observeUnsyncedCount(): Flow<Int> = entryDao.observeUnsyncedCount()

    suspend fun getTypeByBackendKey(backendKey: String): EventTypeEntity? =
        typeDao.getByBackendKey(backendKey)

    suspend fun logEvent(
        typeKey: String,
        value: Double?,
        epochMillis: Long = System.currentTimeMillis(),
        note: String? = null
    ) {
        entryDao.insert(
            EventEntryEntity(
                clientEventId = UUID.randomUUID().toString(),
                typeKey = typeKey,
                epochMillis = epochMillis,
                value = value,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )
        SyncScheduler.scheduleOneTime(context)
    }

    suspend fun deleteEntry(clientEventId: String) {
        val entry = entryDao.getById(clientEventId) ?: return
        if (entry.synced) {
            entryDao.softDelete(clientEventId, deletedAt = System.currentTimeMillis())
            SyncScheduler.scheduleOneTime(context)
        } else {
            entryDao.hardDelete(clientEventId)
        }
    }
}
