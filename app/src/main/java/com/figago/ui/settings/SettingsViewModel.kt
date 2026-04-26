package com.figago.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_DISTANCE
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_OFF
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_TIME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.figago.data.entity.ProfileEntity

/**
 * ViewModel экрана настроек.
 *
 * Читает/записывает все параметры через SettingsRepository (DataStore).
 * При открытии экрана загружает текущие значения, при изменении —
 * немедленно сохраняет в DataStore.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val profileRepository: ProfileRepository,
    private val lampStatisticsRepository: com.figago.domain.repository.LampStatisticsRepository,
    private val telemetryExportUseCase: com.figago.domain.usecase.TelemetryExportUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    var originalState: SettingsUiState = SettingsUiState()
        private set

    val activeProfile: StateFlow<ProfileEntity?> = settingsRepository.observeActiveProfileId()
        .flatMapLatest { id -> profileRepository.getProfileByIdFlow(id) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    val activeProfileSessionCount: StateFlow<Int> = settingsRepository.observeActiveProfileId()
        .flatMapLatest { id -> sessionRepository.observeAllSessions(id).map { it.size } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    /** Средние пробеги по лампочкам из LampStatistics. */
    private val _lampAverages = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val lampAverages: StateFlow<Map<Int, Float>> = _lampAverages.asStateFlow()

    init {
        // Начальная загрузка настроек текущего профиля
        viewModelScope.launch {
            val profileId = settingsRepository.getActiveProfileId()
            applySnapshot(settingsRepository.getSettingsForProfile(profileId))
        }

        // Перезагружаем настройки при смене активного профиля.
        // drop(1) — пропускаем первый emit (уже обработан выше).
        // distinctUntilChanged — реагируем только на реальную смену ID.
        viewModelScope.launch {
            settingsRepository.observeActiveProfileId()
                .distinctUntilChanged()
                .drop(1)
                .collect { newProfileId ->

                    val snapshot = settingsRepository.getSettingsForProfile(newProfileId)
                    applySnapshot(snapshot)

                    // Применяем язык нового профиля
                    val localeList = if (snapshot.appLanguageCode.isEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(snapshot.appLanguageCode)
                    }
                    AppCompatDelegate.setApplicationLocales(localeList)
                }
        }

        // Загрузка статистики лампочек
        viewModelScope.launch {
            settingsRepository.observeActiveProfileId()
                .distinctUntilChanged()
                .collect { profileId ->
                    val averages = lampStatisticsRepository.getAveragesForProfile(profileId)
                    _lampAverages.value = averages
                }
        }
    }

    /**
     * Применяет атомарный снимок настроек к UI-состоянию.
     */
    private fun applySnapshot(s: com.figago.domain.repository.SettingsSnapshot) {
        val loadedState = SettingsUiState(
            appLanguageCode = s.appLanguageCode,
            useStatistics = s.useStatistics,
            defaultRanges = s.defaultRanges,
            vibrateOnCommand = s.vibrateOnCommand,
            soundOnCommand = s.soundOnCommand,
            ttsMode = s.ttsMode,
            ttsDistanceIntervalKm = s.ttsDistanceIntervalKm,
            ttsTimeIntervalMin = s.ttsTimeIntervalMin,
            gpsIntervalSec = s.gpsIntervalSec,
            autoCloseTime = s.autoCloseTime,
            autoPauseSpeedLimitKmH = s.autoPauseSpeedLimitKmH,
            pointOfNoReturnWarningKm = s.pointOfNoReturnWarningKm,
            unitSystem = s.unitSystem,
            autoTransportDetectionEnabled = s.autoTransportDetectionEnabled,
        )
        originalState = loadedState
        _uiState.value = loadedState
    }

    // ===== Батарея и прогноз =====

    fun setUseStatistics(value: Boolean) {
        _uiState.value = _uiState.value.copy(useStatistics = value)
        viewModelScope.launch { settingsRepository.setUseStatistics(value) }
    }

    fun setDefaultRange(level: Int, km: Double) {
        val updated = _uiState.value.defaultRanges.toMutableMap()
        updated[level] = km
        _uiState.value = _uiState.value.copy(defaultRanges = updated)
        viewModelScope.launch { settingsRepository.setDefaultRangeForLevel(level, km) }
    }

    // ===== Голосовые команды =====

    fun setVibrateOnCommand(value: Boolean) {
        _uiState.value = _uiState.value.copy(vibrateOnCommand = value)
        viewModelScope.launch { settingsRepository.setVibrateOnCommand(value) }
    }

    fun setSoundOnCommand(value: Boolean) {
        _uiState.value = _uiState.value.copy(soundOnCommand = value)
        viewModelScope.launch { settingsRepository.setSoundOnCommand(value) }
    }

    // ===== TTS-оповещения =====

    fun setTtsMode(mode: String) {
        _uiState.value = _uiState.value.copy(ttsMode = mode)
        viewModelScope.launch { settingsRepository.setTtsAnnounceMode(mode) }
    }

    fun setTtsDistanceInterval(km: Double) {
        _uiState.value = _uiState.value.copy(ttsDistanceIntervalKm = km)
        viewModelScope.launch { settingsRepository.setTtsDistanceIntervalKm(km) }
    }

    fun setTtsTimeInterval(minutes: Int) {
        _uiState.value = _uiState.value.copy(ttsTimeIntervalMin = minutes)
        viewModelScope.launch { settingsRepository.setTtsTimeIntervalMin(minutes) }
    }

    // ===== GPS-трекинг =====

    fun setGpsInterval(sec: Int) {
        val clamped = sec.coerceIn(5, 60)
        _uiState.value = _uiState.value.copy(gpsIntervalSec = clamped)
        viewModelScope.launch { settingsRepository.setGpsIntervalSec(clamped) }
    }

    fun setAutoCloseTime(time: String) {
        _uiState.value = _uiState.value.copy(autoCloseTime = time)
        viewModelScope.launch { settingsRepository.setAutoCloseTime(time) }
    }

    fun setAutoPauseSpeedLimit(limit: Int) {
        val clamped = limit.coerceIn(0, 50)
        _uiState.value = _uiState.value.copy(autoPauseSpeedLimitKmH = clamped)
        viewModelScope.launch { settingsRepository.setAutoPauseSpeedLimitKmH(clamped) }
    }

    fun setPointOfNoReturnWarningKm(km: Int) {
        val clamped = km.coerceIn(0, 15)
        _uiState.value = _uiState.value.copy(pointOfNoReturnWarningKm = clamped)
        viewModelScope.launch { settingsRepository.setPointOfNoReturnWarningKm(clamped) }
    }

    fun setAutoTransportDetectionEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoTransportDetectionEnabled = enabled)
        viewModelScope.launch { settingsRepository.setAutoTransportDetection(enabled) }
    }

    /** Циклическое переключение режима TTS: off → distance → time → off. */
    fun cycleTtsMode() {
        val nextMode = when (_uiState.value.ttsMode) {
            TTS_MODE_OFF -> TTS_MODE_DISTANCE
            TTS_MODE_DISTANCE -> TTS_MODE_TIME
            TTS_MODE_TIME -> TTS_MODE_OFF
            else -> TTS_MODE_OFF
        }
    }

    // ===== Система единиц =====
    
    fun setUnitSystem(unit: String) {
        _uiState.value = _uiState.value.copy(unitSystem = unit)
        viewModelScope.launch { settingsRepository.setUnitSystem(unit) }
    }

    // ===== Язык приложения (per-profile) =====
    
    fun setAppLanguage(langCode: String) {
        _uiState.value = _uiState.value.copy(appLanguageCode = langCode)

        // Сохраняем per-profile
        viewModelScope.launch { settingsRepository.setAppLanguageCode(langCode) }

        // Применяем
        val localeList = if (langCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(langCode)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /** Удаление коляски с опциональным переносом треков */
    fun deleteProfile(deletedId: Long, reassignToId: Long?, onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (reassignToId != null) {
                sessionRepository.reassignSessionsProfile(oldProfileId = deletedId, newProfileId = reassignToId)
            } else {
                // If we don't reassign, we should just let cascade delete happen, 
                // but since DaySession cascade deletes, it will drop them entirely.
            }
            
            // Fetch profile and delete it
            val profile = profileRepository.getProfileById(deletedId)
            if (profile != null) {
                profileRepository.deleteProfile(profile)
            }
            
            // Switch active profile if we just deleted the active one
            val currentActive = settingsRepository.getActiveProfileId()
            if (currentActive == deletedId) {
                // Switch to the first available if possible (we passed reassignToId, or get first available)
                val fallbackId = reassignToId ?: profileRepository.getAllProfiles().first().firstOrNull()?.id ?: 1L
                settingsRepository.setActiveProfileId(fallbackId)
            }
            onDeleted()
        }
    }

    /** Универсальное обновление параметров текущей коляски. */
    fun updateProfile(updater: (ProfileEntity) -> ProfileEntity) {
        viewModelScope.launch {
            activeProfile.value?.let { currentProfile ->
                val updated = updater(currentProfile)
                profileRepository.saveProfile(updated)
            }
        }
    }

    /** Экспорт телеметрии с выбранными категориями. */
    fun exportTelemetry(categories: Set<String>) {
        viewModelScope.launch {
            try {
                val intent = telemetryExportUseCase.export(categories)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            } catch (e: Exception) {
                // Логируем ошибку, но не крашим приложение
                android.util.Log.e("Telemetry", "Ошибка экспорта телеметрии", e)
            }
        }
    }
}
