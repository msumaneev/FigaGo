package com.figago.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.SettingsRepository.Companion.DEFAULT_RANGES
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_OFF
import com.figago.domain.repository.SettingsRepository.Companion.UNIT_KILOMETERS
import com.figago.domain.repository.SettingsSnapshot
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация SettingsRepository через Jetpack DataStore Preferences.
 *
 * Все per-profile настройки хранятся с префиксом «p{profileId}_»,
 * чтобы каждый профиль коляски имел свои собственные настройки.
 * Глобальные ключи (ACTIVE_PROFILE_ID) — без префикса.
 */
@Singleton
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    // ===== Глобальные ключи (без префикса профиля) =====
    private val KEY_ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
    private val KEY_SKIP_DIAGNOSTICS = booleanPreferencesKey("skip_diagnostics_check")

    override fun observeSkipDiagnostics(): Flow<Boolean> {
        return dataStore.data.map { prefs -> prefs[KEY_SKIP_DIAGNOSTICS] ?: false }
    }

    override suspend fun getSkipDiagnostics(): Boolean {
        return dataStore.data.first()[KEY_SKIP_DIAGNOSTICS] ?: false
    }

    override suspend fun setSkipDiagnostics(value: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_SKIP_DIAGNOSTICS] = value }
    }

    // ===== Функции для per-profile ключей =====

    /** Создаёт ключ с привязкой к профилю: p{id}_keyName */
    private fun profileString(profileId: Long, name: String) = stringPreferencesKey("p${profileId}_$name")
    private fun profileBool(profileId: Long, name: String) = booleanPreferencesKey("p${profileId}_$name")
    private fun profileInt(profileId: Long, name: String) = intPreferencesKey("p${profileId}_$name")
    private fun profileDouble(profileId: Long, name: String) = doublePreferencesKey("p${profileId}_$name")

    /** Получить текущий активный профиль для чтения ключей. */
    private suspend fun pid(): Long = dataStore.data.first()[KEY_ACTIVE_PROFILE_ID] ?: 1L

    /** Flow активного профиля (для flatMapLatest). */
    private fun pidFlow(): Flow<Long> = dataStore.data.map { prefs -> prefs[KEY_ACTIVE_PROFILE_ID] ?: 1L }

    // ===== Общие настройки (глобальные) =====

    override fun observeActiveProfileId(): Flow<Long> {
        return dataStore.data.map { prefs -> prefs[KEY_ACTIVE_PROFILE_ID] ?: 1L }
    }

    override suspend fun getActiveProfileId(): Long {
        return dataStore.data.first()[KEY_ACTIVE_PROFILE_ID] ?: 1L
    }

    override suspend fun setActiveProfileId(id: Long) {
        dataStore.edit { prefs -> prefs[KEY_ACTIVE_PROFILE_ID] = id }
    }

    // ===== Единицы измерения (per-profile) =====

    override fun observeUnitSystem(): Flow<String> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs -> prefs[profileString(id, "unit_system")] ?: UNIT_KILOMETERS }
        }
    }

    override suspend fun getUnitSystem(): String {
        val id = pid()
        return dataStore.data.first()[profileString(id, "unit_system")] ?: UNIT_KILOMETERS
    }

    override suspend fun setUnitSystem(unit: String) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileString(id, "unit_system")] = unit }
    }

    // ===== Батарея и прогноз (per-profile) =====

    override fun observeUseStatistics(): Flow<Boolean> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs -> prefs[profileBool(id, "use_statistics")] ?: true }
        }
    }

    override suspend fun getUseStatistics(): Boolean {
        val id = pid()
        return dataStore.data.first()[profileBool(id, "use_statistics")] ?: true
    }

    override suspend fun setUseStatistics(value: Boolean) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileBool(id, "use_statistics")] = value }
    }

    override suspend fun getDefaultRangeForLevel(level: Int): Double {
        val id = pid()
        return dataStore.data.first()[profileDouble(id, "default_range_level_$level")]
            ?: DEFAULT_RANGES.getOrDefault(level, 5.0)
    }

    override suspend fun setDefaultRangeForLevel(level: Int, km: Double) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileDouble(id, "default_range_level_$level")] = km }
    }

    override suspend fun getAllDefaultRanges(): Map<Int, Double> {
        val id = pid()
        val prefs = dataStore.data.first()
        return (1..5).associateWith { level ->
            prefs[profileDouble(id, "default_range_level_$level")]
                ?: DEFAULT_RANGES.getOrDefault(level, 5.0)
        }
    }

    override fun observeAllDefaultRanges(): Flow<Map<Int, Double>> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs ->
                (1..5).associateWith { level ->
                    prefs[profileDouble(id, "default_range_level_$level")]
                        ?: DEFAULT_RANGES.getOrDefault(level, 5.0)
                }
            }
        }
    }

    // ===== Голосовые команды (per-profile) =====

    override suspend fun getVibrateOnCommand(): Boolean {
        val id = pid()
        return dataStore.data.first()[profileBool(id, "vibrate_on_command")] ?: true
    }

    override suspend fun setVibrateOnCommand(value: Boolean) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileBool(id, "vibrate_on_command")] = value }
    }

    override suspend fun getSoundOnCommand(): Boolean {
        val id = pid()
        return dataStore.data.first()[profileBool(id, "sound_on_command")] ?: false
    }

    override suspend fun setSoundOnCommand(value: Boolean) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileBool(id, "sound_on_command")] = value }
    }

    // ===== TTS-оповещения (per-profile) =====

    override fun observeTtsAnnounceMode(): Flow<String> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs -> prefs[profileString(id, "tts_announce_mode")] ?: TTS_MODE_OFF }
        }
    }

    override suspend fun getTtsAnnounceMode(): String {
        val id = pid()
        return dataStore.data.first()[profileString(id, "tts_announce_mode")] ?: TTS_MODE_OFF
    }

    override suspend fun setTtsAnnounceMode(mode: String) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileString(id, "tts_announce_mode")] = mode }
    }

    override suspend fun getTtsDistanceIntervalKm(): Double {
        val id = pid()
        return dataStore.data.first()[profileDouble(id, "tts_distance_interval_km")] ?: 1.0
    }

    override suspend fun setTtsDistanceIntervalKm(km: Double) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileDouble(id, "tts_distance_interval_km")] = km }
    }

    override suspend fun getTtsTimeIntervalMin(): Int {
        val id = pid()
        return dataStore.data.first()[profileInt(id, "tts_time_interval_min")] ?: 15
    }

    override suspend fun setTtsTimeIntervalMin(minutes: Int) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileInt(id, "tts_time_interval_min")] = minutes }
    }

    // ===== GPS-трекинг (per-profile) =====

    override suspend fun getGpsIntervalSec(): Int {
        val id = pid()
        return dataStore.data.first()[profileInt(id, "gps_interval_sec")] ?: 10
    }

    override suspend fun setGpsIntervalSec(sec: Int) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileInt(id, "gps_interval_sec")] = sec }
    }

    override suspend fun getAutoCloseTime(): String {
        val id = pid()
        return dataStore.data.first()[profileString(id, "auto_close_time")] ?: "23:59"
    }

    override suspend fun setAutoCloseTime(time: String) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileString(id, "auto_close_time")] = time }
    }

    override suspend fun getAutoPauseSpeedLimitKmH(): Int {
        val id = pid()
        return dataStore.data.first()[profileInt(id, "auto_pause_speed_limit_kmh")] ?: 15
    }

    override suspend fun setAutoPauseSpeedLimitKmH(limit: Int) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileInt(id, "auto_pause_speed_limit_kmh")] = limit }
    }

    override fun observePointOfNoReturnWarningKm(): Flow<Int> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs -> prefs[profileInt(id, "point_of_no_return_warning_km")] ?: 2 }
        }
    }

    override suspend fun getPointOfNoReturnWarningKm(): Int {
        val id = pid()
        return dataStore.data.first()[profileInt(id, "point_of_no_return_warning_km")] ?: 2
    }

    override suspend fun setPointOfNoReturnWarningKm(km: Int) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileInt(id, "point_of_no_return_warning_km")] = km }
    }

    // ===== Язык интерфейса (per-profile) =====

    override suspend fun getAppLanguageCode(): String {
        val id = pid()
        return dataStore.data.first()[profileString(id, "app_language_code")] ?: ""
    }

    override suspend fun setAppLanguageCode(code: String) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileString(id, "app_language_code")] = code }
    }

    // ===== Автоопределение транспорта (per-profile) =====

    override fun observeAutoTransportDetection(): Flow<Boolean> {
        return pidFlow().flatMapLatest { id ->
            dataStore.data.map { prefs -> prefs[profileBool(id, "auto_transport_detection")] ?: true }
        }
    }

    override suspend fun getAutoTransportDetection(): Boolean {
        val id = pid()
        return dataStore.data.first()[profileBool(id, "auto_transport_detection")] ?: true
    }

    override suspend fun setAutoTransportDetection(value: Boolean) {
        val id = pid()
        dataStore.edit { prefs -> prefs[profileBool(id, "auto_transport_detection")] = value }
    }

    // ===== Атомарный снимок всех настроек профиля =====

    /**
     * Читает ВСЕ per-profile настройки из ОДНОГО snapshot DataStore.
     * Принимает profileId напрямую — не зависит от pid().
     */
    override suspend fun getSettingsForProfile(profileId: Long): SettingsSnapshot {
        val prefs = dataStore.data.first()
        val id = profileId

        val ranges = (1..5).associateWith { level ->
            prefs[profileDouble(id, "default_range_level_$level")]
                ?: DEFAULT_RANGES.getOrDefault(level, 5.0)
        }

        return SettingsSnapshot(
            unitSystem = prefs[profileString(id, "unit_system")] ?: UNIT_KILOMETERS,
            useStatistics = prefs[profileBool(id, "use_statistics")] ?: true,
            defaultRanges = ranges,
            vibrateOnCommand = prefs[profileBool(id, "vibrate_on_command")] ?: true,
            soundOnCommand = prefs[profileBool(id, "sound_on_command")] ?: false,
            ttsMode = prefs[profileString(id, "tts_announce_mode")] ?: TTS_MODE_OFF,
            ttsDistanceIntervalKm = prefs[profileDouble(id, "tts_distance_interval_km")] ?: 1.0,
            ttsTimeIntervalMin = prefs[profileInt(id, "tts_time_interval_min")] ?: 15,
            gpsIntervalSec = prefs[profileInt(id, "gps_interval_sec")] ?: 10,
            autoCloseTime = prefs[profileString(id, "auto_close_time")] ?: "23:59",
            autoPauseSpeedLimitKmH = prefs[profileInt(id, "auto_pause_speed_limit_kmh")] ?: 15,
            pointOfNoReturnWarningKm = prefs[profileInt(id, "point_of_no_return_warning_km")] ?: 2,
            appLanguageCode = prefs[profileString(id, "app_language_code")] ?: "",
            autoTransportDetectionEnabled = prefs[profileBool(id, "auto_transport_detection")] ?: true,
        )
    }
}
