package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Событие падения напряжения батареи (погасание индикатора).
 *
 * Фиксируется голосовой командой или кнопкой «Лампочка погасла».
 * Хранит оставшееся количество индикаторов и пройденную дистанцию
 * на момент события для последующей аналитики расхода батареи.
 *
 * @property id                Уникальный идентификатор события (автоинкремент).
 * @property dayId             FK → DaySession.id — ссылка на суточную сессию.
 * @property ledCountRemaining Количество оставшихся индикаторов (4, 3, 2, 1, 0).
 * @property distanceAtEvent   Суммарная дистанция с начала дня на момент события (метры).
 * @property timestamp         Временная метка фиксации (epoch millis).
 */
@Entity(
    tableName = "led_event",
    foreignKeys = [
        ForeignKey(
            entity = DaySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["day_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("day_id")],
)
data class LedEventEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "day_id")
    val dayId: Long,

    @ColumnInfo(name = "led_count_remaining")
    val ledCountRemaining: Int,

    @ColumnInfo(name = "distance_at_event")
    val distanceAtEvent: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
)
