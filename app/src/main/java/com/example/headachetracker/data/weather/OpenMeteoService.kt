package com.example.headachetracker.data.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val daily: DailyData?
)

@JsonClass(generateAdapter = true)
data class DailyData(
    val time: List<String>,
    @Json(name = "temperature_2m_max") val temperatureMax: List<Double?>,
    @Json(name = "temperature_2m_min") val temperatureMin: List<Double?>,
    @Json(name = "rain_sum") val rainSum: List<Double?>,
    @Json(name = "surface_pressure_mean") val pressureMean: List<Double?>
)

interface OpenMeteoService {

    @GET("v1/archive")
    suspend fun getHistoricalWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,rain_sum,surface_pressure_mean",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
