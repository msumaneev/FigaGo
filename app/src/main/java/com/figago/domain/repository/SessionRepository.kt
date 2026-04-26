package com.figago.domain.repository

import com.figago.domain.model.DaySession
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для управления суточными сессиями.
 * Определяет контракт без привязки к конкретной реализации (Room).
 */
interface SessionRepository {

    /** Создать новую сессию. Возвращает id. */
    suspend fun createSession(date: String): Long

    /** Получить активную сессию (может быть только одна). */
    suspend fun getActiveSession(): DaySession?

    /** Реактивная подписка на активную сессию. */
    fun observeActiveSession(): Flow<DaySession?>

    /** Закрыть сессию (is_active = false). */
    suspend fun closeSession(sessionId: Long)

    /** Обновить суммарную дистанцию сессии. */
    suspend fun updateDistance(sessionId: Long, distance: Double)

    /** Получить сессию по id. */
    suspend fun getById(sessionId: Long): DaySession?

    /** Получить все сессии по конкретной дате. */
    suspend fun getAllSessionsByDate(date: String): List<DaySession>

    /** Реактивная подписка на конкретную сессию. */
    fun observeById(sessionId: Long): Flow<DaySession?>

    /** Все сессии (новые первыми) — для истории, отфильтрованные по профилю. */
    fun observeAllSessions(profileId: Long): Flow<List<DaySession>>

    /** Все сессии без разделения на профили, сгруппированные по дате. */
    fun observeAllSessionsGroupedByDate(): Flow<List<DaySession>>

    /** Удалить сессию по id. */
    suspend fun deleteSession(sessionId: Long)

    /** Удалить все сессии. */
    suspend fun deleteAllSessions()

    /** Переназначить связанные треки на новый профиль перед удалением старого. */
    suspend fun reassignSessionsProfile(oldProfileId: Long, newProfileId: Long)

    /** Все сессии за период (для экспорта телеметрии). */
    suspend fun getAllSessionsSince(sinceTimestamp: Long): List<DaySession>
}
