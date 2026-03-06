package com.example.headachetracker.data.source

import androidx.compose.ui.graphics.Color
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import javax.inject.Inject

class HeadacheDataSource @Inject constructor(
    private val dao: HeadacheDao
) : AnalysisDataSource {

    override val seriesName = "Pain Level"

    override suspend fun getDataForRange(startMillis: Long, endMillis: Long): TimeSeriesData {
        val entries = dao.getEntriesByDateRange(startMillis, endMillis)
        return TimeSeriesData(
            seriesName = seriesName,
            color = Color(0xFFE53935),
            points = entries.map { entry ->
                TimeSeriesPoint(
                    timestamp = entry.timestamp,
                    value = entry.painLevel.toFloat(),
                    label = entry.notes
                )
            }
        )
    }
}
