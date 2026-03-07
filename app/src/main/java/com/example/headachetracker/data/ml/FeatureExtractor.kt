package com.example.headachetracker.data.ml

import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.local.DailyWeather
import com.example.headachetracker.data.local.DailyWeatherDao
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.weather.ForecastDailyData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class FeatureExtractor @Inject constructor(
    private val headacheDao: HeadacheDao,
    private val dailyWeatherDao: DailyWeatherDao,
    private val healthConnectRepository: HealthConnectRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val zone: ZoneId get() = ZoneId.systemDefault()

    // -----------------------------------------------------------------------
    // Training data
    // -----------------------------------------------------------------------

    /**
     * Builds labeled training samples from historical data.
     *
     * A "candidate day D" is any day present in the weather archive. For each
     * such day, features are constructed from day D's weather, day D-1's
     * weather, and available Health Connect data. The label is 1 if there is
     * any headache entry on day D, 0 otherwise.
     *
     * To mitigate class imbalance, no-headache days are capped at
     * [maxNegativeRatio] × (headache day count).
     */
    suspend fun buildTrainingSamples(maxNegativeRatio: Int = 3): List<TrainingSample> {
        val allEntries = headacheDao.getEntriesByDateRange(0L, Long.MAX_VALUE)
        if (allEntries.isEmpty()) return emptyList()

        val headacheDays = allEntries
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .mapValues { (_, entries) -> entries.maxOf { it.painLevel } > 0 }
            .filterValues { it }
            .keys

        // Determine the date range we have weather for
        val oldest = allEntries.minOf { it.timestamp }
        val oldestDate = Instant.ofEpochMilli(oldest).atZone(zone).toLocalDate()
        val yesterday = LocalDate.now(zone).minusDays(1)

        val startStr = oldestDate.minusDays(1).format(dateFormatter)
        val endStr = yesterday.format(dateFormatter)
        val weatherRecords = dailyWeatherDao.getByDateRange(startStr, endStr)
        if (weatherRecords.isEmpty()) return emptyList()

        val weatherByDate = weatherRecords.associateBy { LocalDate.parse(it.date, dateFormatter) }

        // Health Connect data for the full range
        val healthStart = oldestDate.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val healthEnd = yesterday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sleepByDate = healthConnectRepository.getSleepData(healthStart, healthEnd)
            .associate { it.date to it.totalDurationHours }
        val fitnessByDate = healthConnectRepository.getFitnessData(healthStart, healthEnd)
            .associate { it.date to it }

        // Build samples for every candidate day that has weather for D and D-1
        val positiveSamples = mutableListOf<TrainingSample>()
        val negativeSamples = mutableListOf<TrainingSample>()

        val candidateDays = weatherByDate.keys
            .filter { it.isAfter(oldestDate.minusDays(1)) && !it.isAfter(yesterday) }
            .sorted()

        for (day in candidateDays) {
            val dayWeather = weatherByDate[day] ?: continue
            val prevWeather = weatherByDate[day.minusDays(1)] ?: continue
            if (!dayWeather.hasRequiredFields() || !prevWeather.hasRequiredFields()) continue

            val features = buildFeatures(
                targetDay = day,
                dayWeather = dayWeather,
                prevDayWeather = prevWeather,
                sleepByDate = sleepByDate,
                fitnessByDate = fitnessByDate.mapValues { it.value.steps to it.value.exerciseMinutes }
            ) ?: continue

            val label = if (day in headacheDays) 1 else 0
            val sample = TrainingSample(features, label)
            if (label == 1) positiveSamples += sample else negativeSamples += sample
        }

        if (positiveSamples.isEmpty()) return emptyList()

        // Cap negatives to avoid severe imbalance
        val cappedNegatives = if (negativeSamples.size > positiveSamples.size * maxNegativeRatio) {
            negativeSamples.shuffled().take(positiveSamples.size * maxNegativeRatio)
        } else {
            negativeSamples
        }

        return (positiveSamples + cappedNegatives).shuffled()
    }

    // -----------------------------------------------------------------------
    // Inference vectors (today & tomorrow)
    // -----------------------------------------------------------------------

    /**
     * Builds inference feature vectors for [today] and [tomorrow] using live
     * forecast data and today's Health Connect data.
     *
     * Returns null for a day if required weather data is unavailable.
     */
    suspend fun buildInferenceFeatures(
        forecastData: ForecastDailyData,
        forecastDates: List<LocalDate>
    ): InferenceFeatures {
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        // Build forecast lookup
        val forecastByDate = forecastDates.zip(
            forecastData.time.indices.map { i ->
                DailyWeather(
                    date = forecastData.time[i],
                    latitude = 0.0,
                    longitude = 0.0,
                    temperatureMax = forecastData.temperature_2m_max.getOrNull(i),
                    temperatureMin = forecastData.temperature_2m_min.getOrNull(i),
                    pressureMean = forecastData.surface_pressure_mean.getOrNull(i),
                    rainSum = forecastData.rain_sum.getOrNull(i),
                    fetchedAt = System.currentTimeMillis()
                )
            }
        ).toMap()

        val archiveYesterday = dailyWeatherDao.getByDate(yesterday.format(dateFormatter))
        val archiveToday = dailyWeatherDao.getByDate(today.format(dateFormatter))

        // Health data: today's sleep (ended this morning) and today's fitness
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val sleepList = healthConnectRepository.getSleepData(
            yesterday.atStartOfDay(zone).toInstant().toEpochMilli(), now
        )
        val fitnessList = healthConnectRepository.getFitnessData(
            yesterday.atStartOfDay(zone).toInstant().toEpochMilli(), now
        )
        val sleepByDate = sleepList.associate { it.date to it.totalDurationHours }
        val fitnessByDate = fitnessList.associate { it.date to (it.steps to it.exerciseMinutes) }

        // Today's prediction: target = today, D-1 = yesterday
        val todayForecast = forecastByDate[today] ?: archiveToday
        val todayFeatures = if (todayForecast != null) {
            val prevWeather = archiveYesterday ?: forecastByDate[yesterday]
            if (prevWeather != null && todayForecast.hasRequiredFields() && prevWeather.hasRequiredFields()) {
                buildFeatures(
                    targetDay = today,
                    dayWeather = todayForecast,
                    prevDayWeather = prevWeather,
                    sleepByDate = sleepByDate,
                    fitnessByDate = fitnessByDate
                )
            } else null
        } else null

        // Tomorrow's prediction: target = tomorrow, D-1 = today
        val tomorrowForecast = forecastByDate[tomorrow]
        val todayWeatherForTomorrow = archiveToday ?: forecastByDate[today]
        val tomorrowFeatures = if (tomorrowForecast != null && todayWeatherForTomorrow != null &&
            tomorrowForecast.hasRequiredFields() && todayWeatherForTomorrow.hasRequiredFields()
        ) {
            buildFeatures(
                targetDay = tomorrow,
                dayWeather = tomorrowForecast,
                prevDayWeather = todayWeatherForTomorrow,
                sleepByDate = sleepByDate,
                fitnessByDate = fitnessByDate
            )
        } else null

        return InferenceFeatures(
            today = todayFeatures,
            tomorrow = tomorrowFeatures,
            todayForecastSummary = forecastByDate[today] ?: archiveToday,
            tomorrowForecastSummary = forecastByDate[tomorrow]
        )
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private fun buildFeatures(
        targetDay: LocalDate,
        dayWeather: DailyWeather,
        prevDayWeather: DailyWeather,
        sleepByDate: Map<LocalDate, Double>,
        fitnessByDate: Map<LocalDate, Pair<Long, Long>>
    ): PredictionFeatures? {
        val tempMax = dayWeather.temperatureMax ?: return null
        val tempMin = dayWeather.temperatureMin ?: return null
        val pressure = dayWeather.pressureMean ?: return null
        val rain = dayWeather.rainSum ?: 0.0
        val prevPressure = prevDayWeather.pressureMean ?: return null

        val prevDay = targetDay.minusDays(1)
        val sleepHours = sleepByDate[targetDay] ?: sleepByDate[prevDay] ?: Double.NaN
        val (steps, exercise) = fitnessByDate[prevDay] ?: (null to null)

        val dayOfWeek = targetDay.dayOfWeek.value.toDouble()  // 1 = Monday … 7 = Sunday
        val month = targetDay.monthValue.toDouble()

        return PredictionFeatures(
            dayTempMax = tempMax,
            dayTempMin = tempMin,
            dayPressure = pressure,
            dayRain = rain,
            pressureDelta = pressure - prevPressure,
            prevDayPressure = prevPressure,
            prevNightSleepHours = if (sleepHours.isNaN()) Double.NaN else sleepHours,
            prevDayStepsThousands = (steps ?: Long.MIN_VALUE).let {
                if (it == Long.MIN_VALUE) Double.NaN else it / 1000.0
            },
            prevDayExerciseMinutes = (exercise ?: Long.MIN_VALUE).let {
                if (it == Long.MIN_VALUE) Double.NaN else it.toDouble()
            },
            dayOfWeekSin = sin(2 * PI * (dayOfWeek - 1) / 7),
            dayOfWeekCos = cos(2 * PI * (dayOfWeek - 1) / 7),
            monthSin = sin(2 * PI * (month - 1) / 12),
            monthCos = cos(2 * PI * (month - 1) / 12)
        )
    }

    private fun DailyWeather.hasRequiredFields() =
        temperatureMax != null && temperatureMin != null && pressureMean != null
}

data class InferenceFeatures(
    val today: PredictionFeatures?,
    val tomorrow: PredictionFeatures?,
    val todayForecastSummary: DailyWeather?,
    val tomorrowForecastSummary: DailyWeather?
)
