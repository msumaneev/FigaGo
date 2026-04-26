package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Отрезок пути внутри суточной сессии.
 *
 * Каждое нажатие «Старт» создаёт новый TrackSegment,
 * «Стоп» фиксирует end_time и segment_distance.
 *
 * @property id              Уникальный идентификатор отрезка (автоинкремент).
 * @property dayId           FK → DaySession.id — ссылка на родительскую сессию.
 * @property startTime       Время начала записи отрезка (epoch millis).
 * @property endTime         Время окончания записи (null — отрезок ещё записывается).
 * @property segmentDistance Дистанция отрезка в метрах.
 */
@Entity(
    tableName = "track_segment",
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
data class TrackSegmentEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "day_id")
    val dayId: Long,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,

    @ColumnInfo(name = "segment_distance")
    val segmentDistance: Double = 0.0,

    @ColumnInfo(name = "profile_id", defaultValue = "1")
    val profileId: Long = 1L,
)
