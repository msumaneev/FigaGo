package com.figago.ui.settings

import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_DISTANCE
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_OFF
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_TIME

/**
 * Состояние экрана настроек.
 *
 * @property useStatistics использовать реальную статистику для прогноза
 * @property defaultRanges дефолтный пробег по уровням (5 downTo 1 → км)
 * @property vibrateOnCommand вибрация при голосовой команде
 * @property soundOnCommand звуковой сигнал при голосовой команде
 * @property ttsMode режим TTS-оповещений: "off" / "distance" / "time"
 * @property ttsDistanceIntervalKm интервал по километрам
 * @property ttsTimeIntervalMin интервал по минутам
 * @property gpsIntervalSec интервал GPS (секунды)
 * @property autoCloseTime время автозакрытия (формат "HH:mm")
 * @property autoPauseSpeedLimitKmH авто-пауза при скорости больше X км/ч (0 = откл)
 * @property appLanguageCode код языка: "ru", "en", "" (системный)
 */
data class SettingsUiState(
    val useStatistics: Boolean = true,
    val defaultRanges: Map<Int, Double> = mapOf(5 to 15.0, 4 to 8.0, 3 to 6.0, 2 to 4.0, 1 to 2.0),
    val vibrateOnCommand: Boolean = true,
    val soundOnCommand: Boolean = false,
    val ttsMode: String = TTS_MODE_OFF,
    val ttsDistanceIntervalKm: Double = 1.0,
    val ttsTimeIntervalMin: Int = 15,
    val gpsIntervalSec: Int = 10,
    val autoCloseTime: String = "23:59",
    val autoPauseSpeedLimitKmH: Int = 15,
    val pointOfNoReturnWarningKm: Int = 2,
    val appLanguageCode: String = "",
    val unitSystem: String = com.figago.domain.repository.SettingsRepository.UNIT_KILOMETERS,
    val autoTransportDetectionEnabled: Boolean = true,
) {

    /** Человекочитаемое название режима TTS. */
    val ttsModeLabel: String
        get() = when (ttsMode) {
            TTS_MODE_OFF -> ""
            TTS_MODE_DISTANCE -> ""
            TTS_MODE_TIME -> ""
            else -> ""
        }
}
