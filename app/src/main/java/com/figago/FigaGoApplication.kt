package com.figago

import android.app.Application
import com.figago.service.ErrorLogCollector
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входа приложения FigaGo.
 * Аннотация @HiltAndroidApp инициализирует Hilt DI-контейнер при старте.
 */
@HiltAndroidApp
class FigaGoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация перехватчика ошибок для телеметрии
        ErrorLogCollector(this).install()
    }
}
