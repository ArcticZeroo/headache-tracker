package com.example.headachetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyWeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weather: DailyWeather)

    @Query("SELECT * FROM daily_weather WHERE date = :date")
    suspend fun getByDate(date: String): DailyWeather?

    @Query("SELECT * FROM daily_weather WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<DailyWeather>

    @Query("SELECT date FROM daily_weather WHERE date IN (:dates)")
    suspend fun getExistingDates(dates: List<String>): List<String>
}
