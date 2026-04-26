package com.figago.domain.repository

import com.figago.data.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для управления парком колясок (Профилями).
 */
interface ProfileRepository {

    /** Получить список всех доступных профилей. */
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    /** Получить профиль по ID. */
    suspend fun getProfileById(id: Long): ProfileEntity?
    fun getProfileByIdFlow(id: Long): Flow<ProfileEntity?>

    /** Получить текущий активный профиль (ID берется из SettingsRepository). */
    fun getActiveProfile(): Flow<ProfileEntity?>

    /** Сохранить (или обновить) профиль. Возвращает ID нового профиля. */
    suspend fun saveProfile(profile: ProfileEntity): Long

    /** Удалить профиль. */
    suspend fun deleteProfile(profile: ProfileEntity)
}
