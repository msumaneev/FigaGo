package com.figago.data.repository

import com.figago.data.dao.LocationPointDao
import com.figago.data.dao.TrackSegmentDao
import com.figago.data.entity.LocationPointEntity
import com.figago.data.entity.TrackSegmentEntity
import com.figago.data.mapper.toDomain
import com.figago.domain.model.LocationPoint
import com.figago.domain.model.TrackSegment
import com.figago.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация TrackRepository.
 * Управляет отрезками пути и GPS-точками через DAO.
 */
@Singleton
class TrackRepositoryImpl @Inject constructor(
    private val segmentDao: TrackSegmentDao,
    private val pointDao: LocationPointDao,
) : TrackRepository {

    override suspend fun createSegment(dayId: Long, startTime: Long, profileId: Long): Long {
        val entity = TrackSegmentEntity(dayId = dayId, startTime = startTime, profileId = profileId)
        return segmentDao.insert(entity)
    }

    override suspend fun finishSegment(segmentId: Long, endTime: Long, distance: Double) {
        segmentDao.finishSegment(segmentId, endTime, distance)
    }

    override suspend fun updateSegmentDistance(segmentId: Long, distance: Double) {
        segmentDao.updateDistance(segmentId, distance)
    }

    override suspend fun getActiveSegment(dayId: Long): TrackSegment? {
        return segmentDao.getActiveSegment(dayId)?.toDomain()
    }

    override suspend fun getSegmentById(segmentId: Long): TrackSegment? {
        return segmentDao.getById(segmentId)?.toDomain()
    }

    override suspend fun getSegmentsByDayId(dayId: Long): List<TrackSegment> {
        return segmentDao.getSegmentsByDayId(dayId).map { it.toDomain() }
    }

    override fun observeSegmentsByDayId(dayId: Long): Flow<List<TrackSegment>> {
        return segmentDao.observeSegmentsByDayId(dayId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addLocationPoint(
        segmentId: Long, latitude: Double, longitude: Double, timestamp: Long, isTransport: Boolean
    ): Long {

        val entity = LocationPointEntity(
            segmentId = segmentId,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            isTransport = isTransport,
        )
        return pointDao.insert(entity)
    }

    override suspend fun addLocationPoints(points: List<LocationPoint>) {
        val entities = points.map { 
            LocationPointEntity(
                segmentId = it.segmentId,
                latitude = it.latitude,
                longitude = it.longitude,
                timestamp = it.timestamp,
                isTransport = it.isTransport,
            )
        }
        pointDao.insertAll(entities)
    }

    override suspend fun getLastPoint(segmentId: Long): LocationPoint? {
        return pointDao.getLastPoint(segmentId)?.toDomain()
    }

    override suspend fun getPointsBySegmentId(segmentId: Long): List<LocationPoint> {
        return pointDao.getPointsBySegmentId(segmentId).map { it.toDomain() }
    }

    override fun observePointsBySegmentId(segmentId: Long): Flow<List<LocationPoint>> {
        return pointDao.observePointsBySegmentId(segmentId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteSegment(segmentId: Long) {
        segmentDao.deleteSegment(segmentId)
    }

    override suspend fun getAllTracksSince(sinceTimestamp: Long): List<TrackSegment> {
        return segmentDao.getAllSegments()
            .map { it.toDomain() }
            .filter { it.startTime >= sinceTimestamp }
    }

    override suspend fun getAllLocationsSince(sinceTimestamp: Long): List<LocationPoint> {
        return pointDao.getAllPointsSince(sinceTimestamp)
            .map { it.toDomain() }
    }
}
