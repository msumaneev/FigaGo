package com.figago.domain.usecase

import com.figago.domain.model.DaySession
import com.figago.domain.model.LedEvent
import com.figago.domain.model.TrackSegment
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Агрегированная статистика за конкретный день.
 */
data class DayStats(
    val session: DaySession,
    val segments: List<TrackSegment>,
    val ledEvents: List<LedEvent>,
    val totalDistance: Double,
    val segmentCount: Int,
    val totalMovingTimeMs: Long,
    val ledEventCount: Int,
    val walkDistance: Double = 0.0,
    val transportDistance: Double = 0.0,
    val walkTimeMs: Long = 0L,
    val transportTimeMs: Long = 0L
)

/**
 * Получение агрегированной статистики за конкретный день.
 *
 * Собирает данные из всех репозиториев и вычисляет:
 * - общую дистанцию
 * - количество отрезков
 * - время в движении (сумма длительностей всех завершённых отрезков)
 * - количество LED-событий
 */
class GetDayStatsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
    private val ledEventRepository: LedEventRepository,
) {

    suspend operator fun invoke(sessionId: Long): DayStats? {
        val session = sessionRepository.getById(sessionId) ?: return null
        val segments = trackRepository.getSegmentsByDayId(sessionId)
        val ledEvents = ledEventRepository.getEventsByDayId(sessionId)

        // Время в движении — сумма (end_time - start_time) для завершённых отрезков
        val totalMovingTimeMs = segments
            .filter { it.endTime != null }
            .sumOf { (it.endTime!! - it.startTime) }

        return DayStats(
            session = session,
            segments = segments,
            ledEvents = ledEvents,
            totalDistance = session.totalDistance,
            segmentCount = segments.size,
            totalMovingTimeMs = totalMovingTimeMs,
            ledEventCount = ledEvents.size,
        )
    }
}
