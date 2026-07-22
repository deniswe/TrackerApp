package com.example.trackerapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.trackerapp.TrackerApplication

class LogEventAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val typeKey = parameters[TYPE_KEY_PARAM] ?: return
        val value = parameters[VALUE_PARAM]?.toDouble()
        val repository = (context.applicationContext as TrackerApplication).repository
        repository.logEvent(typeKey = typeKey, value = value)
    }

    companion object {
        val TYPE_KEY_PARAM = ActionParameters.Key<String>("typeKey")
        val VALUE_PARAM = ActionParameters.Key<Int>("value")
    }
}
