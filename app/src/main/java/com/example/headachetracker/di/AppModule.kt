package com.example.headachetracker.di

import com.example.headachetracker.data.source.AnalysisDataSource
import com.example.headachetracker.data.source.FitnessDataSource
import com.example.headachetracker.data.source.HeadacheDataSource
import com.example.headachetracker.data.source.SleepDataSource
import com.example.headachetracker.data.source.WeatherDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @IntoSet
    abstract fun bindHeadacheDataSource(impl: HeadacheDataSource): AnalysisDataSource

    @Binds
    @IntoSet
    abstract fun bindSleepDataSource(impl: SleepDataSource): AnalysisDataSource

    @Binds
    @IntoSet
    abstract fun bindFitnessDataSource(impl: FitnessDataSource): AnalysisDataSource

    @Binds
    @IntoSet
    abstract fun bindWeatherDataSource(impl: WeatherDataSource): AnalysisDataSource
}
