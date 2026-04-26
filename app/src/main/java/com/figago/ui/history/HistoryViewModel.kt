package com.figago.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.figago.domain.model.DaySession
import com.figago.domain.model.LedEvent
import com.figago.domain.model.TrackSegment
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.TrackRepository
import com.figago.domain.usecase.DayStats
import com.figago.domain.usecase.GetDayStatsUseCase
import com.figago.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для экрана истории и деталей дня.
 *
 * Предоставляет:
 * - Реактивный список всех сессий (для HistoryScreen)
 * - Детальную статистику выбранного дня (для DayDetailScreen)
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetHistoryUseCase,
    private val getDayStatsUseCase: GetDayStatsUseCase,
    private val sessionRepository: com.figago.domain.repository.SessionRepository,
    private val trackRepository: TrackRepository,
    private val ledEventRepository: LedEventRepository,
    private val profileRepository: com.figago.domain.repository.ProfileRepository,
) : ViewModel() {

    /** Активный профиль для отображения иконки */
    val activeProfile = profileRepository.getActiveProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _showAllProfiles = MutableStateFlow(false)
    val showAllProfiles: StateFlow<Boolean> = _showAllProfiles.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<DaySession>> = combine(
        _showAllProfiles,
        profileRepository.getActiveProfile()
    ) { showAll, profile ->
        if (showAll) {
            sessionRepository.observeAllSessionsGroupedByDate()
        } else {
            val activeId = profile?.id
            if (activeId != null) {
                sessionRepository.observeAllSessions(activeId)
            } else {
                flowOf(emptyList())
            }
        }
    }.flatMapLatest { it }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun toggleShowAllProfiles(showAll: Boolean) {
        _showAllProfiles.value = showAll
    }

    /** Детальная статистика выбранного дня. */
    private val _dayStats = MutableStateFlow<DayStats?>(null)
    val dayStats: StateFlow<DayStats?> = _dayStats.asStateFlow()

    /** Данные полилиний для отрисовки карты на экране деталей. */
    private val _polylines = MutableStateFlow<List<com.figago.ui.components.TrackPolylineData>>(emptyList())
    val polylines: StateFlow<List<com.figago.ui.components.TrackPolylineData>> = _polylines.asStateFlow()

    /** Загружает детали конкретного дня для экрана деталей. */
    fun loadDayStats(sessionId: Long) {
        viewModelScope.launch {
            val showAll = _showAllProfiles.value
            val primaryStats = getDayStatsUseCase(sessionId)
            
            val allSegments = mutableListOf<TrackSegment>()
            var dayStatsValue = primaryStats
            
            if (primaryStats != null && showAll) {
                // If showAllProfiles is true, fetch segments from ALL sessions of that date
                val allDaySessions = sessionRepository.getAllSessionsByDate(primaryStats.session.date)
                var totalDist = 0.0
                var totalMovingMs = 0L
                var ledCount = 0
                val allLedEvents = mutableListOf<LedEvent>()
                
                for (session in allDaySessions) {
                    val segments = trackRepository.getSegmentsByDayId(session.id)
                    allSegments.addAll(segments)
                    
                    val leds = ledEventRepository.getEventsByDayId(session.id)
                    allLedEvents.addAll(leds)
                    
                    totalDist += session.totalDistance
                    ledCount += leds.size
                    totalMovingMs += segments.filter { it.endTime != null }.sumOf { it.endTime!! - it.startTime }
                }
                
                dayStatsValue = primaryStats.copy(
                    segments = allSegments,
                    ledEvents = allLedEvents,
                    totalDistance = totalDist,
                    segmentCount = allSegments.size,
                    totalMovingTimeMs = totalMovingMs,
                    ledEventCount = ledCount
                )
            } else if (primaryStats != null) {
                allSegments.addAll(primaryStats.segments)
            }
            
            var totalWalkDist = 0.0
            var totalTransportDist = 0.0
            var totalWalkTime = 0L
            var totalTransportTime = 0L

            // Загружаем точки для каждого сегмента
            if (dayStatsValue != null) {
                val pLines = mutableListOf<com.figago.ui.components.TrackPolylineData>()
                for (segment in allSegments) {
                    val points = trackRepository.getPointsBySegmentId(segment.id)
                    if (points.isNotEmpty()) {
                        var currentChunk = mutableListOf<com.figago.domain.model.LocationPoint>()
                        var isCurrentTransport = points.first().isTransport

                        for (i in 0 until points.size - 1) {
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val res = FloatArray(1)
                            android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, res)
                            val dist = res[0].toDouble()
                            val timeGap = p2.timestamp - p1.timestamp
                            
                            if (p2.isTransport) {
                                totalTransportDist += dist
                                totalTransportTime += timeGap
                            } else {
                                totalWalkDist += dist
                                totalWalkTime += timeGap
                            }
                        }

                        for (point in points) {
                            if (point.isTransport == isCurrentTransport) {
                                currentChunk.add(point)
                            } else {
                                // Save current chunk
                                pLines.add(
                                    com.figago.ui.components.TrackPolylineData(
                                        points = currentChunk.map { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) },
                                        color = if (isCurrentTransport) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color(0xFFFF9800)
                                    )
                                )
                                // Start new chunk, including the previous point to close the gap
                                val lastPoint = currentChunk.last()
                                currentChunk = mutableListOf(lastPoint, point)
                                isCurrentTransport = point.isTransport
                            }
                        }

                        // Add the last chunk
                        if (currentChunk.size >= 2) {
                            pLines.add(
                                com.figago.ui.components.TrackPolylineData(
                                    points = currentChunk.map { com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude) },
                                    color = if (isCurrentTransport) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color(0xFFFF9800)
                                )
                            )
                        }
                    }
                }
                
                // Update day stats with fresh calculated separated walk/transport values
                dayStatsValue = dayStatsValue.copy(
                    totalDistance = totalWalkDist + totalTransportDist,
                    totalMovingTimeMs = totalWalkTime + totalTransportTime,
                    walkDistance = totalWalkDist,
                    transportDistance = totalTransportDist,
                    walkTimeMs = totalWalkTime,
                    transportTimeMs = totalTransportTime
                )

                _polylines.value = pLines
            } else {
                _polylines.value = emptyList()
            }
            
            _dayStats.value = dayStatsValue
        }
    }

    /** Полностью удаляет сессию и все связанные с ней данные из БД. */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    /** Удаляет всю историю поездок. */
    fun deleteAllSessions() {
        viewModelScope.launch {
            sessionRepository.deleteAllSessions()
        }
    }
}
