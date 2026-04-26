package com.figago.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.figago.service.TrackingService
import com.figago.ui.navigation.FigaGoNavHost
import com.figago.ui.permissions.RequestPermissionsEffect
import com.figago.ui.theme.FigaGoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная Activity приложения FigaGo.
 *
 * Служит единственным контейнером для Jetpack Compose UI.
 * При старте запрашивает runtime-разрешения (GPS, уведомления).
 * Обрабатывает Deep Link-и для голосовых команд (Google Assistant Routines).
 *
 * Deep Link схема: figago://command/{command}
 * - figago://command/start_track  → «Я поехал» (StartDay + StartTrack)
 * - figago://command/stop_track   → «Я остановился» (StopTrack)
 * - figago://command/led_event    → «Лампочка» (погасить индикатор)
 * - figago://command/announce_status → «Какой пробег» (TTS-озвучка)
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Обработка deep link при холодном старте
        handleDeepLink(intent)

        // Проверяем, куда мы должны открыть интерфейс
        val startDestination = if (intent?.data?.lastPathSegment in listOf("start_track", "stop_track", "led_event")) {
            com.figago.ui.navigation.Routes.DASHBOARD
        } else {
            com.figago.ui.navigation.Routes.MAP
        }

        setContent {
            FigaGoTheme {
                // Запрос разрешений при первом запуске
                RequestPermissionsEffect()

                // Основная навигация
                FigaGoNavHost(startDestination = startDestination)
            }
        }
    }

    /**
     * Обработка нового Intent (при singleTask launchMode).
     * Вызывается при повторном открытии через deep link, когда Activity уже запущена.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Обновляем интент для Compose, чтобы он мог перестроиться
        handleDeepLink(intent)
    }

    /**
     * Маршрутизация deep link-а в TrackingService.
     *
     * Парсит последний сегмент пути из URI и отправляет
     * соответствующий Intent.action в foreground service.
     */
    private fun handleDeepLink(intent: Intent?) {

        val uri = intent?.data ?: return

        // Ожидаем формат: figago://command/{command}
        val command = uri.lastPathSegment ?: return

        val serviceAction = when (command) {
            "start_track" -> TrackingService.ACTION_VOICE_START_TRACK
            "stop_track" -> TrackingService.ACTION_STOP_TRACK
            "led_event" -> TrackingService.ACTION_LED_EVENT
            "announce_status" -> TrackingService.ACTION_ANNOUNCE_STATUS
            else -> {
                android.util.Log.w("MainActivity", "Неизвестная голосовая команда: $command")
                return
            }
        }

        val serviceIntent = Intent(this, TrackingService::class.java).apply {
            action = serviceAction
        }

        startService(serviceIntent)
    }
}
