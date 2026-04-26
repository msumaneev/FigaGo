package com.figago.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.figago.data.entity.DaySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с суточными сессиями трекинга.
 *
 * Предоставляет CRUD-операции и специализированные запросы
 * для управления жизненным циклом дня.
 */
@Dao
interface DaySessionDao {

    /** Создать новую суточную сессию. Возвращает id созданной записи. */
    @Insert
    suspend fun insert(session: DaySessionEntity): Long

    /** Обновить существующую сессию (дистанция, статус). */
    @Update
    suspend fun update(session: DaySessionEntity)

    /** Получить активную сессию (is_active = true). Может быть только одна. */
    @Query("SELECT * FROM day_session WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSession(): DaySessionEntity?

    /** Реактивная подписка на активную сессию для UI. */
    @Query("SELECT * FROM day_session WHERE is_active = 1 LIMIT 1")
    fun observeActiveSession(): Flow<DaySessionEntity?>

    /** Найти первую сессию по дате (формат yyyy-MM-dd). */
    @Query("SELECT * FROM day_session WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): DaySessionEntity?

    /** Найти все сессии по дате (формат yyyy-MM-dd) для любого профиля. */
    @Query("SELECT * FROM day_session WHERE date = :date")
    suspend fun getAllSessionsByDate(date: String): List<DaySessionEntity>

    /** Закрыть сессию — установить is_active = false. */
    @Query("UPDATE day_session SET is_active = 0 WHERE id = :sessionId")
    suspend fun closeSession(sessionId: Long)

    /** Обновить суммарную дистанцию сессии. */
    @Query("UPDATE day_session SET total_distance = :distance WHERE id = :sessionId")
    suspend fun updateDistance(sessionId: Long, distance: Double)

    /** Все сессии, отфильтрованные по профилю и отсортированные по дате (новые первыми). */
    @Query("SELECT * FROM day_session WHERE profile_id = :profileId ORDER BY date DESC")
    fun observeAllSessions(profileId: Long): Flow<List<DaySessionEntity>>

    /** Все сессии без разделения на профили, сгруппированные по дате. Идентификатор берется первый попавшийся для навигации. */
    @Query("SELECT MIN(id) as id, date, SUM(total_distance) as total_distance, MAX(is_active) as is_active, MIN(profile_id) as profile_id FROM day_session GROUP BY date ORDER BY date DESC")
    fun observeAllSessionsGroupedByDate(): Flow<List<DaySessionEntity>>

    /** Получить сессию по id. */
    @Query("SELECT * FROM day_session WHERE id = :sessionId")
    suspend fun getById(sessionId: Long): DaySessionEntity?

    /** Реактивная подписка на конкретную сессию. */
    @Query("SELECT * FROM day_session WHERE id = :sessionId")
    fun observeById(sessionId: Long): Flow<DaySessionEntity?>

    /** Удалить сессию по id. Каскадное удаление треков настроено в Room (onDelete = CASCADE). */
    @Query("DELETE FROM day_session WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    /** Удалить все сессии. */
    @Query("DELETE FROM day_session")
    suspend fun deleteAllSessions()

    /** Переназначить все сессии с одного профиля на другой (массовое обновление). */
    @Query("UPDATE day_session SET profile_id = :newProfileId WHERE profile_id = :oldProfileId")
    suspend fun reassignSessionsProfile(oldProfileId: Long, newProfileId: Long)
}
