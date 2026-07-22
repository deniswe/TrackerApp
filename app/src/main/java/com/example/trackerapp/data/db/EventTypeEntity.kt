package com.example.trackerapp.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_types",
    indices = [Index(value = ["backendKey"], unique = true)]
)
data class EventTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val backendKey: String,
    val emoji: String,
    val colorArgb: Int,
    val kind: EventKind,
    val scaleMin: Int?,
    val scaleMax: Int?,
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long
)