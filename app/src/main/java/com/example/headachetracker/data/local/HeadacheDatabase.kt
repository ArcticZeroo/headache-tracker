package com.example.headachetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HeadacheEntry::class], version = 1, exportSchema = true)
abstract class HeadacheDatabase : RoomDatabase() {
    abstract fun headacheDao(): HeadacheDao
}
