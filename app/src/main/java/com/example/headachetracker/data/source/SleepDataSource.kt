package com.example.headachetracker.data.source

import androidx.compose.ui.graphics.Color
import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import java.time.ZoneId
import javax.inject.Inject

class SleepDataSource @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : AnalysisDataSource {

    override val seriesName = "Sleep (hours)"

    override suspend fun getDataForRange(startMillis: Long, endMillis: Long): TimeSeriesData {
        val sleepData = healthConnectRepository.getSleepData(startMillis, endMillis)
        val zone = ZoneId.systemDefault()

        return TimeSeriesData(
            seriesName = seriesName,
            color = Color(0xFF5C6BC0),
            points = sleepData.map { data ->
                TimeSeriesPoint(
                    timestamp = data.date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    value = data.totalDurationHours.toFloat(),
                    label = String.format("%.1fh sleep", data.totalDurationHours)
                )
            }
        )
    }
}
