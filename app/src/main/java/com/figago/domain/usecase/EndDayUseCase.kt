package com.figago.domain.usecase

import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import javax.inject.Inject

/**
 * Завершение рабочего дня.
 *
 * 1. Если есть активный отрезок — завершает его (StopTrackUseCase).
 * 2. Закрывает сессию (is_active = false).
 *
 * Если активной сессии нет — ничего не делает.
 */
class EndDayUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
    private val stopTrackUseCase: StopTrackUseCase,
) {

    suspend operator fun invoke() {
        val session = sessionRepository.getActiveSession() ?: return

        // Если идёт запись трека — останавливаем
        val activeSegment = trackRepository.getActiveSegment(session.id)

        if (activeSegment != null) {
            stopTrackUseCase()
        }

        sessionRepository.closeSession(session.id)
    }
}
