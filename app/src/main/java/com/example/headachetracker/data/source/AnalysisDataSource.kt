package com.example.headachetracker.data.source

import com.example.headachetracker.data.model.TimeSeriesData

interface AnalysisDataSource {
    val seriesName: String
    suspend fun getDataForRange(startMillis: Long, endMillis: Long): TimeSeriesData
}
