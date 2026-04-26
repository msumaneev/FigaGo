package com.figago.domain.usecase

import com.figago.domain.model.DaySession
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * Получение списка всех сессий для экрана истории.
 * Возвращает реактивный Flow, автоматически обновляющий UI при изменениях в БД,
 * отфильтрованный по текущему активному профилю.
 */
class GetHistoryUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<DaySession>> {
        return settingsRepository.observeActiveProfileId().flatMapLatest { profileId ->
            sessionRepository.observeAllSessions(profileId)
        }
    }
}
