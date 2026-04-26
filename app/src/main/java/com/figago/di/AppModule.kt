package com.figago.di

import android.content.Context
import androidx.room.Room
import com.figago.data.db.AppDatabase
import com.figago.data.dao.DaySessionDao
import com.figago.data.dao.TrackSegmentDao
import com.figago.data.dao.LocationPointDao
import com.figago.data.dao.LedEventDao
import com.figago.data.dao.ProfileDao
import com.figago.data.dao.LampStatisticsDao
import dagger.Module
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-модуль приложения.
 * Предоставляет синглтоны: Room Database и все DAO.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `profile` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `icon_id` INTEGER NOT NULL, `max_speed` REAL NOT NULL, `max_mileage` REAL, `led_count` INTEGER, `led_distances` TEXT)")
            database.execSQL("INSERT INTO `profile` (`id`, `type`, `name`, `icon_id`, `max_speed`, `max_mileage`, `led_count`, `led_distances`) VALUES (1, 'ELECTRIC', 'Моя коляска', 0, 10.0, 20.0, 5, NULL)")
            database.execSQL("ALTER TABLE `location_point` ADD COLUMN `is_transport` INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE `track_segment` ADD COLUMN `profile_id` INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `day_session` ADD COLUMN `profile_id` INTEGER DEFAULT NULL")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `lamp_statistics` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`profile_id` INTEGER NOT NULL, " +
                "`lamp_index` INTEGER NOT NULL, " +
                "`actual_distance` REAL NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
            )
        }
    }

    /** Единственный экземпляр Room-базы данных на весь жизненный цикл приложения. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "figago_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .addCallback(object : androidx.room.RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Засев дефолтного профиля при первом создании БД (fresh install)
                db.execSQL(
                    "INSERT INTO `profile` (`id`, `type`, `name`, `icon_id`, `max_speed`, `max_mileage`, `led_count`, `led_distances`) " +
                    "VALUES (1, 'ELECTRIC', 'Моя коляска', 0, 10.0, 20.0, 5, NULL)"
                )
            }
        })
        .build()
    }

    @Provides
    fun provideDaySessionDao(db: AppDatabase): DaySessionDao = db.daySessionDao()

    @Provides
    fun provideTrackSegmentDao(db: AppDatabase): TrackSegmentDao = db.trackSegmentDao()

    @Provides
    fun provideLocationPointDao(db: AppDatabase): LocationPointDao = db.locationPointDao()

    @Provides
    fun provideLedEventDao(db: AppDatabase): LedEventDao = db.ledEventDao()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideLampStatisticsDao(db: AppDatabase): LampStatisticsDao = db.lampStatisticsDao()
}
