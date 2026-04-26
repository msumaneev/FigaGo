package com.figago.domain.usecase

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Статусы проверок перед стартом трекинга.
 */
data class PreflightStatus(
    val isGpsEnabled: Boolean,
    val hasBackgroundLocation: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
    val hasActivityRecognition: Boolean,
) {
    val isAllClear: Boolean
        get() = isGpsEnabled && hasBackgroundLocation && isIgnoringBatteryOptimizations && hasActivityRecognition
}

/**
 * Утилита для выполнения предстартовых проверок системы.
 */
@Singleton
class PreflightChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Выполняет все проверки и возвращает результат.
     */
    fun check(): PreflightStatus {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // До Android 10 фон разрешен автоматически при ACCESS_FINE_LOCATION
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }

        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return PreflightStatus(
            isGpsEnabled = isGpsEnabled,
            hasBackgroundLocation = hasBackgroundLocation,
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
            hasActivityRecognition = hasActivityRecognition,
        )
    }

    /**
     * Интент для включения GPS.
     */
    fun getGpsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    /**
     * Интент для настроек приложения (чтобы дать разрешение на фоне).
     */
    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    /**
     * Интент для исключения из экономии энергии.
     */
    fun getBatteryOptimizationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            getAppSettingsIntent()
        }
    }
}
