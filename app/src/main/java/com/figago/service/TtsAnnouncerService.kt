package com.figago.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_DISTANCE
import com.figago.domain.repository.SettingsRepository.Companion.TTS_MODE_TIME
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис голосовых оповещений о пробеге и прогнозе остатка.
 *
 * Инициализирует Android TTS, формирует текст на русском языке,
 * запрашивает AudioFocus с ducking для корректной работы поверх музыки.
 *
 * Поддерживает два режима автоматических оповещений:
 * - Каждые N км пробега
 * - Каждые N минут записи
 *
 * Команда «Какой пробег» работает ВСЕГДА, независимо от режима.
 */
@Singleton
class TtsAnnouncerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val formatMetricsUseCase: com.figago.domain.usecase.FormatMetricsUseCase,
) {

    companion object {
        private const val TAG = "TtsAnnouncer"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    /** Последний километр, на котором произошло оповещение (для режима «по км»). */
    private var lastAnnouncedAtKm: Double = 0.0

    /** Последнее время оповещения в millis (для режима «по минутам»). */
    private var lastAnnouncedAtTimeMs: Long = 0L

    /** Время старта текущего трека (для «по минутам»). */
    private var trackStartTimeMs: Long = 0L

    /**
     * Инициализация TTS-движка. Вызывать при старте трека.
     */
    fun initialize() {

        if (tts != null) return

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        tts = TextToSpeech(context) { status ->

            if (status == TextToSpeech.SUCCESS) {

                val locale = Locale("ru", "RU")
                val result = tts?.setLanguage(locale)

                isInitialized = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED

                if (!isInitialized) {
                    Log.w(TAG, "Русский язык TTS не поддерживается на этом устройстве")
                }
            } else {
                Log.e(TAG, "Ошибка инициализации TTS: status=$status")
            }
        }
    }

    /**
     * Сбросить счётчики при старте нового трека.
     */
    fun resetCounters() {
        lastAnnouncedAtKm = 0.0
        lastAnnouncedAtTimeMs = System.currentTimeMillis()
        trackStartTimeMs = System.currentTimeMillis()
    }

    /**
     * Проверяет, пора ли озвучивать, и если да — озвучивает.
     *
     * Вызывается из TrackingService при каждом обновлении дистанции.
     *
     * @param currentDistanceM текущий суммарный пробег (метры)
     * @param forecastKm прогнозный остаток (км) или null
     * @param ttsMode режим оповещений ("off" / "distance" / "time")
     * @param distanceIntervalKm интервал в км (для режима distance)
     * @param timeIntervalMin интервал в минутах (для режима time)
     */
    suspend fun checkAndAnnounce(
        currentDistanceM: Double,
        forecastKm: Double?,
        ttsMode: String,
        distanceIntervalKm: Double,
        timeIntervalMin: Int,
    ) {

        if (!isInitialized) return

        val currentKm = currentDistanceM / 1000.0
        val now = System.currentTimeMillis()

        val shouldAnnounce = when (ttsMode) {

            TTS_MODE_DISTANCE -> {
                // Проверяем: прошли ли мы следующую отметку по км
                val nextMilestone = lastAnnouncedAtKm + distanceIntervalKm
                currentKm >= nextMilestone
            }

            TTS_MODE_TIME -> {
                // Проверяем: прошло ли достаточно времени
                val elapsedMin = (now - lastAnnouncedAtTimeMs) / 60_000.0
                elapsedMin >= timeIntervalMin
            }

            else -> false
        }

        if (shouldAnnounce) {

            lastAnnouncedAtKm = currentKm
            lastAnnouncedAtTimeMs = now
            speak(buildAnnouncementText(currentDistanceM, forecastKm))
        }
    }

    /**
     * Принудительное озвучивание (команда «какой пробег»).
     * Работает ВСЕГДА, независимо от настроек автоматических оповещений.
     *
     * @param currentDistanceM текущий суммарный пробег (метры)
     * @param forecastKm прогнозный остаток (км) или null
     */
    suspend fun announceNow(currentDistanceM: Double, forecastKm: Double?) {

        if (!isInitialized) {
            Log.w(TAG, "TTS не инициализирован, пропускаем озвучку")
            return
        }

        speak(buildAnnouncementText(currentDistanceM, forecastKm))
    }

    /**
     * Озвучивает сообщение «Сессия не начата».
     */
    fun announceNoSession() {

        if (!isInitialized) return

        speak("Сессия не начата")
    }

    /**
     * Озвучивает сообщение о Точке Невозврата.
     */
    fun announcePointOfNoReturn(kmRemaining: Int, currentDistanceKm: Int) {
        if (!isInitialized) return
        speak("Точка невозврата через $kmRemaining километров. Пробег $currentDistanceKm километров.")
    }

    /**
     * Формирует текст озвучки.
     */
    private suspend fun buildAnnouncementText(distanceM: Double, forecastKm: Double?): String {
        val formatResult = formatMetricsUseCase(distanceM, 0.0)
        val distanceText = formatResult.formattedDistanceString
        val sb = StringBuilder("Пробег $distanceText")

        if (forecastKm != null && forecastKm > 0) {
            val forecastResult = formatMetricsUseCase(forecastKm * 1000.0, 0.0)
            val forecastText = forecastResult.formattedDistanceString
            sb.append(". Осталось примерно $forecastText")
        }

        return sb.toString()
    }

    /**
     * Озвучивает текст через TTS с AudioFocus ducking.
     */
    private fun speak(text: String) {

        val engine = tts ?: return

        // Запрашиваем AudioFocus с ducking (приглушение музыки)
        requestAudioFocus()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { abandonAudioFocus() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { abandonAudioFocus() }
        })

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "figago_announce")
        Log.i(TAG, "TTS: $text")
    }

    /**
     * Запрос AudioFocus с ducking (приглушение фоновой музыки на время озвучки).
     */
    private fun requestAudioFocus() {

        val am = audioManager ?: return

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()

        am.requestAudioFocus(focusRequest!!)
    }

    /**
     * Освобождение AudioFocus после завершения озвучки.
     */
    private fun abandonAudioFocus() {

        val am = audioManager ?: return

        focusRequest?.let { am.abandonAudioFocusRequest(it) }
    }

    /**
     * Освобождение ресурсов TTS. Вызывать при закрытии сервиса.
     */
    fun shutdown() {

        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        abandonAudioFocus()
    }
}
