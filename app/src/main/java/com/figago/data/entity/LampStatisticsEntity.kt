package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * История фактического пробега по каждой лампочке.
 *
 * Используется для статистического прогнозирования остатка хода.
 * Для каждого [lampIndex] хранится скользящее окно записей (HISTORY_LIMIT = 5).
 *
 * @property id             Уникальный идентификатор записи.
 * @property profileId      ID профиля коляски (FK → profile.id).
 * @property lampIndex      Индекс лампочки (0 = первая тухнущая, N = последняя).
 * @property actualDistance  Фактический пробег на этой лампочке (км).
 * @property timestamp      Временная метка записи (millis).
 */
@Entity(tableName = "lamp_statistics")
data class LampStatisticsEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: Long,

    @ColumnInfo(name = "lamp_index")
    val lampIndex: Int,

    @ColumnInfo(name = "actual_distance")
    val actualDistance: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
