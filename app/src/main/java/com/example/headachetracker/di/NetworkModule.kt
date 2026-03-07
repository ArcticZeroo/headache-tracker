package com.example.headachetracker.di

import com.example.headachetracker.data.weather.OpenMeteoForecastService
import com.example.headachetracker.data.weather.OpenMeteoService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    @Named("archive")
    fun provideArchiveRetrofit(moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://archive-api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    @Named("forecast")
    fun provideForecastRetrofit(moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenMeteoService(@Named("archive") retrofit: Retrofit): OpenMeteoService {
        return retrofit.create(OpenMeteoService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenMeteoForecastService(@Named("forecast") retrofit: Retrofit): OpenMeteoForecastService {
        return retrofit.create(OpenMeteoForecastService::class.java)
    }
}
