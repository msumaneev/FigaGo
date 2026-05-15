package com.figago.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.figago.data.dao.DaySessionDao
import com.figago.data.dao.LedEventDao
import com.figago.data.dao.LocationPointDao
import com.figago.data.dao.LampStatisticsDao
import com.figago.data.dao.ProfileDao
import com.figago.data.dao.TrackSegmentDao
import com.figago.data.entity.DaySessionEntity
import com.figago.data.entity.LedEventEntity
import com.figago.data.entity.LocationPointEntity
import com.figago.data.entity.LampStatisticsEntity
import com.figago.data.entity.ProfileEntity
import com.figago.data.entity.TrackSegmentEntity
import androidx.room.TypeConverters

/**
 * Главная Room-база данных приложения FigaGo.
 *
 * Содержит 6 таблиц:
 * - day_session       — суточные сессии трекинга
 * - track_segment     — отрезки пути внутри дня
 * - location_point    — GPS-координаты маршрута
 * - led_event         — события разряда батареи
 * - profile           — профили колясок
 * - lamp_statistics   — статистика пробега по лампочкам
 *
 * Версия 4 — добавлена таблица lamp_statistics.
 */
@Database(
    entities = [
        DaySessionEntity::class,
        TrackSegmentEntity::class,
        LocationPointEntity::class,
        LedEventEntity::class,
        ProfileEntity::class,
        LampStatisticsEntity::class,
        com.figago.data.entity.EventLogEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun daySessionDao(): DaySessionDao
    abstract fun trackSegmentDao(): TrackSegmentDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun ledEventDao(): LedEventDao
    abstract fun profileDao(): ProfileDao
    abstract fun lampStatisticsDao(): LampStatisticsDao
    abstract fun eventLogDao(): com.figago.data.dao.EventLogDao
}
