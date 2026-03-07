package com.example.headachetracker.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.headachetracker.data.local.HeadacheDatabase
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.location.GeocodingProvider
import com.example.headachetracker.data.location.LocationProvider
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

        val locationProvider = LocationProvider(context)
        val geocodingProvider = GeocodingProvider(context)
        val location = locationProvider.getCurrentLocation()
        val locationName = if (location != null) {
            geocodingProvider.getLocationName(location.latitude, location.longitude)
        } else null

        withContext(Dispatchers.IO) {
            val db = HeadacheDatabase.getInstance(context)
            db.headacheDao().insert(
                HeadacheEntry(
                    painLevel = painLevel,
                    timestamp = System.currentTimeMillis(),
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    locationName = locationName
                )
            )
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Logged pain level $painLevel", Toast.LENGTH_SHORT).show()
        }
    }
}
