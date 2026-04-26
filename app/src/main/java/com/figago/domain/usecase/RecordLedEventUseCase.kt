package com.figago.domain.usecase

import com.figago.domain.model.BatteryWeightProfiles
import com.figago.domain.repository.LampStatisticsRepository
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Фиксация LED-события (погасание индикатора батареи).
 *
 * 1. Читает активную DaySession и текущую total_distance.
 * 2. Определяет ledCountRemaining на основе последнего события.
 * 3. Записывает LedEvent в Room.
 * 4. Вычисляет дельту пробега и сохраняет в LampStatistics.
 * 5. Обрабатывает пропуски (распределение по весам).
 *
 * Возвращает оставшееся количество индикаторов после фиксации.
 *
 * @throws IllegalStateException если нет активной сессии или счётчик уже = 0.
 */
class RecordLedEventUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val ledEventRepository: LedEventRepository,
    private val lampStatisticsRepository: LampStatisticsRepository,
    private val profileRepository: ProfileRepository,
) {

    companion object {
        /** Максимальное количество индикаторов (полная батарея). Используется как fallback. */
        const val MAX_LED_COUNT = 5
    }

    suspend operator fun invoke(): Int {
        val session = sessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии для записи LED-события.")

        val profile = profileRepository.getActiveProfile().first()
        val maxLed = profile?.ledCount ?: MAX_LED_COUNT

        // Определяем текущее значение счётчика
        val lastEvent = ledEventRepository.getLastEvent(session.id)
        val currentLedCount = lastEvent?.ledCountRemaining ?: maxLed

        if (currentLedCount <= 0) {
            throw IllegalStateException("Все индикаторы уже погасли (счётчик = 0).")
        }

        val newLedCount = currentLedCount - 1

        ledEventRepository.recordEvent(
            dayId = session.id,
            ledCountRemaining = newLedCount,
            distanceAtEvent = session.totalDistance,
            timestamp = System.currentTimeMillis(),
        )

        // Записываем факт пробега в LampStatistics
        if (profile != null) {
            recordLampStatistic(
                profileId = profile.id,
                ledCount = maxLed,
                previousLedCount = currentLedCount,
                newLedCount = newLedCount,
                currentDistance = session.totalDistance,
                lastEventDistance = lastEvent?.distanceAtEvent ?: 0.0,
            )
        }

        return newLedCount
    }

    /**
     * Каскадное переключение индикаторов — устанавливает целевое значение ledCount.
     *
     * Записывает одно событие с итоговым значением ledCountRemaining = targetLedCount.
     * Используется при нажатии на конкретный индикатор (каскадная логика).
     * Обрабатывает пропуски: если перескочили несколько лампочек, дельта распределяется
     * по весам.
     *
     * @param targetLedCount целевое количество горящих индикаторов (0..maxLed)
     * @return новое значение ledCount
     * @throws IllegalStateException если нет активной сессии
     */
    suspend operator fun invoke(targetLedCount: Int): Int {

        val session = sessionRepository.getActiveSession()
            ?: throw IllegalStateException("Нет активной сессии для записи LED-события.")

        val profile = profileRepository.getActiveProfile().first()
        val maxLed = profile?.ledCount ?: MAX_LED_COUNT

        require(targetLedCount in 0..maxLed) {
            "Значение ledCount ($targetLedCount) вне допустимого диапазона 0..$maxLed"
        }

        val lastEvent = ledEventRepository.getLastEvent(session.id)
        val currentLedCount = lastEvent?.ledCountRemaining ?: maxLed

        ledEventRepository.recordEvent(
            dayId = session.id,
            ledCountRemaining = targetLedCount,
            distanceAtEvent = session.totalDistance,
            timestamp = System.currentTimeMillis(),
        )

        // Записываем факт(ы) пробега в LampStatistics
        if (profile != null && targetLedCount < currentLedCount) {
            recordLampStatistic(
                profileId = profile.id,
                ledCount = maxLed,
                previousLedCount = currentLedCount,
                newLedCount = targetLedCount,
                currentDistance = session.totalDistance,
                lastEventDistance = lastEvent?.distanceAtEvent ?: 0.0,
            )
        }

        return targetLedCount
    }

    /**
     * Записывает статистику пробега в LampStatistics.
     *
     * Если перескочено несколько лампочек (пропуск), общая дельта распределяется
     * пропорционально базовым весам пропущенных лампочек.
     */
    private suspend fun recordLampStatistic(
        profileId: Long,
        ledCount: Int,
        previousLedCount: Int,
        newLedCount: Int,
        currentDistance: Double,
        lastEventDistance: Double,
    ) {
        val totalDeltaKm = (currentDistance - lastEventDistance) / 1000.0
        if (totalDeltaKm <= 0) return

        val skippedCount = previousLedCount - newLedCount
        if (skippedCount <= 0) return

        val weights = BatteryWeightProfiles.getWeights(ledCount)

        if (skippedCount == 1) {
            // Обычный случай: потухла одна лампочка
            val lampIndex = ledCount - previousLedCount // 0-based индекс лампочки
            lampStatisticsRepository.recordDistance(profileId, lampIndex, totalDeltaKm.toFloat())
        } else {
            // Пропуск: распределяем пропорционально весам
            val skippedIndices = (previousLedCount - 1 downTo newLedCount).map { level ->
                ledCount - level - 1
            }

            val totalWeight = skippedIndices.sumOf { idx ->
                weights.getOrElse(idx) { 1f / ledCount }.toDouble()
            }

            for (idx in skippedIndices) {
                val weight = weights.getOrElse(idx) { 1f / ledCount }.toDouble()
                val proportion = if (totalWeight > 0) weight / totalWeight else 1.0 / skippedCount
                val distanceKm = totalDeltaKm * proportion
                lampStatisticsRepository.recordDistance(profileId, idx, distanceKm.toFloat())
            }
        }
    }
}
