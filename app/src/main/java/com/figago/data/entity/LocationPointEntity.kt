package com.figago.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * GPS-точка маршрута.
 *
 * Каждая зафиксированная FusedLocationProviderClient координата
 * сохраняется как отдельная LocationPointEntity.
 *
 * @property id        Уникальный идентификатор точки (автоинкремент).
 * @property segmentId FK → TrackSegment.id — ссылка на родительский отрезок.
 * @property latitude  Широта (WGS-84).
 * @property longitude Долгота (WGS-84).
 * @property timestamp Временная метка фиксации (epoch millis).
 */
@Entity(
    tableName = "location_point",
    foreignKeys = [
        ForeignKey(
            entity = TrackSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segment_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("segment_id")],
)
data class LocationPointEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "segment_id")
    val segmentId: Long,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "is_transport")
    val isTransport: Boolean = false,
)
