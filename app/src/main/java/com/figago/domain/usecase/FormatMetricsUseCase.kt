package com.figago.domain.usecase

import android.content.Context
import com.figago.R
import com.figago.domain.repository.SettingsRepository
import com.figago.domain.repository.SettingsRepository.Companion.UNIT_MILES
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Класс, хранящий форматированные значения метрик.
 */
data class FormattedMetrics(
    val distanceValue: Double,
    val distanceUnitString: String,
    val formattedDistanceString: String,
    val speedValue: Double,
    val speedUnitString: String,
)

/**
 * Утилитарный юзкейс для форматирования метрик (дистанции и скорости) с учетом 
 * выбранной пользователем системы измерений (КМ или Мили).
 * 
 * Все данные в БД хранятся строго в Метрах (Meters) и Км/ч (Km/h).
 * Их конвертация в мили происходит ТОЛЬКО на этом слое перед отдачей в UI.
 *
 * formattedDistanceString — полностью локализованная строка вида "250 м" или "1 км 200 м",
 * формируется из строковых ресурсов (R.string.fmt_*).
 */
class FormatMetricsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Конвертирует метры и скорость (км/ч) в нужную пользователю систему мер.
     * @param distanceMeters дистанция в метрах.
     * @param currentSpeedKmh текущая скорость в км/ч.
     */
    suspend operator fun invoke(distanceMeters: Double, currentSpeedKmh: Double): FormattedMetrics {
        val unitSystem = settingsRepository.getUnitSystem()
        
        val distanceKm = distanceMeters / 1000.0
        
        return if (unitSystem == UNIT_MILES) {
            val distMiles = distanceKm * 0.621371
            val distFeet = distMiles * 5280.0
            
            val formattedDist = if (distMiles < 1.0) {
                context.getString(R.string.fmt_feet, distFeet.toInt())
            } else {
                val fullMiles = distMiles.toInt()
                val remainingFeet = ((distMiles - fullMiles) * 5280).toInt()

                if (remainingFeet > 0) {
                    context.getString(R.string.fmt_mi_ft, fullMiles, remainingFeet)
                } else {
                    context.getString(R.string.fmt_mi_only, fullMiles)
                }
            }

            FormattedMetrics(
                distanceValue = distMiles,
                distanceUnitString = context.getString(R.string.unit_mi),
                formattedDistanceString = formattedDist,
                speedValue = currentSpeedKmh * 0.621371,
                speedUnitString = context.getString(R.string.unit_mph)
            )
        } else {
            val formattedDist = if (distanceMeters < 1000.0) {
                context.getString(R.string.fmt_meters, distanceMeters.toInt())
            } else {
                val fullKm = (distanceMeters / 1000).toInt()
                val remainingM = (distanceMeters % 1000).toInt()

                if (remainingM > 0) {
                    context.getString(R.string.fmt_km_m, fullKm, remainingM)
                } else {
                    context.getString(R.string.fmt_km_only, fullKm)
                }
            }

            FormattedMetrics(
                distanceValue = distanceKm,
                distanceUnitString = context.getString(R.string.unit_km),
                formattedDistanceString = formattedDist,
                speedValue = currentSpeedKmh,
                speedUnitString = context.getString(R.string.unit_kmh)
            )
        }
    }
}
