package com.example.headachetracker.di

import android.content.Context
import androidx.room.Room
import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.HeadacheDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HeadacheDatabase {
        return Room.databaseBuilder(
            context,
            HeadacheDatabase::class.java,
            "headache_tracker.db"
        ).build()
    }

    @Provides
    fun provideHeadacheDao(database: HeadacheDatabase): HeadacheDao {
        return database.headacheDao()
    }
}
