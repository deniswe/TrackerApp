package com.example.trackerapp.data.db

import android.graphics.Color

fun seedEventTypes(now: Long): List<EventTypeEntity> = listOf(
    EventTypeEntity(
        name = "Café",
        backendKey = "cafe",
        emoji = "☕",
        colorArgb = Color.parseColor("#6F4E37"),
        kind = EventKind.OCCURRENCE,
        scaleMin = null,
        scaleMax = null,
        sortOrder = 0,
        createdAt = now
    ),
    EventTypeEntity(
        name = "Cola",
        backendKey = "cola",
        emoji = "🥤",
        colorArgb = Color.parseColor("#B22222"),
        kind = EventKind.OCCURRENCE,
        scaleMin = null,
        scaleMax = null,
        sortOrder = 1,
        createdAt = now
    ),
    EventTypeEntity(
        name = "Sick",
        backendKey = "sick",
        emoji = "🤒",
        colorArgb = Color.parseColor("#556B2F"),
        kind = EventKind.OCCURRENCE,
        scaleMin = null,
        scaleMax = null,
        sortOrder = 2,
        createdAt = now
    ),
    EventTypeEntity(
        name = "Sleep",
        backendKey = "sleep",
        emoji = "😴",
        colorArgb = Color.parseColor("#4169E1"),
        kind = EventKind.SCORE,
        scaleMin = 1,
        scaleMax = 5,
        sortOrder = 3,
        createdAt = now
    ),
    EventTypeEntity(
        name = "Headache",
        backendKey = "headache",
        emoji = "🤕",
        colorArgb = Color.parseColor("#8E44AD"),
        kind = EventKind.OCCURRENCE,
        scaleMin = null,
        scaleMax = null,
        sortOrder = 4,
        createdAt = now
    ),
    EventTypeEntity(
        name = "Energy Drink",
        backendKey = "energy_drink",
        emoji = "⚡",
        colorArgb = Color.parseColor("#F1C40F"),
        kind = EventKind.OCCURRENCE,
        scaleMin = null,
        scaleMax = null,
        sortOrder = 5,
        createdAt = now
    )
)