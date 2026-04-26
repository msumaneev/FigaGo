package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Суточная сессия трекинга.
 *
 * Каждый «рабочий день» = одна запись DaySessionEntity.
 * Содержит суммарную дистанцию и флаг активности.
 *
 * @property id           Уникальный идентификатор сессии (автоинкремент).
 * @property date         Дата сессии в формате ISO-8601 (yyyy-MM-dd).
 * @property totalDistance Суммарная дистанция за день в метрах.
 * @property isActive     Флаг: true — сессия активна, false — закрыта.
 */
@Entity(tableName = "day_session")
data class DaySessionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "total_distance")
    val totalDistance: Double = 0.0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "profile_id")
    val profileId: Long? = null
)
