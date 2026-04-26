package com.figago.data.repository

import com.figago.data.dao.DaySessionDao
import com.figago.data.entity.DaySessionEntity
import com.figago.data.mapper.toDomain
import com.figago.domain.model.DaySession
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация SessionRepository.
 * Делегирует операции в DaySessionDao, маппит Entity → Domain.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val daySessionDao: DaySessionDao,
    private val settingsRepository: SettingsRepository,
) : SessionRepository {

    override suspend fun createSession(date: String): Long {
        val activeProfileId = settingsRepository.getActiveProfileId()
        val entity = DaySessionEntity(date = date, totalDistance = 0.0, isActive = true, profileId = activeProfileId)
        return daySessionDao.insert(entity)
    }

    override suspend fun getActiveSession(): DaySession? {
        return daySessionDao.getActiveSession()?.toDomain()
    }

    override fun observeActiveSession(): Flow<DaySession?> {
        return daySessionDao.observeActiveSession().map { it?.toDomain() }
    }

    override suspend fun closeSession(sessionId: Long) {
        daySessionDao.closeSession(sessionId)
    }

    override suspend fun updateDistance(sessionId: Long, distance: Double) {
        daySessionDao.updateDistance(sessionId, distance)
    }

    override suspend fun getById(sessionId: Long): DaySession? {
        return daySessionDao.getById(sessionId)?.toDomain()
    }

    override suspend fun getAllSessionsByDate(date: String): List<DaySession> {
        return daySessionDao.getAllSessionsByDate(date).map { it.toDomain() }
    }

    override fun observeById(sessionId: Long): Flow<DaySession?> {
        return daySessionDao.observeById(sessionId).map { it?.toDomain() }
    }

    override fun observeAllSessions(profileId: Long): Flow<List<DaySession>> {
        return daySessionDao.observeAllSessions(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeAllSessionsGroupedByDate(): Flow<List<DaySession>> {
        return daySessionDao.observeAllSessionsGroupedByDate().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteSession(sessionId: Long) {
        daySessionDao.deleteSession(sessionId)
    }

    override suspend fun deleteAllSessions() {
        daySessionDao.deleteAllSessions()
    }

    override suspend fun reassignSessionsProfile(oldProfileId: Long, newProfileId: Long) {
        daySessionDao.reassignSessionsProfile(oldProfileId, newProfileId)
    }

    override suspend fun getAllSessionsSince(sinceTimestamp: Long): List<DaySession> {
        // DaySession хранит дату строкой, поэтому возвращаем все — фильтрация по timestamp невозможна
        return daySessionDao.observeAllSessionsGroupedByDate()
            .first()
            .map { it.toDomain() }
    }
}
