package com.example.headachetracker.data.correlation

import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.weather.WeatherRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrelationRepository @Inject constructor(
    private val headacheDao: HeadacheDao,
    private val weatherRepository: WeatherRepository,
    private val healthConnectRepository: HealthConnectRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun computeCorrelations(startMillis: Long, endMillis: Long): List<CorrelationResult> {
        val entries = headacheDao.getEntriesByDateRange(startMillis, endMillis)
        if (entries.size < CorrelationEngine.MIN_SAMPLE_SIZE) return emptyList()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val painByDate = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()
        }.filterKeys { it != today }
            .mapValues { (_, dayEntries) ->
                dayEntries.maxOf { it.painLevel }.toDouble()
            }

        val results = mutableListOf<CorrelationResult>()

        // Sleep correlation
        val sleepData = healthConnectRepository.getSleepData(startMillis, endMillis)
        if (sleepData.isNotEmpty()) {
            val sleepByDate = sleepData.associate { it.date to it.totalDurationHours }
            val paired = pairData(painByDate, sleepByDate)
            if (paired != null) {
                val (painValues, sleepValues) = paired
                val r = CorrelationEngine.pearson(painValues, sleepValues)
                if (r != null) {
                    val pctDiff = CorrelationEngine.computePercentageDiff(painValues, sleepValues)
                    results.add(CorrelationEngine.interpret("Sleep Duration", r, painValues.size, pctDiff))
                }
            }
        }

        // Steps / exercise correlation
        val fitnessData = healthConnectRepository.getFitnessData(startMillis, endMillis)
        if (fitnessData.isNotEmpty()) {
            val stepsByDate = fitnessData.associate { it.date to it.steps.toDouble() }
            val exerciseByDate = fitnessData.associate { it.date to it.exerciseMinutes.toDouble() }

            val stepsResult = pairAndCorrelate(painByDate, stepsByDate, "Daily Steps")
            if (stepsResult != null) results.add(stepsResult)

            val exerciseResult = pairAndCorrelate(painByDate, exerciseByDate, "Exercise Duration")
            if (exerciseResult != null) results.add(exerciseResult)
        }

        // Ensure weather data covering D-1 through D+1 for the full range
        val adjStart = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            .minusDays(1).format(dateFormatter)
        val adjEnd = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
            .plusDays(1).format(dateFormatter)
        val locationEntry = entries.firstOrNull { it.latitude != null && it.longitude != null }
        if (locationEntry != null) {
            try {
                weatherRepository.ensureWeatherForDateRange(
                    adjStart, adjEnd,
                    locationEntry.latitude!!, locationEntry.longitude!!
                )
            } catch (_: Exception) { }
        }

        // Weather correlations (same-day + adjacent-day)
        val weatherData = weatherRepository.getWeatherForDateRange(adjStart, adjEnd)
        if (weatherData.isNotEmpty()) {
            val weatherByDate = weatherData.associateBy { LocalDate.parse(it.date, dateFormatter) }

            val tempByDate = mutableMapOf<LocalDate, Double>()
            val pressureByDate = mutableMapOf<LocalDate, Double>()
            val rainByDate = mutableMapOf<LocalDate, Double>()
            val prevRainByDate = mutableMapOf<LocalDate, Double>()
            val prevPressureByDate = mutableMapOf<LocalDate, Double>()
            val nextRainByDate = mutableMapOf<LocalDate, Double>()
            val nextPressureByDate = mutableMapOf<LocalDate, Double>()

            for (date in painByDate.keys) {
                weatherByDate[date]?.let { w ->
                    w.temperatureMax?.let { max ->
                        w.temperatureMin?.let { min ->
                            tempByDate[date] = (max + min) / 2.0
                        }
                    }
                    w.pressureMean?.let { pressureByDate[date] = it }
                    w.rainSum?.let { rainByDate[date] = it }
                }
                weatherByDate[date.minusDays(1)]?.let { prev ->
                    prev.rainSum?.let { prevRainByDate[date] = it }
                    prev.pressureMean?.let { prevPressureByDate[date] = it }
                }
                weatherByDate[date.plusDays(1)]?.let { next ->
                    next.rainSum?.let { nextRainByDate[date] = it }
                    next.pressureMean?.let { nextPressureByDate[date] = it }
                }
            }

            val tempResult = pairAndCorrelate(painByDate, tempByDate, "Temperature")
            if (tempResult != null) results.add(tempResult)

            val pressureResult = pairAndCorrelate(painByDate, pressureByDate, "Barometric Pressure")
            if (pressureResult != null) results.add(pressureResult)

            val rainResult = pairAndCorrelate(painByDate, rainByDate, "Rainfall")
            if (rainResult != null) results.add(rainResult)

            val prevRainResult = pairAndCorrelate(painByDate, prevRainByDate, "Prev. Day Rain")
            if (prevRainResult != null) results.add(prevRainResult)

            val prevPressureResult = pairAndCorrelate(painByDate, prevPressureByDate, "Prev. Day Pressure")
            if (prevPressureResult != null) results.add(prevPressureResult)

            val nextRainResult = pairAndCorrelate(painByDate, nextRainByDate, "Next Day Rain")
            if (nextRainResult != null) results.add(nextRainResult)

            val nextPressureResult = pairAndCorrelate(painByDate, nextPressureByDate, "Next Day Pressure")
            if (nextPressureResult != null) results.add(nextPressureResult)
        }

        return results
    }

    private fun pairAndCorrelate(
        painByDate: Map<LocalDate, Double>,
        factorByDate: Map<LocalDate, Double>,
        factorName: String
    ): CorrelationResult? {
        val paired = pairData(painByDate, factorByDate) ?: return null
        val (painValues, factorValues) = paired
        val r = CorrelationEngine.pearson(painValues, factorValues) ?: return null
        val pctDiff = CorrelationEngine.computePercentageDiff(painValues, factorValues)
        return CorrelationEngine.interpret(factorName, r, painValues.size, pctDiff)
    }

    private fun pairData(
        painByDate: Map<LocalDate, Double>,
        factorByDate: Map<LocalDate, Double>
    ): Pair<List<Double>, List<Double>>? {
        val commonDates = painByDate.keys.intersect(factorByDate.keys).toSortedSet()
        if (commonDates.size < CorrelationEngine.MIN_SAMPLE_SIZE) return null

        val painValues = commonDates.map { painByDate.getValue(it) }
        val factorValues = commonDates.map { factorByDate.getValue(it) }
        return painValues to factorValues
    }
}
