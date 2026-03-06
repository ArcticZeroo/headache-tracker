package com.example.headachetracker.data.repository

import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.source.AnalysisDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalysisRepository @Inject constructor(
    private val dataSources: Set<@JvmSuppressWildcards AnalysisDataSource>
) {

    suspend fun getAllSeriesForRange(startMillis: Long, endMillis: Long): List<TimeSeriesData> {
        return dataSources.map { it.getDataForRange(startMillis, endMillis) }
    }

    suspend fun getSeriesByName(
        name: String,
        startMillis: Long,
        endMillis: Long
    ): TimeSeriesData? {
        return dataSources.find { it.seriesName == name }
            ?.getDataForRange(startMillis, endMillis)
    }

    fun getAvailableSeriesNames(): List<String> = dataSources.map { it.seriesName }
}
