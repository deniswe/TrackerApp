package com.example.trackerapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventEntryDao {

    @Insert
    suspend fun insert(entry: EventEntryEntity)

    @Query("SELECT * FROM event_entries WHERE deleted = 0 ORDER BY epochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<EventEntryEntity>>

    @Query("SELECT * FROM event_entries WHERE clientEventId = :clientEventId LIMIT 1")
    suspend fun getById(clientEventId: String): EventEntryEntity?

    @Query("DELETE FROM event_entries WHERE clientEventId = :clientEventId")
    suspend fun hardDelete(clientEventId: String)

    @Query(
        "UPDATE event_entries SET deleted = 1, deletedAt = :deletedAt, synced = 0 " +
            "WHERE clientEventId = :clientEventId"
    )
    suspend fun softDelete(clientEventId: String, deletedAt: Long)

    // deleted = 0: deletions aren't propagated to the backend yet (no delete endpoint
    // there), so soft-deleted rows stay out of the create-sync upload queue even
    // though they're technically "unsynced" — see APP_PROJECT.md's delete semantics.
    @Query("SELECT * FROM event_entries WHERE synced = 0 AND deleted = 0 ORDER BY epochMillis")
    suspend fun getUnsynced(): List<EventEntryEntity>

    @Query("SELECT COUNT(*) FROM event_entries WHERE synced = 0 AND deleted = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE event_entries SET synced = 1 WHERE clientEventId IN (:clientEventIds)")
    suspend fun markSynced(clientEventIds: List<String>)
}