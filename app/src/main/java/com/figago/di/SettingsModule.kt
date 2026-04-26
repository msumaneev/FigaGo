package com.figago.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.figago.data.repository.SettingsRepositoryImpl
import com.figago.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Расширение Context для инициализации DataStore.
 * Файл настроек: figago_settings.preferences_pb.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "figago_settings",
)

/**
 * Hilt-модуль для DataStore и SettingsRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsProviderModule {

    /** Предоставляет единственный экземпляр DataStore<Preferences>. */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

/**
 * Hilt-модуль привязки интерфейса SettingsRepository к реализации.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: com.figago.data.repository.ProfileRepositoryImpl): com.figago.domain.repository.ProfileRepository
}
