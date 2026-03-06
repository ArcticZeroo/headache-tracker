package com.example.headachetracker.data.source

import androidx.compose.ui.graphics.Color
import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import java.time.ZoneId
import javax.inject.Inject

class FitnessDataSource @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : AnalysisDataSource {

    override val seriesName = "Steps (thousands)"

    override suspend fun getDataForRange(startMillis: Long, endMillis: Long): TimeSeriesData {
        val fitnessData = healthConnectRepository.getFitnessData(startMillis, endMillis)
        val zone = ZoneId.systemDefault()

        return TimeSeriesData(
            seriesName = seriesName,
            color = Color(0xFF66BB6A),
            points = fitnessData.map { data ->
                TimeSeriesPoint(
                    timestamp = data.date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    value = (data.steps / 1000f),
                    label = "${data.steps} steps, ${data.exerciseMinutes}min exercise"
                )
            }
        )
    }
}
