package com.figago.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.os.Build

// ===== Цветовая палитра FigaGo =====
// Тёмная тема по умолчанию — для использования на улице / в движении

private val FigaGoPrimary = Color(0xFF4CAF50)          // Зелёный — активность, движение
private val FigaGoPrimaryVariant = Color(0xFF388E3C)
private val FigaGoSecondary = Color(0xFFFFC107)         // Жёлтый-янтарный — лампочки/индикаторы
private val FigaGoError = Color(0xFFEF5350)             // Красный — стоп, ошибки
private val FigaGoBackground = Color(0xFF121212)
private val FigaGoSurface = Color(0xFF1E1E2E)
private val FigaGoOnPrimary = Color.White
private val FigaGoOnBackground = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = FigaGoPrimary,
    onPrimary = FigaGoOnPrimary,
    secondary = FigaGoSecondary,
    onSecondary = Color.Black,
    error = FigaGoError,
    background = FigaGoBackground,
    surface = FigaGoSurface,
    onBackground = FigaGoOnBackground,
    onSurface = FigaGoOnBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = FigaGoPrimaryVariant,
    onPrimary = Color.White,
    secondary = FigaGoSecondary,
    onSecondary = Color.Black,
    error = FigaGoError,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

/**
 * Compose-тема приложения FigaGo.
 * По умолчанию используется тёмная тема для лучшей видимости на улице.
 */
@Composable
fun FigaGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
