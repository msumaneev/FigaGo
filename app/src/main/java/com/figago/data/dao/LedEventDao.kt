package com.figago.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.figago.data.entity.LedEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с LED-событиями (фиксация разряда батареи).
 *
 * Каждое событие регистрируется голосовой командой или кнопкой.
 * Хранит оставшееся число индикаторов и дистанцию на момент события.
 */
@Dao
interface LedEventDao {

    /** Зарегистрировать новое LED-событие. */
    @Insert
    suspend fun insert(event: LedEventEntity): Long

    /** Все LED-события конкретной сессии, упорядоченные по времени. */
    @Query("SELECT * FROM led_event WHERE day_id = :dayId ORDER BY timestamp ASC")
    suspend fun getEventsByDayId(dayId: Long): List<LedEventEntity>

    /** Реактивная подписка на LED-события сессии (для UI). */
    @Query("SELECT * FROM led_event WHERE day_id = :dayId ORDER BY timestamp ASC")
    fun observeEventsByDayId(dayId: Long): Flow<List<LedEventEntity>>

    /** Все LED-события за всё время — для расчёта среднего пробега между разрядами. */
    @Query("SELECT * FROM led_event ORDER BY timestamp ASC")
    suspend fun getAllEvents(): List<LedEventEntity>

    /** Количество LED-событий за конкретную сессию. */
    @Query("SELECT COUNT(*) FROM led_event WHERE day_id = :dayId")
    suspend fun getEventCountByDayId(dayId: Long): Int

    /** Последнее LED-событие сессии (для определения текущего значения счётчика). */
    @Query("SELECT * FROM led_event WHERE day_id = :dayId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEvent(dayId: Long): LedEventEntity?

    /**
     * Количество завершённых сессий, в которых есть LED-события.
     * Используется для определения достаточности статистики (≥ 3).
     */
    @Query(
        """
        SELECT COUNT(DISTINCT e.day_id) 
        FROM led_event e 
        JOIN day_session d ON e.day_id = d.id 
        WHERE d.is_active = 0
        """
    )
    suspend fun getCompletedSessionsWithEventsCount(): Int

    /**
     * Все LED-события из завершённых сессий, упорядоченные по сессии и времени.
     * Используется для расчёта среднего пробега на каждом уровне заряда.
     */
    @Query(
        """
        SELECT e.* 
        FROM led_event e 
        JOIN day_session d ON e.day_id = d.id 
        WHERE d.is_active = 0 
        ORDER BY e.day_id ASC, e.timestamp ASC
        """
    )
    suspend fun getEventsFromCompletedSessions(): List<LedEventEntity>
}
