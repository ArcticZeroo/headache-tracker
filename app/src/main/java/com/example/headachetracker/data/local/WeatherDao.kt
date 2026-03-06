package com.example.headachetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weatherData: WeatherData): Long

    @Query("SELECT * FROM weather_data WHERE entry_id = :entryId")
    suspend fun getByEntryId(entryId: Long): WeatherData?

    @Query("SELECT * FROM weather_data WHERE entry_id IN (:entryIds)")
    suspend fun getByEntryIds(entryIds: List<Long>): List<WeatherData>

    @Query("""
        SELECT w.* FROM weather_data w
        INNER JOIN headache_entries h ON w.entry_id = h.id
        WHERE h.timestamp BETWEEN :start AND :end
        ORDER BY h.timestamp ASC
    """)
    suspend fun getByDateRange(start: Long, end: Long): List<WeatherData>

    @Query("""
        SELECT h.id FROM headache_entries h
        LEFT JOIN weather_data w ON h.id = w.entry_id
        WHERE h.latitude IS NOT NULL AND h.longitude IS NOT NULL AND w.id IS NULL
    """)
    suspend fun getEntryIdsWithoutWeather(): List<Long>

    @Query("DELETE FROM weather_data WHERE entry_id = :entryId")
    suspend fun deleteByEntryId(entryId: Long)
}
