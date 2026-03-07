package com.example.headachetracker.data.source

import androidx.compose.ui.graphics.Color
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import com.example.headachetracker.data.weather.WeatherRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class WeatherDataSource @Inject constructor(
    private val weatherRepository: WeatherRepository
) : AnalysisDataSource {

    override val seriesName = "Pressure (hPa)"

    override suspend fun getDataForRange(startMillis: Long, endMillis: Long): TimeSeriesData {
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate().format(formatter)
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate().format(formatter)

        val weatherData = weatherRepository.getWeatherForDateRange(startDate, endDate)

        return TimeSeriesData(
            seriesName = seriesName,
            color = Color(0xFFFF9800),
            points = weatherData.mapNotNull { data ->
                val pressure = data.pressureMean ?: return@mapNotNull null
                val date = LocalDate.parse(data.date, formatter)
                val tempMax = data.temperatureMax?.let {
                    val f = it * 9.0 / 5.0 + 32.0
                    "%.0f°F".format(f)
                } ?: ""
                val rain = data.rainSum?.let { "%.1fmm rain".format(it) } ?: ""

                TimeSeriesPoint(
                    timestamp = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    value = pressure.toFloat(),
                    label = listOf(tempMax, rain).filter { it.isNotEmpty() }.joinToString(", ")
                )
            }
        )
    }
}
