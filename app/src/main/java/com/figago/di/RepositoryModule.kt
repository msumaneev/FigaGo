package com.figago.di

import com.figago.data.repository.LedEventRepositoryImpl
import com.figago.data.repository.SessionRepositoryImpl
import com.figago.data.repository.TrackRepositoryImpl
import com.figago.data.repository.LampStatisticsRepositoryImpl
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import com.figago.domain.repository.LampStatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль привязки интерфейсов репозиториев к их реализациям.
 * Использует @Binds для нулевых накладных расходов.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds
    @Singleton
    abstract fun bindLedEventRepository(impl: LedEventRepositoryImpl): LedEventRepository

    @Binds
    @Singleton
    abstract fun bindLampStatisticsRepository(impl: LampStatisticsRepositoryImpl): LampStatisticsRepository
}
