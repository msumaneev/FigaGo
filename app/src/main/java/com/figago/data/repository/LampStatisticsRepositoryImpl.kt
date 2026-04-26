package com.figago.data.repository

import com.figago.data.dao.LampStatisticsDao
import com.figago.data.entity.LampStatisticsEntity
import com.figago.domain.repository.LampStatisticsRepository
import com.figago.domain.repository.LampStatisticsRepository.Companion.HISTORY_LIMIT
import javax.inject.Inject

/**
 * Реализация репозитория статистики лампочек.
 *
 * Обеспечивает скользящее окно: при вставке проверяет количество записей
 * для данного lampIndex и удаляет самую старую при превышении HISTORY_LIMIT.
 */
class LampStatisticsRepositoryImpl @Inject constructor(
    private val dao: LampStatisticsDao
) : LampStatisticsRepository {

    override suspend fun recordDistance(profileId: Long, lampIndex: Int, distanceKm: Float) {
        // Вставляем новую запись
        dao.insert(
            LampStatisticsEntity(
                profileId = profileId,
                lampIndex = lampIndex,
                actualDistance = distanceKm,
                timestamp = System.currentTimeMillis()
            )
        )

        // Проверяем лимит и удаляем старые
        val count = dao.getCountByLampIndex(profileId, lampIndex)
        if (count > HISTORY_LIMIT) {
            dao.deleteOldest(profileId, lampIndex)
        }
    }

    override suspend fun getAverageDistance(profileId: Long, lampIndex: Int): Float? {
        val count = dao.getCountByLampIndex(profileId, lampIndex)
        if (count == 0) return null
        return dao.getAverageByLampIndex(profileId, lampIndex)
    }

    override suspend fun getAveragesForProfile(profileId: Long): Map<Int, Float> {
        return dao.getAveragesForProfile(profileId)
            .associate { it.lamp_index to it.avg_dist }
    }

    override suspend fun getAllSince(sinceTimestamp: Long): List<LampStatisticsEntity> {
        return dao.getAllSince(sinceTimestamp)
    }
}
