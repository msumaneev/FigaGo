package com.figago.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.figago.data.entity.TrackSegmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с отрезками пути (TrackSegment).
 *
 * Каждый отрезок создаётся при нажатии «Старт» и завершается при «Стоп».
 */
@Dao
interface TrackSegmentDao {

    /** Создать новый отрезок. Возвращает id созданной записи. */
    @Insert
    suspend fun insert(segment: TrackSegmentEntity): Long

    /** Обновить отрезок (end_time, segment_distance). */
    @Update
    suspend fun update(segment: TrackSegmentEntity)

    /** Получить все отрезки конкретной сессии. */
    @Query("SELECT * FROM track_segment WHERE day_id = :dayId ORDER BY start_time ASC")
    suspend fun getSegmentsByDayId(dayId: Long): List<TrackSegmentEntity>

    /** Реактивная подписка на отрезки конкретной сессии (для карты истории). */
    @Query("SELECT * FROM track_segment WHERE day_id = :dayId ORDER BY start_time ASC")
    fun observeSegmentsByDayId(dayId: Long): Flow<List<TrackSegmentEntity>>

    /**
     * Найти активный (незавершённый) отрезок — у которого end_time IS NULL.
     * В нормальном режиме может быть только один.
     */
    @Query("SELECT * FROM track_segment WHERE day_id = :dayId AND end_time IS NULL LIMIT 1")
    suspend fun getActiveSegment(dayId: Long): TrackSegmentEntity?

    /** Завершить отрезок: установить end_time и финальную дистанцию. */
    @Query("UPDATE track_segment SET end_time = :endTime, segment_distance = :distance WHERE id = :segmentId")
    suspend fun finishSegment(segmentId: Long, endTime: Long, distance: Double)

    /** Обновить дистанцию отрезка (вызывается при каждой новой GPS-точке). */
    @Query("UPDATE track_segment SET segment_distance = :distance WHERE id = :segmentId")
    suspend fun updateDistance(segmentId: Long, distance: Double)

    /** Получить отрезок по id. */
    @Query("SELECT * FROM track_segment WHERE id = :segmentId")
    suspend fun getById(segmentId: Long): TrackSegmentEntity?

    /** Удалить отрезок по id. Каскадное удаление точек настроено в Room. */
    @Query("DELETE FROM track_segment WHERE id = :segmentId")
    suspend fun deleteSegment(segmentId: Long)

    /** Все отрезки (для экспорта). */
    @Query("SELECT * FROM track_segment ORDER BY start_time DESC")
    suspend fun getAllSegments(): List<TrackSegmentEntity>
}
