package com.figago.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория настроек приложения FigaGo.
 *
 * Хранит параметры прогнозирования пробега (нелинейная модель батареи),
 * настройки TTS-оповещений, GPS-трекинга и голосовых команд.
 * Реализуется через Jetpack DataStore Preferences.
 */
interface SettingsRepository {

    // ===== Общие настройки =====

    /** Активный профиль коляски. */
    fun observeActiveProfileId(): Flow<Long>
    suspend fun getActiveProfileId(): Long
    suspend fun setActiveProfileId(id: Long)
    // ===== Глобальные настройки приложения =====
    fun observeSkipDiagnostics(): Flow<Boolean>
    suspend fun getSkipDiagnostics(): Boolean
    suspend fun setSkipDiagnostics(value: Boolean)

    /** Система измерения (km / mi). */
    fun observeUnitSystem(): Flow<String>
    suspend fun getUnitSystem(): String
    suspend fun setUnitSystem(unit: String)

    // ===== Батарея и прогноз =====

    /** Использовать реальную статистику (true) или дефолтные значения (false). */
    fun observeUseStatistics(): Flow<Boolean>
    suspend fun getUseStatistics(): Boolean
    suspend fun setUseStatistics(value: Boolean)

    /** Дефолтный пробег (км) на каждом уровне заряда (5→4, 4→3 и т.д.). */
    suspend fun getDefaultRangeForLevel(level: Int): Double
    suspend fun setDefaultRangeForLevel(level: Int, km: Double)

    /** Все дефолтные пробеги по уровням (5 downTo 1 → km). */
    suspend fun getAllDefaultRanges(): Map<Int, Double>
    fun observeAllDefaultRanges(): Flow<Map<Int, Double>>

    // ===== Голосовые команды =====

    /** Вибрация при голосовой команде. */
    suspend fun getVibrateOnCommand(): Boolean
    suspend fun setVibrateOnCommand(value: Boolean)

    /** Звуковой сигнал при голосовой команде. */
    suspend fun getSoundOnCommand(): Boolean
    suspend fun setSoundOnCommand(value: Boolean)

    // ===== TTS-оповещения =====

    /** Режим оповещений: "off" / "distance" / "time". */
    fun observeTtsAnnounceMode(): Flow<String>
    suspend fun getTtsAnnounceMode(): String
    suspend fun setTtsAnnounceMode(mode: String)

    /** Интервал оповещений по расстоянию (км). */
    suspend fun getTtsDistanceIntervalKm(): Double
    suspend fun setTtsDistanceIntervalKm(km: Double)

    /** Интервал оповещений по времени (мин). */
    suspend fun getTtsTimeIntervalMin(): Int
    suspend fun setTtsTimeIntervalMin(minutes: Int)

    // ===== GPS-трекинг =====

    /** Интервал GPS-записи (секунды, 5..60). */
    suspend fun getGpsIntervalSec(): Int
    suspend fun setGpsIntervalSec(sec: Int)

    /** Время автозакрытия сессии (формат "HH:mm"). */
    suspend fun getAutoCloseTime(): String
    suspend fun setAutoCloseTime(time: String)

    /** Лимит скорости для авто-паузы (км/ч). 0 = выключено. */
    suspend fun getAutoPauseSpeedLimitKmH(): Int
    suspend fun setAutoPauseSpeedLimitKmH(limit: Int)

    /** Точка невозврата: предупреждать за X км (0 = выключено) */
    fun observePointOfNoReturnWarningKm(): Flow<Int>
    suspend fun getPointOfNoReturnWarningKm(): Int
    suspend fun setPointOfNoReturnWarningKm(km: Int)

    /** Язык интерфейса (per-profile). Пустая строка = системный. */
    suspend fun getAppLanguageCode(): String
    suspend fun setAppLanguageCode(code: String)

    /** Автоопределение транспорта. */
    fun observeAutoTransportDetection(): Flow<Boolean>
    suspend fun getAutoTransportDetection(): Boolean
    suspend fun setAutoTransportDetection(value: Boolean)

    /**
     * Атомарный снимок всех per-profile настроек.
     * Читает из ОДНОГО snapshot DataStore, гарантируя когерентность.
     */
    suspend fun getSettingsForProfile(profileId: Long): SettingsSnapshot

    companion object {
        /** Значения по умолчанию для нелинейного разряда батареи. */
        val DEFAULT_RANGES = mapOf(
            5 to 15.0,   // Полный → 4: 15 км
            4 to 8.0,    // 4 → 3: 8 км
            3 to 6.0,    // 3 → 2: 6 км
            2 to 4.0,    // 2 → 1: 4 км
            1 to 2.0,    // 1 → 0: 2 км (критический)
        )

        const val TTS_MODE_OFF = "off"
        const val TTS_MODE_DISTANCE = "distance"
        const val TTS_MODE_TIME = "time"
        
        const val UNIT_KILOMETERS = "km"
        const val UNIT_MILES = "mi"
    }
}

/**
 * Полный снимок per-profile настроек.
 * Используется для атомарной загрузки при смене профиля.
 */
data class SettingsSnapshot(
    val unitSystem: String = SettingsRepository.UNIT_KILOMETERS,
    val useStatistics: Boolean = true,
    val defaultRanges: Map<Int, Double> = SettingsRepository.DEFAULT_RANGES,
    val vibrateOnCommand: Boolean = true,
    val soundOnCommand: Boolean = false,
    val ttsMode: String = SettingsRepository.TTS_MODE_OFF,
    val ttsDistanceIntervalKm: Double = 1.0,
    val ttsTimeIntervalMin: Int = 15,
    val gpsIntervalSec: Int = 10,
    val autoCloseTime: String = "23:59",
    val autoPauseSpeedLimitKmH: Int = 15,
    val pointOfNoReturnWarningKm: Int = 2,
    val appLanguageCode: String = "",
    val autoTransportDetectionEnabled: Boolean = true,
)
