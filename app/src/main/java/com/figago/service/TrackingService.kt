package com.figago.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.figago.data.entity.ProfileEntity
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.TrackRepository
import com.figago.domain.usecase.EndDayUseCase
import com.figago.domain.usecase.ForecastRemainingDistanceUseCase
import com.figago.domain.usecase.RecordLedEventUseCase
import com.figago.domain.usecase.StartDayUseCase
import com.figago.domain.usecase.StartTrackUseCase
import com.figago.domain.usecase.StopTrackUseCase
import com.figago.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import javax.inject.Inject


/**
 * Foreground Service для GPS-трекинга.
 *
 * Отвечает за:
 * - Получение GPS-координат через FusedLocationProviderClient
 * - Запись LocationPoint в Room
 * - Пересчёт дистанции (segment_distance, total_distance)
 * - Отображение persistent notification с текущей дистанцией
 * - Автозакрытие сессии в 23:59
 * - Обработку голосовых команд (LED-события, старт/стоп, озвучка)
 * - TTS-оповещения по км/минутам
 *
 * Управляется через Intent.action:
 * - ACTION_START_DAY         — начало дня
 * - ACTION_END_DAY           — конец дня
 * - ACTION_START_TRACK       — начало записи трека
 * - ACTION_STOP_TRACK        — остановка записи трека
 * - ACTION_LED_EVENT         — фиксация LED-события (декремент на 1)
 * - ACTION_SET_LED_COUNT     — каскадная установка ledCount (с extra EXTRA_LED_COUNT)
 * - ACTION_VOICE_START_TRACK — «Я поехал» (StartDay если нужно + StartTrack)
 * - ACTION_ANNOUNCE_STATUS   — «Какой пробег» (TTS-озвучка статуса)
 */
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    companion object {
        const val ACTION_START_DAY = "com.figago.action.START_DAY"
        const val ACTION_END_DAY = "com.figago.action.END_DAY"
        const val ACTION_START_TRACK = "com.figago.action.START_TRACK"
        const val ACTION_STOP_TRACK = "com.figago.action.STOP_TRACK"
        const val ACTION_LED_EVENT = "com.figago.action.LED_EVENT"
        const val ACTION_SET_LED_COUNT = "com.figago.action.SET_LED_COUNT"
        const val ACTION_VOICE_START_TRACK = "com.figago.action.VOICE_START_TRACK"
        const val ACTION_ANNOUNCE_STATUS = "com.figago.action.ANNOUNCE_STATUS"
        const val ACTION_SET_REALTIME_MODE = "com.figago.action.SET_REALTIME_MODE"
        const val ACTION_ACTIVITY_TRANSITION = "com.figago.action.ACTIVITY_TRANSITION"
        const val ACTION_SET_MANUAL_TRANSPORT = "com.figago.action.SET_MANUAL_TRANSPORT"

        const val EXTRA_LED_COUNT = "extra_led_count"
        const val EXTRA_IS_REALTIME = "extra_is_realtime"
        const val EXTRA_IS_TRANSPORT = "extra_is_transport"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "figago_tracking"

        /** Минимальное расстояние между точками для записи (метры). Фильтр GPS-шума. */
        private const val MIN_DISTANCE_THRESHOLD = 3.0f

        /** Максимальное правдоподобное расстояние между двумя точками (метры). Фильтр аномалий. */
        private const val MAX_DISTANCE_THRESHOLD = 500.0f
    }

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var trackRepository: TrackRepository
    @Inject lateinit var ledEventRepository: LedEventRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var trackingEngine: TrackingEngine
    @Inject lateinit var startDayUseCase: StartDayUseCase
    @Inject lateinit var endDayUseCase: EndDayUseCase
    @Inject lateinit var startTrackUseCase: StartTrackUseCase
    @Inject lateinit var stopTrackUseCase: StopTrackUseCase
    @Inject lateinit var recordLedEventUseCase: RecordLedEventUseCase
    @Inject lateinit var forecastUseCase: ForecastRemainingDistanceUseCase
    @Inject lateinit var ttsAnnouncer: TtsAnnouncerService
    @Inject lateinit var eventLogDao: com.figago.data.dao.EventLogDao

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    /** id активного отрезка пути (null — запись не идёт). */
    private var activeSegmentId: Long? = null

    /** Последняя зафиксированная точка (для расчёта дистанции). */
    private var lastLocation: Location? = null

    /** Текущая дистанция отрезка (метры). */
    private var currentSegmentDistance: Double = 0.0

    /** Флаг: идёт ли запись GPS. */
    private var isRecording = false
    private var isRealtimeMode = false

    /** Текущий профиль коляски (загружается при старте). */
    private var currentProfile: ProfileEntity? = null
    
    /** State Machine: счётчики измерений скорости */
    private var highSpeedCount = 0
    private var normalSpeedCount = 0
    private var isTransportMode = false

    private var isSysTransportMode = false

    /** Флаг, чтобы произносить предупреждение о точке невозврата один раз за запуск. */
    private var isPoNRWarningPlayed = false

    /** Периодический тикер для TTS-оповещений по времени (независимый от GPS). */
    private var ttsTickerJob: Job? = null

    /** Флаг вкл/выкл автоопределения транспорта из настроек. */
    private var autoTransportDetectionEnabled = true

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        ttsAnnouncer.initialize()

        lifecycleScope.launch {
            settingsRepository.observeAutoTransportDetection().collect { enabled ->
                autoTransportDetectionEnabled = enabled
                if (!enabled && isTransportMode) {
                    isTransportMode = false // Force exit transport mode if disabled dynamically
                }
            }
        }

        lifecycleScope.launch {
            profileRepository.getActiveProfile().collect { profile ->
                currentProfile = profile
                if (isRecording || activeSegmentId != null) {
                    val session = sessionRepository.getActiveSession()
                    updateNotification(session?.totalDistance ?: 0.0)
                }
            }
        }

        lifecycleScope.launch {
            var isFirstEmission = true
            kotlinx.coroutines.flow.combine(
                settingsRepository.observeTtsAnnounceMode(),
                settingsRepository.observeTtsDistanceIntervalKm(),
                settingsRepository.observeTtsTimeIntervalMin()
            ) { mode, dist, time ->
                Triple(mode, dist, time)
            }.collect { (mode, dist, time) ->
                if (!isFirstEmission) {
                    // Сбрасываем старый таймер и начинаем отсчет заново (п. 4.1)
                    if (isRecording) {
                        ttsAnnouncer.resetCounters()
                    }
                    // Логируем изменение (п. 1.4)
                    try {
                        val session = sessionRepository.getActiveSession()
                        if (session != null) {
                            eventLogDao.insert(
                                com.figago.data.entity.EventLogEntity(
                                    dayId = session.id,
                                    eventType = "SETTINGS_CHANGED",
                                    context = "{\"ttsMode\": \"$mode\", \"ttsDistance\": $dist, \"ttsTime\": $time}"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TrackingService", "Failed to log SETTINGS_CHANGED", e)
                    }
                }
                isFirstEmission = false
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_DAY -> handleStartDay()
            ACTION_END_DAY -> handleEndDay()
            ACTION_START_TRACK -> handleStartTrack()
            ACTION_STOP_TRACK -> handleStopTrack()
            ACTION_LED_EVENT -> handleLedEvent()
            ACTION_SET_LED_COUNT -> {
                val count = intent.getIntExtra(EXTRA_LED_COUNT, -1)
                if (count >= 0) handleSetLedCount(count)
            }
            ACTION_VOICE_START_TRACK -> handleVoiceStartTrack()
            ACTION_ANNOUNCE_STATUS -> handleAnnounceStatus()
            ACTION_SET_REALTIME_MODE -> {
                val isRealtime = intent.getBooleanExtra(EXTRA_IS_REALTIME, false)
                handleSetRealtimeMode(isRealtime)
            }
            ACTION_ACTIVITY_TRANSITION -> {
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result = ActivityTransitionResult.extractResult(intent)
                    result?.transitionEvents?.forEach { event ->
                        if (event.activityType == DetectedActivity.IN_VEHICLE) {
                            if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                isSysTransportMode = true
                                android.util.Log.i("TrackingService", "Activity API: IN_VEHICLE ENTER")
                            } else if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                                isSysTransportMode = false
                                android.util.Log.i("TrackingService", "Activity API: IN_VEHICLE EXIT")
                            }
                        }
                    }
                }
            }
            ACTION_SET_MANUAL_TRANSPORT -> {
                val isTransport = intent.getBooleanExtra(EXTRA_IS_TRANSPORT, false)
                handleSetManualTransport(isTransport)
            }
        }

        return START_STICKY
    }

    // ===== Обработчики команд =====

    private fun handleStartDay() {
        lifecycleScope.launch {
            try {
                startDayUseCase()
                startForeground(NOTIFICATION_ID, buildNotification(0.0))
                handleStartTrack()
            } catch (e: IllegalStateException) {
                // Сессия уже существует — просто запускаем foreground
                startForeground(NOTIFICATION_ID, buildNotification(0.0))
                handleStartTrack()
            }
        }
    }

    private fun handleEndDay() {
        lifecycleScope.launch {
            stopLocationUpdates()
            endDayUseCase()
            ttsAnnouncer.shutdown()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleStartTrack() {
        lifecycleScope.launch {
            try {
                currentProfile = profileRepository.getActiveProfile().first()
                if (activeSegmentId != null) return@launch

                val segmentId = startTrackUseCase()
                activeSegmentId = segmentId
                currentSegmentDistance = 0.0
                lastLocation = null
                isRecording = true
                highSpeedCount = 0
                normalSpeedCount = 0
                isTransportMode = false
                
                ttsAnnouncer.resetCounters()
                trackingEngine.reset()
                trackingEngine.updateState { it.copy(isRecording = true) }
                
                startLocationUpdates()
                startTtsTicker()
                
                val session = sessionRepository.getActiveSession()
                updateNotification(session?.totalDistance ?: 0.0)
            } catch (e: IllegalStateException) {
                android.util.Log.w("TrackingService", "Не удалось начать запись: ${e.message}")
            }
        }
    }

    private fun handleStopTrack() {
        ttsTickerJob?.cancel()
        ttsTickerJob = null
        lifecycleScope.launch {
            stopLocationUpdates()
            stopTrackUseCase()
            activeSegmentId = null
            isRecording = false
            trackingEngine.updateState { it.copy(isRecording = false) }

            // Обновляем уведомление — показываем суммарную дистанцию без записи
            val session = sessionRepository.getActiveSession()
            updateNotification(session?.totalDistance ?: 0.0)
        }
    }

    /**
     * Периодический тикер для TTS-оповещений по времени.
     * Запускается каждые 30 секунд и проверяет, прошёл ли интервал.
     * Работает независимо от GPS — даже если коляска стоит.
     */
    private fun startTtsTicker() {

        ttsTickerJob?.cancel()
        ttsTickerJob = lifecycleScope.launch {

            while (true) {

                delay(30_000L) // Проверяем каждые 30 секунд

                if (!isRecording) break

                val session = sessionRepository.getActiveSession() ?: continue
                checkTtsAnnouncement(session.totalDistance, session.id)
            }
        }
    }

    private fun handleLedEvent() {
        lifecycleScope.launch {
            try {
                val remaining = recordLedEventUseCase()
                vibrateConfirmation()
                android.util.Log.i("TrackingService", "LED-событие зафиксировано. Осталось: $remaining")
            } catch (e: IllegalStateException) {
                android.util.Log.w("TrackingService", "LED-событие не записано: ${e.message}")
            }
        }
    }

    /**
     * Каскадная установка ledCount (при нажатии на конкретный индикатор).
     */
    private fun handleSetLedCount(targetCount: Int) {
        lifecycleScope.launch {
            try {
                recordLedEventUseCase(targetCount)
                vibrateConfirmation()
                android.util.Log.i("TrackingService", "LED установлен каскадно: $targetCount")
            } catch (e: Exception) {
                android.util.Log.w("TrackingService", "Ошибка установки LED: ${e.message}")
            }
        }
    }

    /**
     * «Я поехал» — голосовая команда.
     *
     * Поведение:
     * - DayState.IDLE → автоматически StartDay → затем StartTrack
     * - DayState.PAUSED → только StartTrack
     * - DayState.RECORDING → игнорируем, вибрация
     */
    private fun handleVoiceStartTrack() {
        lifecycleScope.launch {

            val session = sessionRepository.getActiveSession()

            if (session == null) {

                // День не начат → начинаем день, затем трек
                try {
                    startDayUseCase()
                    startForeground(NOTIFICATION_ID, buildNotification(0.0))
                } catch (_: IllegalStateException) {
                    startForeground(NOTIFICATION_ID, buildNotification(0.0))
                }
            }

            // Проверяем, не идёт ли уже запись
            val currentSession = sessionRepository.getActiveSession()

            if (currentSession != null) {
                val activeSegment = trackRepository.getActiveSegment(currentSession.id)
                if (activeSegment != null) {
                    // Уже записываем → вибрация «уже записываю»
                    vibrateConfirmation()
                    return@launch
                }
            }

            // Начинаем запись
            handleStartTrack()
            vibrateConfirmation()
        }
    }

    /**
     * «Какой пробег» — голосовая команда TTS.
     *
     * Работает ВСЕГДА, независимо от настроек автоматических оповещений.
     */
    private fun handleAnnounceStatus() {
        lifecycleScope.launch {

            val session = sessionRepository.getActiveSession()

            if (session == null) {
                ttsAnnouncer.announceNoSession()
                vibrateConfirmation()
                return@launch
            }

            val lastEvent = ledEventRepository.getLastEvent(session.id)
            val currentLedCount = lastEvent?.ledCountRemaining ?: RecordLedEventUseCase.MAX_LED_COUNT
            val distanceAtLastLed = lastEvent?.distanceAtEvent ?: 0.0

            val forecast = forecastUseCase(
                currentLedCount = currentLedCount,
                currentTotalDistance = session.totalDistance,
                distanceAtLastLedEvent = distanceAtLastLed,
            )

            ttsAnnouncer.announceNow(session.totalDistance, forecast)
            vibrateConfirmation()
        }
    }

    private fun handleSetRealtimeMode(isRealtime: Boolean) {
        if (isRealtimeMode == isRealtime) return
        isRealtimeMode = isRealtime
        if (isRecording && activeSegmentId != null) {
            stopLocationUpdates()
            isRecording = true // stopLocationUpdates сбрасывает флаг, восстанавливаем
            startLocationUpdates()
        }
    }

    private fun handleSetManualTransport(isTransport: Boolean) {
        lifecycleScope.launch {
            trackingEngine.setManualTransportActive(isTransport)
            vibrateConfirmation()
        }
    }

    // ===== GPS-трекинг =====

    @Suppress("MissingPermission") // Разрешение проверяется на уровне UI перед запуском сервиса
    private fun startLocationUpdates() {
        val delayMillis = if (isRealtimeMode) 0L else 30000L
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(1.5f)
            .setMaxUpdateDelayMillis(delayMillis)
            .build()
            
        startActivityTransitionUpdates()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                lifecycleScope.launch {
                    var currentSegmentId = activeSegmentId ?: return@launch
                    val pointsToInsert = mutableListOf<com.figago.domain.model.LocationPoint>()

                    for (location in result.locations) {
                        val previousLocation = lastLocation
                        
                        // Проверяем разрыв по времени (убийство процесса)
                        if (previousLocation != null) {
                            val timeDiffMs = location.time - previousLocation.time
                            if (timeDiffMs > 120_000) {
                                // Сохраняем накопленные перед разрывом точки
                                if (pointsToInsert.isNotEmpty()) {
                                    trackRepository.addLocationPoints(pointsToInsert)
                                    trackRepository.updateSegmentDistance(currentSegmentId, currentSegmentDistance)
                                    pointsToInsert.clear()
                                }
                                
                                // Закрываем старый сегмент
                                trackRepository.finishSegment(currentSegmentId, previousLocation.time, currentSegmentDistance)
                                
                                // Начинаем новый
                                try {
                                    currentSegmentId = startTrackUseCase()
                                    activeSegmentId = currentSegmentId
                                    currentSegmentDistance = 0.0
                                    lastLocation = null
                                    highSpeedCount = 0
                                    normalSpeedCount = 0
                                    isTransportMode = false
                                } catch (e: Exception) {
                                    activeSegmentId = null
                                    return@launch
                                }
                            }
                        }

                        val point = processLocation(location, currentSegmentId)
                        if (point != null) {
                            pointsToInsert.add(point)
                        }
                    }

                    if (pointsToInsert.isNotEmpty()) {
                        trackRepository.addLocationPoints(pointsToInsert)
                        trackRepository.updateSegmentDistance(currentSegmentId, currentSegmentDistance)

                        val session = sessionRepository.getActiveSession()
                        if (session != null) {
                            val segments = trackRepository.getSegmentsByDayId(session.id)
                            val totalDistance = segments.sumOf { it.segmentDistance }
                            sessionRepository.updateDistance(session.id, totalDistance)
                            updateNotification(totalDistance)
                            trackingEngine.updateState { it.copy(totalDistanceMeters = totalDistance) }
                            checkTtsAnnouncement(totalDistance, session.id)
                        }
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
        
        stopActivityTransitionUpdates()
        isRecording = false
    }

    @Suppress("MissingPermission")
    private fun startActivityTransitionUpdates() {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )
        val request = ActivityTransitionRequest(transitions)
        
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, TrackingService::class.java).apply { action = ACTION_ACTIVITY_TRANSITION }, flag)
        
        try {
            ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, pendingIntent)
        } catch (e: Exception) {
            android.util.Log.e("TrackingService", "Failed to request activity transitions", e)
        }
    }

    @Suppress("MissingPermission")
    private fun stopActivityTransitionUpdates() {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getService(this, 0, Intent(this, TrackingService::class.java).apply { action = ACTION_ACTIVITY_TRANSITION }, flag)
        
        try {
            ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pendingIntent)
        } catch (e: Exception) {
            android.util.Log.e("TrackingService", "Failed to remove activity transitions", e)
        }
    }

    /**
     * Обработка новой GPS-точки.
     * Возвращает LocationPoint для batch-вставки, или null если точка отфильтрована.
     */
    private fun processLocation(location: Location, segmentId: Long): com.figago.domain.model.LocationPoint? {
        // Фильтр по точности: отбрасываем явно ошибочные точки с погрешностью > 25 метров.
        if (location.hasAccuracy() && location.accuracy > 25.0f) {
            return null
        }

        // Фильтр холодных прыжков GPS: если это первая точка сегмента и погрешность > 30, отбрасываем
        if (lastLocation == null && location.hasAccuracy() && location.accuracy > 30.0f) {
            return null
        }

        val previousLocation = lastLocation

        if (previousLocation != null) {
            val distance = previousLocation.distanceTo(location)
            val timeDiffMs = location.time - previousLocation.time

            var speedMs = 0.0

            // Фильтр: невозможная скорость (телепортация).
            if (timeDiffMs > 0) {
                speedMs = distance / (timeDiffMs / 1000.0)
                if (speedMs > 7.0) {
                    return null
                }
            }

            // Transport State Machine: гибрид системного Activity API и скорости
            val speedKmH = if (location.hasSpeed()) location.speed * 3.6f else (speedMs * 3.6).toFloat()
            val maxSpeed = currentProfile?.maxSpeed ?: 10.0f
            val transportThreshold = maxSpeed * 1.2f

            if (autoTransportDetectionEnabled && (isSysTransportMode || speedKmH > transportThreshold)) {
                highSpeedCount++
                normalSpeedCount = 0
                if (highSpeedCount >= 5 && !isTransportMode) {
                    isTransportMode = true
                    android.util.Log.i("TrackingService", "Переход в режим ТРАНСПОРТ")
                }
            } else {
                normalSpeedCount++
                highSpeedCount = 0
                if (normalSpeedCount >= 5 && isTransportMode) {
                    isTransportMode = false
                    android.util.Log.i("TrackingService", "Возврат в режим КОЛЯСКА")
                }
            }

            val isManualTransport = trackingEngine.trackingState.value.isManualTransportActive
            val finalIsTransport = isTransportMode || isManualTransport

            // Фильтр: слишком маленькое перемещение (GPS-шум)
            if (distance < MIN_DISTANCE_THRESHOLD) {
                trackingEngine.updateState { it.copy(instantSpeedKmh = 0.0, isTransport = finalIsTransport) }
                lifecycleScope.launch {
                    val session = sessionRepository.getActiveSession()
                    if (session != null) checkTtsAnnouncement(session.totalDistance, session.id)
                }
                return null
            }

            // Фильтр: аномальный прыжок (потеря сигнала, тоннель)
            if (distance > MAX_DISTANCE_THRESHOLD) {
                lastLocation = location
                return null
            }

            // Добавляем дистанцию коляске только если не в транспорте
            if (!finalIsTransport) {
                currentSegmentDistance += distance
            }

            lifecycleScope.launch {
                val session = sessionRepository.getActiveSession()
                val totalSegTime = (System.currentTimeMillis() - (trackRepository.getActiveSegment(session?.id ?: 0)?.startTime ?: System.currentTimeMillis())) / 1000.0
                val avgSpeed = if (totalSegTime > 0) (currentSegmentDistance / totalSegTime) * 3.6 else 0.0

                trackingEngine.updateState { it.copy(
                    instantSpeedKmh = speedKmH.toDouble(),
                    segmentAverageSpeedKmh = avgSpeed,
                    isTransport = finalIsTransport,
                )}
            }
        }

        lastLocation = location
        
        val isManualTransport = trackingEngine.trackingState.value.isManualTransportActive
        val finalIsTransport = isTransportMode || isManualTransport

        return com.figago.domain.model.LocationPoint(
            id = 0,
            segmentId = segmentId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            isTransport = finalIsTransport,
        )
    }

    /**
     * Проверяет и запускает TTS-оповещение (автоматический режим по км или по минутам).
     */
    private suspend fun checkTtsAnnouncement(totalDistanceM: Double, sessionId: Long) {

        val ttsMode = settingsRepository.getTtsAnnounceMode()
        if (ttsMode == SettingsRepository.TTS_MODE_OFF) return

        val distanceIntervalKm = settingsRepository.getTtsDistanceIntervalKm()
        val timeIntervalMin = settingsRepository.getTtsTimeIntervalMin()

        // Считаем прогноз для озвучки
        val lastEvent = ledEventRepository.getLastEvent(sessionId)
        val currentLedCount = lastEvent?.ledCountRemaining ?: RecordLedEventUseCase.MAX_LED_COUNT
        val distanceAtLastLed = lastEvent?.distanceAtEvent ?: 0.0

        val forecast = try {
            forecastUseCase(
                currentLedCount = currentLedCount,
                currentTotalDistance = totalDistanceM,
                distanceAtLastLedEvent = distanceAtLastLed,
            )
        } catch (e: Exception) {
            null
        }

        // Point of No Return logic — использует фиксированный maxMileage из профиля
        val profile = profileRepository.getActiveProfile().first()
        if (profile != null && profile.maxMileage != null && profile.maxMileage > 0) {
            val ponrWarningKm = settingsRepository.getPointOfNoReturnWarningKm()
            val currentDistanceKm = totalDistanceM / 1000.0
            val maxMileageKm = profile.maxMileage.toDouble()
            val ponrKm = maxMileageKm / 2.0

            if (ponrWarningKm > 0 && !isPoNRWarningPlayed && currentDistanceKm >= (ponrKm - ponrWarningKm)) {
                val remainToPonr = ponrKm - currentDistanceKm
                val x = kotlin.math.floor(remainToPonr).toInt().coerceAtLeast(0)
                val currentRounded = kotlin.math.round(currentDistanceKm).toInt()
                ttsAnnouncer.announcePointOfNoReturn(x, currentRounded)
                isPoNRWarningPlayed = true
            }
        }

        ttsAnnouncer.checkAndAnnounce(
            currentDistanceM = totalDistanceM,
            forecastKm = forecast,
            ttsMode = ttsMode,
            distanceIntervalKm = distanceIntervalKm,
            timeIntervalMin = timeIntervalMin,
        )
    }

    // ===== Уведомление =====

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(com.figago.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(com.figago.R.string.notif_title)
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(distanceMeters: Double): Notification {
        val distanceKm = distanceMeters / 1000.0

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(this, 3, Intent(this, TrackingService::class.java).apply { action = ACTION_END_DAY }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, getString(com.figago.R.string.notif_btn_stop), stopIntent)

        val ledIntent = PendingIntent.getService(this, 4, Intent(this, TrackingService::class.java).apply { action = ACTION_LED_EVENT }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val ledAction = NotificationCompat.Action(android.R.drawable.presence_online, getString(com.figago.R.string.notif_btn_led), ledIntent)

        val stateText = if (isTransportMode) " [Транспорт]" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.figago.R.string.notification_tracking_title))
            .setContentText(getString(com.figago.R.string.notification_tracking_text, distanceKm) + stateText)
            .setSmallIcon(currentProfile?.iconId?.takeIf { it != 0 } ?: com.figago.R.drawable.ic_notification_preview)
            .setContentIntent(pendingIntent)
            .addAction(stopAction)
            .addAction(ledAction)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(distanceMeters: Double) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(distanceMeters))
    }

    // ===== Вибрация (подтверждение команды) =====

    private fun vibrateConfirmation() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        ttsTickerJob?.cancel()
        stopLocationUpdates()
        ttsAnnouncer.shutdown()
        super.onDestroy()
    }
}
