package com.example.headachetracker.data.weather

import com.example.headachetracker.data.local.DailyWeather
import com.example.headachetracker.data.local.DailyWeatherDao
import com.example.headachetracker.data.local.HeadacheDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherService: OpenMeteoService,
    private val dailyWeatherDao: DailyWeatherDao,
    private val headacheDao: HeadacheDao
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun syncWeatherAroundEntries() {
        val entries = headacheDao.getEntriesWithLocation()
        if (entries.isEmpty()) return

        val zone = ZoneId.systemDefault()

        // Group entries by approximate location (rounded to ~1 km precision) to batch requests
        val byLocation = entries.groupBy { entry ->
            val lat = Math.round(entry.latitude!! * 100).toDouble() / 100.0
            val lng = Math.round(entry.longitude!! * 100).toDouble() / 100.0
            lat to lng
        }

        for ((_, locationEntries) in byLocation) {
            val refEntry = locationEntries.first()
            val lat = refEntry.latitude!!
            val lng = refEntry.longitude!!

            // Collect D-1, D, D+1 for every entry at this location
            val datesToFetch = mutableSetOf<String>()
            for (entry in locationEntries) {
                val date = Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()
                datesToFetch += date.minusDays(1).format(dateFormatter)
                datesToFetch += date.format(dateFormatter)
                datesToFetch += date.plusDays(1).format(dateFormatter)
            }

            val existingDates = dailyWeatherDao.getExistingDates(datesToFetch.toList()).toSet()
            val missingDates = datesToFetch - existingDates
            if (missingDates.isEmpty()) continue

            val sorted = missingDates.sorted()
            try {
                fetchAndStoreRange(lat, lng, sorted.first(), sorted.last())
            } catch (_: Exception) {
                // Network failure — will retry on next sync
            }
        }
    }

    suspend fun ensureWeatherForDateRange(startDate: String, endDate: String, lat: Double, lng: Double) {
        val start = LocalDate.parse(startDate, dateFormatter)
        val end = LocalDate.parse(endDate, dateFormatter)
        val allDates = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.format(dateFormatter) }
            .toList()

        val existing = dailyWeatherDao.getExistingDates(allDates).toSet()
        val missing = allDates.toSet() - existing
        if (missing.isEmpty()) return

        val sorted = missing.sorted()
        try {
            fetchAndStoreRange(lat, lng, sorted.first(), sorted.last())
        } catch (_: Exception) {
            // Network failure — caller should handle missing data gracefully
        }
    }

    suspend fun getWeatherForDateRange(startDate: String, endDate: String): List<DailyWeather> {
        return dailyWeatherDao.getByDateRange(startDate, endDate)
    }

    private suspend fun fetchAndStoreRange(lat: Double, lng: Double, startDate: String, endDate: String) {
        val response = weatherService.getHistoricalWeather(
            latitude = lat,
            longitude = lng,
            startDate = startDate,
            endDate = endDate
        )
        val daily = response.daily ?: return
        for (i in daily.time.indices) {
            dailyWeatherDao.insert(
                DailyWeather(
                    date = daily.time[i],
                    latitude = lat,
                    longitude = lng,
                    temperatureMax = daily.temperatureMax.getOrNull(i),
                    temperatureMin = daily.temperatureMin.getOrNull(i),
                    pressureMean = daily.pressureMean.getOrNull(i),
                    rainSum = daily.rainSum.getOrNull(i)
                )
            )
        }
    }
}
