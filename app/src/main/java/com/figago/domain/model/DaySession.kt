package com.figago.domain.model

/**
 * Доменная модель суточной сессии трекинга.
 * Не зависит от Room — чистый data-класс для бизнес-логики и UI.
 */
data class DaySession(
    val id: Long = 0,
    val date: String,
    val totalDistance: Double = 0.0,
    val isActive: Boolean = true,
    val profileId: Long? = null
)
