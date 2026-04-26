package com.figago.domain.usecase

import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Начало записи нового отрезка пути (TrackSegment).
 *
 * Создаёт новый TrackSegment для активной сессии.
 * Возвращает id созданного отрезка.
 *
 * Предусловия:
 * - Должна существовать активная DaySession.
 * - Не должно быть другого незавершённого отрезка.
 */
class StartTrackUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
) {

    suspend operator fun invoke(): Long {
        val session = sessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии. Сначала нажмите «Начало дня».")

        val existingSegment = trackRepository.getActiveSegment(session.id)

        if (existingSegment != null) {
            throw IllegalStateException("Запись уже идёт (segment id=${existingSegment.id})")
        }

        return trackRepository.createSegment(
            dayId = session.id,
            startTime = System.currentTimeMillis(),
            profileId = session.profileId ?: 1L
        )
    }
}
