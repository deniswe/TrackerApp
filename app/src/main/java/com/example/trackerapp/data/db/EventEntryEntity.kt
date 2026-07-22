package com.example.trackerapp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_entries",
    indices = [Index(value = ["synced"]), Index(value = ["epochMillis"])]
)
data class EventEntryEntity(
    @PrimaryKey
    val clientEventId: String,
    val typeKey: String,
    val epochMillis: Long,
    val value: Double?,
    val note: String?,
    val source: String = "android",
    val synced: Boolean = false,
    val createdAt: Long,
    val deleted: Boolean = false,
    val deletedAt: Long? = null
)