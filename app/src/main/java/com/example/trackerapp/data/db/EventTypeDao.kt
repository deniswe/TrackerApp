package com.example.trackerapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<EventTypeEntity>)

    @Query("SELECT * FROM event_types ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE enabled = 1 ORDER BY sortOrder, name")
    fun observeEnabled(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE backendKey = :backendKey LIMIT 1")
    suspend fun getByBackendKey(backendKey: String): EventTypeEntity?
}