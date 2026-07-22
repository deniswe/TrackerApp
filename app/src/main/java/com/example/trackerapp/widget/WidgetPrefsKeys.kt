package com.example.trackerapp.widget

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetPrefsKeys {
    val TYPE_KEY = stringPreferencesKey("typeKey")
    val LABEL = stringPreferencesKey("label")
    val EMOJI = stringPreferencesKey("emoji")
    val COLOR_ARGB = intPreferencesKey("colorArgb")
    val SHAPE = stringPreferencesKey("shape")
    val SCALE_MIN = intPreferencesKey("scaleMin")
    val SCALE_MAX = intPreferencesKey("scaleMax")
    val FIXED_VALUE = intPreferencesKey("fixedValue")
}
