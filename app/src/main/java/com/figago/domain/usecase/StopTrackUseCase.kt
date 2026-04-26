package com.figago.domain.usecase

import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Остановка записи текущего отрезка пути.
 *
 * 1. Находит активный отрезок текущей сессии.
 * 2. Устанавливает end_time и финальную segment_distance.
 *
 * Если нет активного отрезка — ничего не делает.
 */
class StopTrackUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
) {

    suspend operator fun invoke() {
        val session = sessionRepository.getActiveSession() ?: return
        val segment = trackRepository.getActiveSegment(session.id) ?: return

        trackRepository.finishSegment(
            segmentId = segment.id,
            endTime = System.currentTimeMillis(),
            distance = segment.segmentDistance,
        )

        // Уничтожение мусорных треков (< 500м)
        if (segment.segmentDistance < 500.0) {
            trackRepository.deleteSegment(segment.id)
            val segmentsLeft = trackRepository.getSegmentsByDayId(session.id)
            if (segmentsLeft.isEmpty()) {
                sessionRepository.deleteSession(session.id)
            }
        }
    }
}
