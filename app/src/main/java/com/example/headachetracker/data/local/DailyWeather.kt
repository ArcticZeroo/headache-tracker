package com.example.headachetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_weather")
data class DailyWeather(
    @PrimaryKey
    val date: String,

    val latitude: Double,

    val longitude: Double,

    @ColumnInfo(name = "temperature_max")
    val temperatureMax: Double? = null,

    @ColumnInfo(name = "temperature_min")
    val temperatureMin: Double? = null,

    @ColumnInfo(name = "pressure_mean")
    val pressureMean: Double? = null,

    @ColumnInfo(name = "rain_sum")
    val rainSum: Double? = null,

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()
)
