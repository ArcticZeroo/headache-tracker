package com.example.headachetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HeadacheEntry::class], version = 1, exportSchema = true)
abstract class HeadacheDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao

    companion object {
        @Volatile
        private var INSTANCE: HeadacheDatabase? = null

        fun getInstance(context: Context): HeadacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HeadacheDatabase::class.java,
                    "headache_tracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
