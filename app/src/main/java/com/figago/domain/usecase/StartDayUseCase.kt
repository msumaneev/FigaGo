package com.figago.domain.usecase

import com.figago.domain.repository.SessionRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Начало нового рабочего дня.
 *
 * Создаёт DaySession с текущей датой.
 * Возвращает id созданной сессии.
 *
 * Предусловие: не должно быть другой активной сессии.
 * Если активная сессия уже есть — выбрасывает IllegalStateException.
 */
class StartDayUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    suspend operator fun invoke(): Long {
        val existing = sessionRepository.getActiveSession()

        if (existing != null) {
            throw IllegalStateException("Активная сессия уже существует (id=${existing.id}, date=${existing.date})")
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return sessionRepository.createSession(today)
    }
}
