package com.example.headachetracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weather_data",
    foreignKeys = [
        ForeignKey(
            entity = HeadacheEntry::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entry_id", unique = true)]
)
data class WeatherData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "entry_id")
    val entryId: Long,

    val latitude: Double,

    val longitude: Double,

    val date: String,

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
