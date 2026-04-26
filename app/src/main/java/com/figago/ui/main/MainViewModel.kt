package com.figago.ui.main

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.figago.domain.model.DaySession
import com.figago.domain.model.LedEvent
import com.figago.domain.model.TrackSegment
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.TrackRepository
import com.figago.domain.usecase.EndDayUseCase
import com.figago.domain.usecase.ForecastRemainingDistanceUseCase
import com.figago.domain.usecase.FormatMetricsUseCase
import com.figago.domain.usecase.PreflightChecker
import com.figago.domain.usecase.PreflightStatus
import com.figago.domain.usecase.RecordLedEventUseCase
import com.figago.domain.usecase.StartDayUseCase
import com.figago.domain.usecase.StartTrackUseCase
import com.figago.domain.usecase.StopTrackUseCase
import com.figago.service.TrackingEngine
import com.figago.service.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.figago.domain.repository.ProfileRepository
import com.figago.data.entity.ProfileEntity
import com.figago.R

/**
 * Состояние экрана жизненного цикла дня.
 */
enum class DayState {
    /** День не начат — показать кнопку «Начало дня». */
    IDLE,
    /** День активен, запись на паузе — показать «Старт» и «Конец дня». */
    PAUSED,
    /** День активен, идёт запись GPS — показать «Стоп» и «Конец дня». */
    RECORDING,
}

/**
 * Полное состояние главного экрана.
 *
 * @property dayState              текущее состояние жизненного цикла дня
 * @property totalDistanceM        суммарная дистанция за день (метры)
 * @property ledCount              оставшееся количество индикаторов (5..0)
 * @property forecastRemainingKm   прогнозный остаточный пробег (км) или null
 * @property distanceAtLastLedEvent пробег на момент последнего LED-события (метры)
 * @property errorMessage          сообщение об ошибке (null = нет ошибки)
 * @property activeSegmentId       id активного отрезка (null = запись не идёт)
 * @property sessionId             id текущей сессии (null = день не начат)
 */
data class MainUiState(
    val dayState: DayState = DayState.IDLE,
    val totalDistanceM: Double = 0.0,
    val ledCount: Int = RecordLedEventUseCase.MAX_LED_COUNT,
    val forecastRemainingKm: Double? = null,
    val distanceAtLastLedEvent: Double = 0.0,
    val errorMessage: String? = null,
    val activeSegmentId: Long? = null,
    val sessionId: Long? = null,
    val previousSegmentsDurationSec: Long = 0L,
    val currentSegmentStartTimeMs: Long? = null,
)

/**
 * ViewModel главного экрана FigaGo.
 *
 * Управляет жизненным циклом дня и трека, подписывается на реактивные
 * потоки из репозиториев, передаёт команды в TrackingService.
 * Вычисляет прогнозный остаточный пробег через ForecastRemainingDistanceUseCase.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
    private val ledEventRepository: LedEventRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingEngine: TrackingEngine,
    private val startDayUseCase: StartDayUseCase,
    private val endDayUseCase: EndDayUseCase,
    private val startTrackUseCase: StartTrackUseCase,
    private val stopTrackUseCase: StopTrackUseCase,
    private val recordLedEventUseCase: RecordLedEventUseCase,
    private val forecastUseCase: ForecastRemainingDistanceUseCase,
    private val formatMetricsUseCase: FormatMetricsUseCase,
    private val preflightChecker: PreflightChecker,
    private val lampStatisticsRepository: com.figago.domain.repository.LampStatisticsRepository,
) : AndroidViewModel(application) {

    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Основной UI-стейт, собранный из нескольких реактивных потоков:
     * - активная сессия → dayState, totalDistance, sessionId
     * - LED-события активной сессии → ledCount, distanceAtLastLedEvent
     * - активный сегмент → activeSegmentId, RECORDING/PAUSED
     * - прогноз остаточного пробега
     */
    val uiState: StateFlow<MainUiState> = sessionRepository.observeActiveSession()
        .flatMapLatest { session ->
            if (session == null) {
                // День не начат
                flowOf(MainUiState(dayState = DayState.IDLE))
            } else {
                // Комбинируем данные активной сессии и отрезков трека
                combine(
                    ledEventRepository.observeEventsByDayId(session.id),
                    sessionRepository.observeById(session.id),
                    trackRepository.observeSegmentsByDayId(session.id),
                    _errorMessage,
                ) { ledEvents, freshSession, segments, error ->
                    val activeSegment = segments.find { it.endTime == null }
                    val previousDurationSec = segments.filter { it.endTime != null }
                        .sumOf { it.endTime!! - it.startTime } / 1000
                    buildUiState(freshSession ?: session, ledEvents, activeSegment, previousDurationSec, error)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    /** Реактивный поток всех полилиний (отрезков) для текущего активного дня. */
    val polylines: StateFlow<List<com.figago.ui.components.TrackPolylineData>> = sessionRepository.observeActiveSession()
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList<com.figago.ui.components.TrackPolylineData>())
            } else {
                trackRepository.observeSegmentsByDayId(session.id).flatMapLatest { segments ->
                    if (segments.isEmpty()) {
                        flowOf(emptyList<com.figago.ui.components.TrackPolylineData>())
                    } else {
                        val pointFlows = segments.map { segment ->
                            trackRepository.observePointsBySegmentId(segment.id)
                        }
                        combine(pointFlows) { pointsArray ->
                            val allPolylines = mutableListOf<com.figago.ui.components.TrackPolylineData>()
                            
                            for (points in pointsArray) {
                                if (points.isEmpty()) continue
                                
                                var currentIsTransport = points.first().isTransport
                                var currentChunk = mutableListOf<com.google.android.gms.maps.model.LatLng>()
                                
                                for (point in points) {
                                    if (point.isTransport != currentIsTransport && currentChunk.isNotEmpty()) {
                                        allPolylines.add(
                                            com.figago.ui.components.TrackPolylineData(
                                                points = currentChunk.toList(),
                                                color = if (currentIsTransport) androidx.compose.ui.graphics.Color(0xFF43A047) else androidx.compose.ui.graphics.Color(0xFFFF9800)
                                            )
                                        )
                                        currentIsTransport = point.isTransport
                                        val lastPt = currentChunk.last()
                                        currentChunk = mutableListOf(lastPt)
                                    }
                                    currentChunk.add(com.google.android.gms.maps.model.LatLng(point.latitude, point.longitude))
                                }
                                
                                if (currentChunk.isNotEmpty()) {
                                    allPolylines.add(
                                        com.figago.ui.components.TrackPolylineData(
                                            points = currentChunk.toList(),
                                            color = if (currentIsTransport) androidx.compose.ui.graphics.Color(0xFF43A047) else androidx.compose.ui.graphics.Color(0xFFFF9800)
                                        )
                                    )
                                }
                            }
                            allPolylines
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trackingState = trackingEngine.trackingState

    val isDiagnosticsSkipped: StateFlow<Boolean> = settingsRepository.observeSkipDiagnostics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val unitSystem: StateFlow<String> = settingsRepository.observeUnitSystem()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.UNIT_KILOMETERS)

    // Formatted active stats (Instant Speed)
    val displayInstantSpeed = combine(trackingState, unitSystem) { trackInfo, _ ->
        formatMetricsUseCase.invoke(0.0, trackInfo.instantSpeedKmh).speedValue.let {
            if (it >= 10.0) String.format("%.0f", it) else String.format("%.1f", it)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0.0")

    val displayInstantSpeedUnit = combine(trackingState, unitSystem) { _, unit ->
        if (unit == SettingsRepository.UNIT_MILES) application.getString(R.string.unit_mph)
        else application.getString(R.string.unit_kmh)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), application.getString(R.string.unit_kmh))

    // Formatted active stats (Average Speed)
    val displayAvgSpeed = combine(trackingState, unitSystem) { trackInfo, _ ->
        formatMetricsUseCase.invoke(0.0, trackInfo.segmentAverageSpeedKmh).speedValue.let {
            String.format("%.1f", it)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0.0")

    // Formatted active stats (Distance)
    val displayTotalDistance = combine(uiState, unitSystem) { state, _ ->
        formatMetricsUseCase.invoke(state.totalDistanceM, 0.0).distanceValue.let {
            if (it >= 100.0) String.format("%.0f", it) else String.format("%.2f", it)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0.00")

    val displayDistanceUnit = combine(uiState, unitSystem) { _, unit ->
        if (unit == SettingsRepository.UNIT_MILES) application.getString(R.string.unit_mi)
        else application.getString(R.string.unit_km)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), application.getString(R.string.unit_km))

    val profiles: StateFlow<List<ProfileEntity>> = profileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = profileRepository.getActiveProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Предупреждение о расхождении статистики: true если хотя бы одна лампочка
     * имеет среднюю статистику хуже установленного значения на >20%.
     */
    val statisticsWarning: StateFlow<Boolean> = settingsRepository.observeActiveProfileId()
        .flatMapLatest { profileId ->
            profileRepository.getProfileByIdFlow(profileId).combine(
                flowOf(profileId)
            ) { profile, pId -> Pair(profile, pId) }
        }
        .flatMapLatest { (profile, profileId) ->
            kotlinx.coroutines.flow.flow {
                if (profile == null || profile.ledDistances == null) {
                    emit(false)
                    return@flow
                }
                val averages = lampStatisticsRepository.getAveragesForProfile(profileId)
                if (averages.isEmpty()) {
                    emit(false)
                    return@flow
                }
                val distances = profile.ledDistances
                var hasWarning = false
                for ((lampIndex, avgKm) in averages) {
                    val setKm = distances.getOrNull(lampIndex) ?: continue
                    if (setKm <= 0f) continue
                    val deviation = (setKm - avgKm) / setKm
                    if (deviation > 0.20f) {
                        hasWarning = true
                        break
                    }
                }
                emit(hasWarning)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Собирает MainUiState из актуальных данных.
     * Определяет dayState на основе наличия активного сегмента,
     * вычисляет прогноз остаточного пробега.
     */
    private suspend fun buildUiState(
        session: DaySession,
        ledEvents: List<LedEvent>,
        activeSegment: TrackSegment?,
        previousDurationSec: Long,
        error: String?,
    ): MainUiState {

        val ledCount = if (ledEvents.isEmpty()) {
            RecordLedEventUseCase.MAX_LED_COUNT
        } else {
            ledEvents.last().ledCountRemaining
        }

        val distanceAtLastLed = if (ledEvents.isEmpty()) 0.0 else ledEvents.last().distanceAtEvent

        val dayState = if (activeSegment != null) DayState.RECORDING else DayState.PAUSED

        // Вычисляем прогноз остаточного пробега
        val forecast = try {
            forecastUseCase(
                currentLedCount = ledCount,
                currentTotalDistance = session.totalDistance,
                distanceAtLastLedEvent = distanceAtLastLed,
            )
        } catch (e: Exception) {
            null
        }

        return MainUiState(
            dayState = dayState,
            totalDistanceM = session.totalDistance,
            ledCount = ledCount,
            forecastRemainingKm = forecast,
            distanceAtLastLedEvent = distanceAtLastLed,
            errorMessage = error,
            activeSegmentId = activeSegment?.id,
            sessionId = session.id,
            previousSegmentsDurationSec = previousDurationSec,
            currentSegmentStartTimeMs = activeSegment?.startTime,
        )
    }

    // ===== Действия пользователя =====

    /** Начало дня — запуск сервиса и создание сессии. */
    fun startDay() {
        sendServiceCommand(TrackingService.ACTION_START_DAY)
    }

    /** Конец дня — остановка записи и закрытие сессии. */
    fun endDay() {
        sendServiceCommand(TrackingService.ACTION_END_DAY)
    }

    /** Начало записи GPS-трека. */
    fun startTrack() {
        sendServiceCommand(TrackingService.ACTION_START_TRACK)
    }

    /** Остановка записи GPS-трека. */
    fun stopTrack() {
        sendServiceCommand(TrackingService.ACTION_STOP_TRACK)
    }

    /** Фиксация LED-события (кнопка «Лампочка погасла» — декремент на 1). */
    fun recordLedEvent() {
        sendServiceCommand(TrackingService.ACTION_LED_EVENT)
    }

    /**
     * Каскадная установка ledCount (нажатие на конкретный индикатор).
     *
     * @param targetCount целевое количество горящих индикаторов
     */
    fun setLedCount(targetCount: Int) {
        val intent = Intent(application, TrackingService::class.java).apply {
            action = TrackingService.ACTION_SET_LED_COUNT
            putExtra(TrackingService.EXTRA_LED_COUNT, targetCount)
        }
        application.startService(intent)
    }

    /** Установка режима реального времени для GPS пакетирования */
    fun setRealtimeMode(isRealtime: Boolean) {
        val intent = Intent(application, TrackingService::class.java).apply {
            action = TrackingService.ACTION_SET_REALTIME_MODE
            putExtra(TrackingService.EXTRA_IS_REALTIME, isRealtime)
        }
        application.startService(intent)
    }

    fun switchActiveProfile(profileId: Long) {
        viewModelScope.launch {
            settingsRepository.setActiveProfileId(profileId)
            // It will automagically trickle down to TrackingService when it starts/updates since it fetches flow!
        }
    }

    /**
     * Создать новый профиль с инкрементальным именем и сделать его активным.
     */
    fun createAndSwitchProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            val existingProfiles = profileRepository.getAllProfiles().first()
            val existingNames = existingProfiles.map { it.name }.toSet()
            
            var baseName = application.getString(R.string.new_wheelchair_default_name)
            var name = baseName
            var counter = 2
            while (existingNames.contains(name)) {
                name = "$baseName $counter"
                counter++
            }
            
            val newProfile = ProfileEntity(
                name = name,
                type = com.figago.data.entity.ProfileType.ELECTRIC,
                iconId = R.drawable.ic_notification_preview,
                maxMileage = 10f,
                ledCount = 5,
                maxSpeed = 10f
            )
            val newId = profileRepository.saveProfile(newProfile)
            settingsRepository.setActiveProfileId(newId)
            onComplete()
        }
    }

    /** Очистить сообщение об ошибке. */
    fun clearError() {
        _errorMessage.value = null
    }

    fun checkPreflight(): PreflightStatus = preflightChecker.check()

    /** Отправить команду в TrackingService через Intent.action. */
    private fun sendServiceCommand(action: String) {

        val intent = Intent(application, TrackingService::class.java).apply {
            this.action = action
        }

        application.startService(intent)
    }
}
