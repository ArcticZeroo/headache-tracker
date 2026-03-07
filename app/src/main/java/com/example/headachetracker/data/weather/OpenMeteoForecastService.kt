package com.example.headachetracker.data.weather

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    val daily: ForecastDailyData?
)

@JsonClass(generateAdapter = true)
data class ForecastDailyData(
    val time: List<String>,
    val temperature_2m_max: List<Double?>,
    val temperature_2m_min: List<Double?>,
    val rain_sum: List<Double?>,
    val surface_pressure_mean: List<Double?>
)

interface OpenMeteoForecastService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,rain_sum,surface_pressure_mean",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timezone") timezone: String = "auto"
    ): ForecastResponse
}
