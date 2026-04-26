package com.figago.domain.repository

import com.figago.domain.model.LedEvent
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для управления LED-событиями (разряд батареи).
 */
interface LedEventRepository {

    /** Зарегистрировать новое LED-событие. Возвращает id. */
    suspend fun recordEvent(dayId: Long, ledCountRemaining: Int, distanceAtEvent: Double, timestamp: Long): Long

    /** Все LED-события конкретной сессии. */
    suspend fun getEventsByDayId(dayId: Long): List<LedEvent>

    /** Реактивная подписка на LED-события сессии. */
    fun observeEventsByDayId(dayId: Long): Flow<List<LedEvent>>

    /** Все LED-события за всё время (для аналитики среднего пробега). */
    suspend fun getAllEvents(): List<LedEvent>

    /** Количество LED-событий за сессию. */
    suspend fun getEventCountByDayId(dayId: Long): Int

    /** Последнее LED-событие сессии (текущее значение счётчика). */
    suspend fun getLastEvent(dayId: Long): LedEvent?

    /** Количество завершённых сессий с LED-событиями (для определения достаточности статистики). */
    suspend fun getCompletedSessionsWithEventsCount(): Int

    /**
     * Средний пробег (метры) на каждом уровне заряда по завершённым сессиям.
     * Возвращает Map<уровень(5..1), средний_пробег_метры>.
     * Если данных нет — пустая Map.
     */
    suspend fun getAverageDistancePerLevel(): Map<Int, Double>

    /** Все LED-события за период (для экспорта телеметрии). */
    suspend fun getAllSince(sinceTimestamp: Long): List<LedEvent>
}
