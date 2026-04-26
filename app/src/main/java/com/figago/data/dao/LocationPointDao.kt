package com.figago.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.figago.data.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с GPS-точками маршрута.
 *
 * Точки записываются с интервалом ~10 сек при активной записи трека.
 * Каждая точка привязана к конкретному TrackSegment.
 */
@Dao
interface LocationPointDao {

    /** Вставить новую GPS-точку. */
    @Insert
    suspend fun insert(point: LocationPointEntity): Long

    /** Пакетная вставка точек (для восстановления / импорта). */
    @Insert
    suspend fun insertAll(points: List<LocationPointEntity>)

    /** Все точки конкретного отрезка, упорядоченные по времени. */
    @Query("SELECT * FROM location_point WHERE segment_id = :segmentId ORDER BY timestamp ASC")
    suspend fun getPointsBySegmentId(segmentId: Long): List<LocationPointEntity>

    /** Реактивная подписка на точки отрезка (для отрисовки полилинии на карте). */
    @Query("SELECT * FROM location_point WHERE segment_id = :segmentId ORDER BY timestamp ASC")
    fun observePointsBySegmentId(segmentId: Long): Flow<List<LocationPointEntity>>

    /** Последняя записанная точка отрезка (для расчёта дистанции до новой точки). */
    @Query("SELECT * FROM location_point WHERE segment_id = :segmentId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPoint(segmentId: Long): LocationPointEntity?

    /** Количество точек в отрезке. */
    @Query("SELECT COUNT(*) FROM location_point WHERE segment_id = :segmentId")
    suspend fun getPointCount(segmentId: Long): Int

    /** Все точки за период (для экспорта телеметрии). */
    @Query("SELECT * FROM location_point WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getAllPointsSince(sinceTimestamp: Long): List<LocationPointEntity>
}
