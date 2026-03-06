package com.example.headachetracker.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.headachetracker.data.local.HeadacheDatabase
import com.example.headachetracker.data.local.HeadacheEntry
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetClickAction : ActionCallback {

    companion object {
        val PAIN_LEVEL_KEY = ActionParameters.Key<Int>("pain_level")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val painLevel = parameters[PAIN_LEVEL_KEY] ?: return

        withContext(Dispatchers.IO) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                HeadacheDatabase::class.java,
                "headache_tracker.db"
            ).build()

            db.headacheDao().insert(
                HeadacheEntry(
                    painLevel = painLevel,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Logged pain level $painLevel", Toast.LENGTH_SHORT).show()
        }
    }
}
