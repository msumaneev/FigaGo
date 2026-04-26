package com.figago.domain.model

/**
 * Доменная модель отрезка пути.
 */
data class TrackSegment(
    val id: Long = 0,
    val dayId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val segmentDistance: Double = 0.0,
)
