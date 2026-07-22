package com.example.trackerapp.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    @SerialName("client_event_id") val clientEventId: String,
    val type: String,
    val timestamp: String,
    val source: String,
    val value: Double? = null,
    val note: String? = null
)
