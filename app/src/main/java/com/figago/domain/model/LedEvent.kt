package com.figago.domain.model

/**
 * Доменная модель LED-события (фиксация разряда батареи).
 */
data class LedEvent(
    val id: Long = 0,
    val dayId: Long,
    val ledCountRemaining: Int,
    val distanceAtEvent: Double,
    val timestamp: Long,
)
