package com.figago.data.mapper

import com.figago.data.entity.DaySessionEntity
import com.figago.data.entity.LedEventEntity
import com.figago.data.entity.LocationPointEntity
import com.figago.data.entity.TrackSegmentEntity
import com.figago.domain.model.DaySession
import com.figago.domain.model.LedEvent
import com.figago.domain.model.LocationPoint
import com.figago.domain.model.TrackSegment

/**
 * Функции маппинга между Room-сущностями и доменными моделями.
 * Обеспечивают изоляцию слоя data от domain.
 */

// ===== DaySession =====

fun DaySessionEntity.toDomain() = DaySession(
    id = id,
    date = date,
    totalDistance = totalDistance,
    isActive = isActive,
    profileId = profileId,
)

fun DaySession.toEntity() = DaySessionEntity(
    id = id,
    date = date,
    totalDistance = totalDistance,
    isActive = isActive,
    profileId = profileId,
)

// ===== TrackSegment =====

fun TrackSegmentEntity.toDomain() = TrackSegment(
    id = id,
    dayId = dayId,
    startTime = startTime,
    endTime = endTime,
    segmentDistance = segmentDistance,
)

fun TrackSegment.toEntity() = TrackSegmentEntity(
    id = id,
    dayId = dayId,
    startTime = startTime,
    endTime = endTime,
    segmentDistance = segmentDistance,
)

// ===== LocationPoint =====

fun LocationPointEntity.toDomain() = LocationPoint(
    id = id,
    segmentId = segmentId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isTransport = isTransport,
)

fun LocationPoint.toEntity() = LocationPointEntity(
    id = id,
    segmentId = segmentId,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp,
    isTransport = isTransport,
)

// ===== LedEvent =====

fun LedEventEntity.toDomain() = LedEvent(
    id = id,
    dayId = dayId,
    ledCountRemaining = ledCountRemaining,
    distanceAtEvent = distanceAtEvent,
    timestamp = timestamp,
)

fun LedEvent.toEntity() = LedEventEntity(
    id = id,
    dayId = dayId,
    ledCountRemaining = ledCountRemaining,
    distanceAtEvent = distanceAtEvent,
    timestamp = timestamp,
)
