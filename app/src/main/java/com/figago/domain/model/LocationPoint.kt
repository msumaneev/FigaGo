package com.figago.domain.model

/**
 * Доменная модель GPS-точки маршрута.
 */
data class LocationPoint(
    val id: Long = 0,
    val segmentId: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val isTransport: Boolean = false,
)
