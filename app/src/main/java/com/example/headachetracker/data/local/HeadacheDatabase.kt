package com.example.headachetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HeadacheEntry::class, WeatherData::class],
    version = 2,
    exportSchema = true
)
abstract class HeadacheDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: HeadacheDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE headache_entries ADD COLUMN location_name TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): HeadacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeadacheDatabase::class.java,
                    "headache_tracker.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
