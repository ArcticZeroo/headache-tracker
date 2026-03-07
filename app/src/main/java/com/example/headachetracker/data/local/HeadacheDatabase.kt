package com.example.headachetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HeadacheEntry::class, DailyWeather::class],
    version = 3,
    exportSchema = true
)
abstract class HeadacheDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao
    abstract fun dailyWeatherDao(): DailyWeatherDao

    companion object {
        @Volatile
        private var INSTANCE: HeadacheDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE headache_entries ADD COLUMN location_name TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_weather (
                        date TEXT NOT NULL PRIMARY KEY,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        temperature_max REAL,
                        temperature_min REAL,
                        pressure_mean REAL,
                        rain_sum REAL,
                        fetched_at INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO daily_weather
                        (date, latitude, longitude, temperature_max, temperature_min, pressure_mean, rain_sum, fetched_at)
                    SELECT date, latitude, longitude, temperature_max, temperature_min, pressure_mean, rain_sum, fetched_at
                    FROM weather_data
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS weather_data")
            }
        }

        fun getInstance(context: Context): HeadacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeadacheDatabase::class.java,
                    "headache_tracker.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
