package com.figago.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.figago.domain.repository.LampStatisticsRepository
import com.figago.domain.repository.LedEventRepository
import com.figago.domain.repository.ProfileRepository
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.TrackRepository
import com.figago.service.ErrorLogCollector
import com.figago.ui.settings.TelemetryCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Формирование ZIP-архива телеметрии по выбранным категориям и отправка по email.
 *
 * Включает:
 * - device_info.json — информация об устройстве (всегда)
 * - settings.json — полный снапшот настроек (всегда)
 * - led_events.json, lamp_statistics.json — данные батареи
 * - sessions.json, tracks.json — история поездок
 * - errors.jsonl — журнал ошибок
 * - locations.json — GPS-координаты (опционально)
 *
 * Данные ограничиваются последними 30 днями.
 */
class TelemetryExportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionRepository: SessionRepository,
    private val trackRepository: TrackRepository,
    private val ledEventRepository: LedEventRepository,
    private val lampStatisticsRepository: LampStatisticsRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val eventLogDao: com.figago.data.dao.EventLogDao,
) {

    companion object {
        private const val DAYS_LIMIT = 30
        private const val EMAIL = "sumaneev@gmail.com"
    }

    /**
     * Формирует ZIP и открывает email-клиент.
     *
     * @param categories набор выбранных категорий из TelemetryCategory
     * @return Intent для отправки email (caller должен startActivity)
     */
    suspend fun export(categories: Set<String>): Intent = withContext(Dispatchers.IO) {
        val sinceTimestamp = System.currentTimeMillis() - DAYS_LIMIT * 24L * 60 * 60 * 1000
        val zipFile = File(context.cacheDir, "figago_telemetry.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->

            // 1. device_info.json — всегда
            val deviceInfo = JSONObject().apply {
                put("deviceModel", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("androidVersion", Build.VERSION.RELEASE)
                put("sdkVersion", Build.VERSION.SDK_INT)
                put("appVersionName", com.figago.BuildConfig.VERSION_NAME)
                put("appVersionCode", com.figago.BuildConfig.VERSION_CODE)
                put("exportTimestamp", System.currentTimeMillis())
            }
            addJsonToZip(zip, "device_info.json", deviceInfo.toString(2))

            // 2. settings.json — всегда
            val profileId = settingsRepository.getActiveProfileId()
            val profile = profileRepository.getActiveProfile().first()
            val snapshot = settingsRepository.getSettingsForProfile(profileId)
            val settingsJson = JSONObject().apply {
                put("profile", JSONObject().apply {
                    put("id", profile?.id)
                    put("name", profile?.name)
                    put("type", profile?.type?.name)
                    put("maxSpeed", profile?.maxSpeed)
                    put("maxMileage", profile?.maxMileage)
                    put("ledCount", profile?.ledCount)
                    put("ledDistances", profile?.ledDistances?.let { JSONArray(it) })
                })
                put("tts", JSONObject().apply {
                    put("mode", snapshot.ttsMode)
                    put("distanceIntervalKm", snapshot.ttsDistanceIntervalKm)
                    put("timeIntervalMin", snapshot.ttsTimeIntervalMin)
                })
                put("ponr", JSONObject().apply {
                    put("warningKm", snapshot.pointOfNoReturnWarningKm)
                })
                put("general", JSONObject().apply {
                    put("unitSystem", snapshot.unitSystem)
                    put("autoTransportDetection", snapshot.autoTransportDetectionEnabled)
                    put("useStatistics", snapshot.useStatistics)
                    put("appLanguage", snapshot.appLanguageCode)
                    put("gpsIntervalSec", snapshot.gpsIntervalSec)
                    put("autoPauseSpeedLimitKmH", snapshot.autoPauseSpeedLimitKmH)
                    put("autoCloseTime", snapshot.autoCloseTime)
                })
            }
            addJsonToZip(zip, "settings.json", settingsJson.toString(2))

            // 3. Батарея
            if (TelemetryCategory.BATTERY_DATA in categories) {
                val ledEvents = ledEventRepository.getAllSince(sinceTimestamp)
                val ledArray = JSONArray()
                ledEvents.forEach { e ->
                    ledArray.put(JSONObject().apply {
                        put("dayId", e.dayId)
                        put("ledCountRemaining", e.ledCountRemaining)
                        put("distanceAtEvent", e.distanceAtEvent)
                        put("timestamp", e.timestamp)
                    })
                }
                addJsonToZip(zip, "led_events.json", ledArray.toString(2))

                val stats = lampStatisticsRepository.getAllSince(sinceTimestamp)
                val statsArray = JSONArray()
                stats.forEach { s ->
                    statsArray.put(JSONObject().apply {
                        put("profileId", s.profileId)
                        put("lampIndex", s.lampIndex)
                        put("actualDistance", s.actualDistance)
                        put("timestamp", s.timestamp)
                    })
                }
                addJsonToZip(zip, "lamp_statistics.json", statsArray.toString(2))
            }

            // 4. История поездок
            if (TelemetryCategory.TRIP_HISTORY in categories) {
                val sessions = sessionRepository.getAllSessionsSince(sinceTimestamp)
                val sessionsArray = JSONArray()
                sessions.forEach { s ->
                    sessionsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("totalDistance", s.totalDistance)
                        put("date", s.date)
                        put("isActive", s.isActive)
                        put("profileId", s.profileId)
                    })
                }
                addJsonToZip(zip, "sessions.json", sessionsArray.toString(2))

                val tracks = trackRepository.getAllTracksSince(sinceTimestamp)
                val tracksArray = JSONArray()
                tracks.forEach { t ->
                    tracksArray.put(JSONObject().apply {
                        put("id", t.id)
                        put("dayId", t.dayId)
                        put("segmentDistance", t.segmentDistance)
                        put("startTime", t.startTime)
                        put("endTime", t.endTime)
                    })
                }
                addJsonToZip(zip, "tracks.json", tracksArray.toString(2))
            }

            // 5. Журнал ошибок
            if (TelemetryCategory.ERROR_LOG in categories) {
                val errorsFile = ErrorLogCollector(context).getErrorsFile()
                if (errorsFile.exists()) {
                    zip.putNextEntry(ZipEntry("errors.jsonl"))
                    errorsFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                
                // Добавляем системные события телеметрии
                val events = eventLogDao.getAllEvents()
                val eventsArray = JSONArray()
                events.forEach { e ->
                    eventsArray.put(JSONObject().apply {
                        put("id", e.id)
                        put("dayId", e.dayId)
                        put("timestamp", e.timestamp)
                        put("eventType", e.eventType)
                        put("context", e.context)
                    })
                }
                addJsonToZip(zip, "events.json", eventsArray.toString(2))
            }

            // 6. GPS-координаты
            if (TelemetryCategory.GPS_COORDS in categories) {
                val locations = trackRepository.getAllLocationsSince(sinceTimestamp)
                val locArray = JSONArray()
                locations.forEach { l ->
                    locArray.put(JSONObject().apply {
                        put("segmentId", l.segmentId)
                        put("latitude", l.latitude)
                        put("longitude", l.longitude)
                        put("timestamp", l.timestamp)
                        put("isTransport", l.isTransport)
                    })
                }
                addJsonToZip(zip, "locations.json", locArray.toString(2))
            }
        }

        // Создаём Intent для отправки email
        val versionName = com.figago.BuildConfig.VERSION_NAME
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)

        Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "FigaGo Telemetry v$versionName")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun addJsonToZip(zip: ZipOutputStream, filename: String, content: String) {
        zip.putNextEntry(ZipEntry(filename))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
