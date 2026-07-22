package com.example.trackerapp.ui.overview

import com.example.trackerapp.data.db.EventEntryEntity
import com.example.trackerapp.data.db.EventTypeEntity

data class EntryListItem(
    val entry: EventEntryEntity,
    val type: EventTypeEntity?
)
