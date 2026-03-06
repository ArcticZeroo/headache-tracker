package com.example.headachetracker.data.correlation

import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.WeatherDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrelationRepository @Inject constructor(
    private val headacheDao: HeadacheDao,
    private val weatherDao: WeatherDao,
    private val healthConnectRepository: HealthConnectRepository
) {
    suspend fun computeCorrelations(startMillis: Long, endMillis: Long): List<CorrelationResult> {
        val entries = headacheDao.getEntriesByDateRange(startMillis, endMillis)
        if (entries.size < CorrelationEngine.MIN_SAMPLE_SIZE) return emptyList()

        val zone = ZoneId.systemDefault()
        val painByDate = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.timestamp).atZone(zone).toLocalDate()
        }.mapValues { (_, dayEntries) ->
            dayEntries.maxOf { it.painLevel }.toDouble()
        }

        val results = mutableListOf<CorrelationResult>()

        // Sleep correlation: match sleep (night before) to headache
        val sleepData = healthConnectRepository.getSleepData(startMillis, endMillis)
        if (sleepData.isNotEmpty()) {
            val sleepByDate = sleepData.associate { it.date to it.totalDurationHours }
            val paired = pairData(painByDate, sleepByDate)
            if (paired != null) {
                val (painValues, sleepValues) = paired
                val r = CorrelationEngine.pearson(painValues, sleepValues)
                if (r != null) {
                    results.add(CorrelationEngine.interpret("Sleep Duration", r, painValues.size))
                }
            }
        }

        // Steps correlation
        val fitnessData = healthConnectRepository.getFitnessData(startMillis, endMillis)
        if (fitnessData.isNotEmpty()) {
            val stepsByDate = fitnessData.associate { it.date to it.steps.toDouble() }
            val exerciseByDate = fitnessData.associate { it.date to it.exerciseMinutes.toDouble() }

            val stepsResult = pairAndCorrelate(painByDate, stepsByDate, "Daily Steps")
            if (stepsResult != null) results.add(stepsResult)

            val exerciseResult = pairAndCorrelate(painByDate, exerciseByDate, "Exercise Duration")
            if (exerciseResult != null) results.add(exerciseResult)
        }

        // Weather correlations
        val weatherData = weatherDao.getByDateRange(startMillis, endMillis)
        if (weatherData.isNotEmpty()) {
            val tempByDate = mutableMapOf<LocalDate, Double>()
            val pressureByDate = mutableMapOf<LocalDate, Double>()
            val rainByDate = mutableMapOf<LocalDate, Double>()

            for (w in weatherData) {
                val date = LocalDate.parse(w.date)
                w.temperatureMax?.let { max ->
                    w.temperatureMin?.let { min ->
                        tempByDate[date] = (max + min) / 2.0
                    }
                }
                w.pressureMean?.let { pressureByDate[date] = it }
                w.rainSum?.let { rainByDate[date] = it }
            }

            val tempResult = pairAndCorrelate(painByDate, tempByDate, "Temperature")
            if (tempResult != null) results.add(tempResult)

            val pressureResult = pairAndCorrelate(painByDate, pressureByDate, "Barometric Pressure")
            if (pressureResult != null) results.add(pressureResult)

            val rainResult = pairAndCorrelate(painByDate, rainByDate, "Rainfall")
            if (rainResult != null) results.add(rainResult)
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
        return CorrelationEngine.interpret(factorName, r, painValues.size)
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
