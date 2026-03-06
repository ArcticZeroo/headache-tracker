package com.example.headachetracker.data.weather

import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.WeatherDao
import com.example.headachetracker.data.local.WeatherData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherService: OpenMeteoService,
    private val weatherDao: WeatherDao,
    private val headacheDao: HeadacheDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun fetchAndStoreWeatherForEntry(entryId: Long) {
        val entry = headacheDao.getEntryById(entryId) ?: return
        val lat = entry.latitude ?: return
        val lng = entry.longitude ?: return

        // Check if weather data already exists for this entry
        if (weatherDao.getByEntryId(entryId) != null) return

        val date = Instant.ofEpochMilli(entry.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val dateStr = date.format(dateFormatter)

        try {
            val response = weatherService.getHistoricalWeather(
                latitude = lat,
                longitude = lng,
                startDate = dateStr,
                endDate = dateStr
            )

            val daily = response.daily ?: return
            if (daily.time.isEmpty()) return

            weatherDao.insert(
                WeatherData(
                    entryId = entryId,
                    latitude = lat,
                    longitude = lng,
                    date = dateStr,
                    temperatureMax = daily.temperatureMax.firstOrNull(),
                    temperatureMin = daily.temperatureMin.firstOrNull(),
                    pressureMean = daily.pressureMean.firstOrNull(),
                    rainSum = daily.rainSum.firstOrNull()
                )
            )
        } catch (_: Exception) {
            // Network failure — will retry on next sync
        }
    }

    suspend fun syncMissingWeatherData() {
        val entryIds = weatherDao.getEntryIdsWithoutWeather()
        for (entryId in entryIds) {
            fetchAndStoreWeatherForEntry(entryId)
        }
    }

    suspend fun getWeatherForDateRange(startMillis: Long, endMillis: Long): List<WeatherData> {
        return weatherDao.getByDateRange(startMillis, endMillis)
    }
}
