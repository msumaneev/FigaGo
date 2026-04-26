package com.figago.domain.repository

import com.figago.domain.model.LocationPoint
import com.figago.domain.model.TrackSegment
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для управления отрезками пути и GPS-точками.
 */
interface TrackRepository {

    /** Создать новый отрезок для сессии. Возвращает id. */
    suspend fun createSegment(dayId: Long, startTime: Long, profileId: Long): Long

    /** Завершить отрезок: установить end_time и финальную дистанцию. */
    suspend fun finishSegment(segmentId: Long, endTime: Long, distance: Double)

    /** Обновить дистанцию активного отрезка. */
    suspend fun updateSegmentDistance(segmentId: Long, distance: Double)

    /** Получить активный (незавершённый) отрезок сессии. */
    suspend fun getActiveSegment(dayId: Long): TrackSegment?

    /** Получить отрезок по id. */
    suspend fun getSegmentById(segmentId: Long): TrackSegment?

    /** Все отрезки сессии. */
    suspend fun getSegmentsByDayId(dayId: Long): List<TrackSegment>

    /** Реактивная подписка на отрезки сессии. */
    fun observeSegmentsByDayId(dayId: Long): Flow<List<TrackSegment>>

    /** Записать GPS-точку. Возвращает id. */
    suspend fun addLocationPoint(segmentId: Long, latitude: Double, longitude: Double, timestamp: Long, isTransport: Boolean = false): Long

    /** Записать пакет (batch) GPS-точек. Обёрнуто в транзакцию на стороне Room или должно быть обёрнуто. */
    suspend fun addLocationPoints(points: List<LocationPoint>)

    /** Получить последнюю точку отрезка (для расчёта дистанции). */
    suspend fun getLastPoint(segmentId: Long): LocationPoint?

    /** Все точки отрезка. */
    suspend fun getPointsBySegmentId(segmentId: Long): List<LocationPoint>

    /** Реактивная подписка на точки отрезка (для полилинии на карте). */
    fun observePointsBySegmentId(segmentId: Long): Flow<List<LocationPoint>>

    /** Удалить отрезок по id. */
    suspend fun deleteSegment(segmentId: Long)

    /** Все отрезки за период (для экспорта телеметрии). */
    suspend fun getAllTracksSince(sinceTimestamp: Long): List<TrackSegment>

    /** Все GPS-точки за период (для экспорта телеметрии). */
    suspend fun getAllLocationsSince(sinceTimestamp: Long): List<LocationPoint>
}
