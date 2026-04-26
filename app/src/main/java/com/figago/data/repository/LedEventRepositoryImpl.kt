package com.figago.data.repository

import com.figago.data.dao.LedEventDao
import com.figago.data.entity.LedEventEntity
import com.figago.data.mapper.toDomain
import com.figago.domain.model.LedEvent
import com.figago.domain.repository.LedEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация LedEventRepository.
 * Делегирует операции в LedEventDao.
 */
@Singleton
class LedEventRepositoryImpl @Inject constructor(
    private val ledEventDao: LedEventDao,
) : LedEventRepository {

    override suspend fun recordEvent(
        dayId: Long, ledCountRemaining: Int, distanceAtEvent: Double, timestamp: Long
    ): Long {

        val entity = LedEventEntity(
            dayId = dayId,
            ledCountRemaining = ledCountRemaining,
            distanceAtEvent = distanceAtEvent,
            timestamp = timestamp,
        )
        return ledEventDao.insert(entity)
    }

    override suspend fun getEventsByDayId(dayId: Long): List<LedEvent> {
        return ledEventDao.getEventsByDayId(dayId).map { it.toDomain() }
    }

    override fun observeEventsByDayId(dayId: Long): Flow<List<LedEvent>> {
        return ledEventDao.observeEventsByDayId(dayId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllEvents(): List<LedEvent> {
        return ledEventDao.getAllEvents().map { it.toDomain() }
    }

    override suspend fun getEventCountByDayId(dayId: Long): Int {
        return ledEventDao.getEventCountByDayId(dayId)
    }

    override suspend fun getLastEvent(dayId: Long): LedEvent? {
        return ledEventDao.getLastEvent(dayId)?.toDomain()
    }

    override suspend fun getCompletedSessionsWithEventsCount(): Int {
        return ledEventDao.getCompletedSessionsWithEventsCount()
    }

    /**
     * Расчёт среднего пробега на каждом уровне заряда.
     *
     * Алгоритм:
     * 1. Получаем все LED-события из завершённых сессий (отсортированные по сессии + времени).
     * 2. Группируем по dayId.
     * 3. Для каждой сессии вычисляем пробег на уровне N: distance[N-1] - distance[N].
     *    Уровень определяется как ledCountRemaining + 1 (т.к. событие фиксирует ПЕРЕХОД с уровня N на N-1).
     * 4. Усредняем по всем сессиям для каждого уровня.
     */
    override suspend fun getAverageDistancePerLevel(): Map<Int, Double> {

        val events = ledEventDao.getEventsFromCompletedSessions().map { it.toDomain() }
        if (events.isEmpty()) return emptyMap()

        // Группируем события по сессиям
        val bySession = events.groupBy { it.dayId }

        // Собираем пробеги по уровням: Map<level, List<distance>>
        val distancesByLevel = mutableMapOf<Int, MutableList<Double>>()

        for ((_, sessionEvents) in bySession) {

            // Предыдущая дистанция (начало сессии = 0)
            var prevDistance = 0.0

            for (event in sessionEvents) {

                // Уровень, С КОТОРОГО произошёл переход (ledCountRemaining — это куда перешли)
                val fromLevel = event.ledCountRemaining + 1
                val distanceOnLevel = event.distanceAtEvent - prevDistance

                if (distanceOnLevel > 0) {
                    distancesByLevel.getOrPut(fromLevel) { mutableListOf() }.add(distanceOnLevel)
                }

                prevDistance = event.distanceAtEvent
            }
        }

        // Усредняем
        return distancesByLevel.mapValues { (_, distances) -> distances.average() }
    }

    override suspend fun getAllSince(sinceTimestamp: Long): List<LedEvent> {
        return ledEventDao.getAllEvents()
            .map { it.toDomain() }
            .filter { it.timestamp >= sinceTimestamp }
    }
}
