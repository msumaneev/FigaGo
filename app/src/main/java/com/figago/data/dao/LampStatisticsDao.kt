package com.figago.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.figago.data.entity.LampStatisticsEntity

/**
 * DAO для таблицы lamp_statistics.
 *
 * Поддерживает скользящее окно записей (HISTORY_LIMIT) для каждого lamp_index
 * в рамках одного profile_id.
 */
@Dao
interface LampStatisticsDao {

    @Insert
    suspend fun insert(entity: LampStatisticsEntity)

    /**
     * Получить все записи для конкретной лампочки и профиля.
     */
    @Query("SELECT * FROM lamp_statistics WHERE profile_id = :profileId AND lamp_index = :lampIndex ORDER BY timestamp DESC")
    suspend fun getByLampIndex(profileId: Long, lampIndex: Int): List<LampStatisticsEntity>

    /**
     * Получить среднее расстояние для конкретной лампочки.
     */
    @Query("SELECT AVG(actual_distance) FROM lamp_statistics WHERE profile_id = :profileId AND lamp_index = :lampIndex")
    suspend fun getAverageByLampIndex(profileId: Long, lampIndex: Int): Float?

    /**
     * Получить количество записей для конкретной лампочки.
     */
    @Query("SELECT COUNT(*) FROM lamp_statistics WHERE profile_id = :profileId AND lamp_index = :lampIndex")
    suspend fun getCountByLampIndex(profileId: Long, lampIndex: Int): Int

    /**
     * Удалить самую старую запись для конкретной лампочки.
     */
    @Query("DELETE FROM lamp_statistics WHERE id = (SELECT id FROM lamp_statistics WHERE profile_id = :profileId AND lamp_index = :lampIndex ORDER BY timestamp ASC LIMIT 1)")
    suspend fun deleteOldest(profileId: Long, lampIndex: Int)

    /**
     * Получить все записи для профиля (для экспорта телеметрии).
     */
    @Query("SELECT * FROM lamp_statistics WHERE profile_id = :profileId ORDER BY lamp_index, timestamp DESC")
    suspend fun getAllByProfile(profileId: Long): List<LampStatisticsEntity>

    /**
     * Получить средние по всем лампочкам профиля (Map<lampIndex, avgDistance>).
     */
    @Query("SELECT lamp_index, AVG(actual_distance) as avg_dist FROM lamp_statistics WHERE profile_id = :profileId GROUP BY lamp_index")
    suspend fun getAveragesForProfile(profileId: Long): List<LampAverage>

    /**
     * Получить все записи для экспорта (за последние N дней).
     */
    @Query("SELECT * FROM lamp_statistics WHERE timestamp >= :sinceTimestamp ORDER BY lamp_index, timestamp DESC")
    suspend fun getAllSince(sinceTimestamp: Long): List<LampStatisticsEntity>
}

/**
 * Результат группировки средних по лампочкам.
 */
data class LampAverage(
    val lamp_index: Int,
    val avg_dist: Float
)
