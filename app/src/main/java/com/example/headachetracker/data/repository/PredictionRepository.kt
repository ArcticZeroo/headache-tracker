package com.example.headachetracker.data.repository

import com.example.headachetracker.data.local.DailyWeather
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.ml.FeatureExtractor
import com.example.headachetracker.data.ml.MIN_TRAINING_ENTRIES
import com.example.headachetracker.data.ml.ModelStorage
import com.example.headachetracker.data.ml.PredictionEngine
import com.example.headachetracker.data.weather.OpenMeteoForecastService
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

enum class ModelStatus { NOT_ENOUGH_DATA, UNTRAINED, READY }

data class DayPrediction(
    val probability: Double,
    val forecastSummary: DailyWeather?
)

data class PredictionResult(
    val status: ModelStatus,
    val today: DayPrediction?,
    val tomorrow: DayPrediction?,
    val entryCount: Int,
    val trainingSampleCount: Int,
    val trainedAtMillis: Long?
)

@Singleton
class PredictionRepository @Inject constructor(
    private val headacheDao: HeadacheDao,
    private val forecastService: OpenMeteoForecastService,
    private val featureExtractor: FeatureExtractor,
    private val predictionEngine: PredictionEngine,
    private val modelStorage: ModelStorage
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val zone: ZoneId get() = ZoneId.systemDefault()

    suspend fun getEntryCount(): Int = headacheDao.getEntryCount()

    /**
     * Returns prediction probabilities for today and tomorrow (where data is
     * available), along with model metadata.
     *
     * Fetches a fresh 2-day forecast each call; the forecast response is small
     * so caching is not warranted here.
     */
    suspend fun getPrediction(): PredictionResult {
        val entryCount = headacheDao.getEntryCount()

        if (entryCount < MIN_TRAINING_ENTRIES) {
            return PredictionResult(
                status = ModelStatus.NOT_ENOUGH_DATA,
                today = null,
                tomorrow = null,
                entryCount = entryCount,
                trainingSampleCount = 0,
                trainedAtMillis = null
            )
        }

        val model = modelStorage.load()
            ?: return PredictionResult(
                status = ModelStatus.UNTRAINED,
                today = null,
                tomorrow = null,
                entryCount = entryCount,
                trainingSampleCount = 0,
                trainedAtMillis = null
            )

        // Fetch 2-day forecast using the most recent entry location
        val locationEntry = headacheDao.getEntriesWithLocation().lastOrNull()
            ?: headacheDao.getLatestEntry()

        val lat = locationEntry?.latitude
        val lon = locationEntry?.longitude

        if (lat == null || lon == null) {
            return PredictionResult(
                status = ModelStatus.READY,
                today = null,
                tomorrow = null,
                entryCount = entryCount,
                trainingSampleCount = model.trainingSampleCount,
                trainedAtMillis = model.trainedAtMillis
            )
        }

        return try {
            val forecastResponse = forecastService.getForecast(lat, lon)
            val forecastData = forecastResponse.daily
                ?: return PredictionResult(
                    status = ModelStatus.READY,
                    today = null,
                    tomorrow = null,
                    entryCount = entryCount,
                    trainingSampleCount = model.trainingSampleCount,
                    trainedAtMillis = model.trainedAtMillis
                )

            val today = LocalDate.now(zone)
            val forecastDates = forecastData.time.map { LocalDate.parse(it, dateFormatter) }

            val inference = featureExtractor.buildInferenceFeatures(forecastData, forecastDates)

            val todayPrediction = inference.today?.let { features ->
                DayPrediction(
                    probability = predictionEngine.predict(features) ?: 0.0,
                    forecastSummary = inference.todayForecastSummary
                )
            }

            val tomorrowPrediction = inference.tomorrow?.let { features ->
                DayPrediction(
                    probability = predictionEngine.predict(features) ?: 0.0,
                    forecastSummary = inference.tomorrowForecastSummary
                )
            }

            PredictionResult(
                status = ModelStatus.READY,
                today = todayPrediction,
                tomorrow = tomorrowPrediction,
                entryCount = entryCount,
                trainingSampleCount = model.trainingSampleCount,
                trainedAtMillis = model.trainedAtMillis
            )
        } catch (_: Exception) {
            PredictionResult(
                status = ModelStatus.READY,
                today = null,
                tomorrow = null,
                entryCount = entryCount,
                trainingSampleCount = model.trainingSampleCount,
                trainedAtMillis = model.trainedAtMillis
            )
        }
    }
}
