package com.figago.domain.usecase

import com.figago.domain.model.BatteryWeightProfiles
import com.figago.domain.repository.LampStatisticsRepository
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.math.max

/**
 * Расчёт прогнозного остаточного пробега (км).
 *
 * Учитывает нелинейный разряд батареи коляски через весовые профили
 * (BatteryWeightProfiles). Каждый уровень заряда имеет свой вес,
 * отражающий долю ёмкости батареи.
 *
 * Алгоритм:
 * 1. Горячий старт: если для лампочки есть статистика в LampStatistics,
 *    используется среднее арифметическое реальных пробегов.
 * 2. Холодный старт: если статистики нет, используется
 *    maxMileage * weight[i] из весового профиля.
 * 3. Прогноз уменьшается в реальном времени по мере езды внутри текущего уровня.
 */
class ForecastRemainingDistanceUseCase @Inject constructor(
    private val lampStatisticsRepository: LampStatisticsRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
) {

    companion object {
        /** Минимальное количество завершённых сессий для использования статистики. */
        const val MIN_SESSIONS_FOR_STATS = 3
    }

    /**
     * Возвращает прогнозный остаточный пробег (км) или null если данных нет.
     *
     * @param currentLedCount текущее количество горящих индикаторов (0..N)
     * @param currentTotalDistance текущий суммарный пробег за день (метры)
     * @param distanceAtLastLedEvent пробег на момент последнего переключения индикатора (метры)
     */
    suspend operator fun invoke(
        currentLedCount: Int,
        currentTotalDistance: Double,
        distanceAtLastLedEvent: Double,
    ): Double? {

        if (currentLedCount <= 0) return 0.0

        val profile = profileRepository.getActiveProfile().first() ?: return null
        val ledCount = profile.ledCount ?: return null
        val maxMileage = profile.maxMileage ?: return null

        if (ledCount <= 0) return null

        // Получить средний пробег на каждом уровне (км)
        val avgDistancePerLevel = getDistancePerLevel(profile.id, ledCount, maxMileage)

        // Средний пробег на текущем уровне (метры)
        val avgForCurrentLevel = avgDistancePerLevel[currentLedCount]?.times(1000.0) ?: return null

        // Пробег с момента последнего переключения индикатора
        val distanceSinceLastLedChange = currentTotalDistance - distanceAtLastLedEvent

        // Оставшийся пробег на текущем уровне (не может быть отрицательным)
        val remainingOnCurrentLevel = max(0.0, avgForCurrentLevel - distanceSinceLastLedChange)

        // Сумма средних для нижних уровней (текущий уровень - 1 downTo 1)
        val sumLowerLevels = (currentLedCount - 1 downTo 1).sumOf { level ->
            (avgDistancePerLevel[level]?.times(1000.0)) ?: 0.0
        }

        // Итоговый прогноз в километрах
        val forecastMeters = remainingOnCurrentLevel + sumLowerLevels
        return forecastMeters / 1000.0
    }

    /**
     * Возвращает средний пробег (км) на каждом уровне.
     *
     * Для каждой лампочки: если есть статистика → среднее из LampStatistics,
     * иначе → maxMileage * weight[i] из весового профиля.
     *
     * Ключ Map — номер уровня (1 = последний/красный, N = первый/полный).
     */
    private suspend fun getDistancePerLevel(
        profileId: Long,
        ledCount: Int,
        maxMileage: Float
    ): Map<Int, Double> {
        val weights = BatteryWeightProfiles.getWeights(ledCount)
        val statistics = lampStatisticsRepository.getAveragesForProfile(profileId)
        val result = mutableMapOf<Int, Double>()

        for (i in 1..ledCount) {
            // lampIndex в БД = ledCount - i (0 = первая тухнущая = уровень ledCount)
            val lampIndex = ledCount - i
            val statAvg = statistics[lampIndex]

            if (statAvg != null) {
                // Горячий старт: используем статистику
                result[i] = statAvg.toDouble()
            } else {
                // Холодный старт: используем вес из профиля
                val weight = weights.getOrElse(lampIndex) { 1f / ledCount }
                result[i] = (maxMileage * weight).toDouble()
            }
        }

        return result
    }
}
