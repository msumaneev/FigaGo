package com.figago.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Состояние трекинга для UI в реальном времени.
 */
data class TrackingState(
    val isRecording: Boolean = false,
    val totalDistanceMeters: Double = 0.0,
    val instantSpeedKmh: Double = 0.0,
    val segmentAverageSpeedKmh: Double = 0.0,
    val isTransport: Boolean = false,
    val isManualTransportActive: Boolean = false,
)

/**
 * Синглтон для связи TrackingService и UI (Dashboard).
 * Позволяет избегать лишних чтений из БД для отображения спидометра
 * и текущих метрик.
 */
@Singleton
class TrackingEngine @Inject constructor() {
    private val _trackingState = MutableStateFlow(TrackingState())
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    fun updateState(updater: (TrackingState) -> TrackingState) {
        _trackingState.value = updater(_trackingState.value)
    }

    fun setManualTransportActive(isActive: Boolean) {
        _trackingState.value = _trackingState.value.copy(isManualTransportActive = isActive)
    }

    fun reset() {
        _trackingState.value = TrackingState()
    }
}
