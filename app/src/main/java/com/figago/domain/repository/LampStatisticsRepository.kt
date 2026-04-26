package com.figago.domain.repository

/**
 * Репозиторий статистики пробега по лампочкам.
 *
 * Обеспечивает скользящее окно записей (HISTORY_LIMIT) для каждого lamp_index.
 */
interface LampStatisticsRepository {

    companion object {
        /** Максимальное количество записей на одну лампочку. */
        const val HISTORY_LIMIT = 5
    }

    /**
     * Записать факт пробега по лампочке с автоматическим удалением старых записей.
     */
    suspend fun recordDistance(profileId: Long, lampIndex: Int, distanceKm: Float)

    /**
     * Получить средний пробег для конкретной лампочки.
     * @return средний пробег (км) или null если записей нет.
     */
    suspend fun getAverageDistance(profileId: Long, lampIndex: Int): Float?

    /**
     * Получить средние для всех лампочек профиля.
     * @return Map<lampIndex, avgDistanceKm>
     */
    suspend fun getAveragesForProfile(profileId: Long): Map<Int, Float>

    /**
     * Получить все записи для экспорта (за последние N дней).
     */
    suspend fun getAllSince(sinceTimestamp: Long): List<com.figago.data.entity.LampStatisticsEntity>
}
