package com.example.headachetracker.data.location

import android.content.Context
import android.location.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeocodingProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getLocationName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null

        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull() ?: return@withContext null
                val city = address.locality ?: address.subAdminArea
                val state = address.adminArea
                listOfNotNull(city, state).joinToString(", ").ifBlank { null }
            } catch (_: Exception) {
                null
            }
        }
    }
}
