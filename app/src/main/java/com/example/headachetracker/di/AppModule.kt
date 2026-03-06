package com.example.headachetracker.di

import com.example.headachetracker.data.source.AnalysisDataSource
import com.example.headachetracker.data.source.HeadacheDataSource
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
}
