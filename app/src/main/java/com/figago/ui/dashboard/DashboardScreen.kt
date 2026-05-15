package com.figago.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.figago.domain.usecase.RecordLedEventUseCase
import com.figago.ui.main.DayState
import com.figago.ui.main.MainUiState
import com.figago.ui.main.MainViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.figago.data.entity.ProfileEntity
import com.figago.data.entity.ProfileType
import com.figago.R
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.WheelchairPickup

/**
 * Экран дашборда для записи трека.
 *
 * Содержит:
 * - Огромный таймер (displayLarge)
 * - Блок дистанции
 * - Прогноз остаточного пробега (≈ X.X км осталось)
 * - Интерактивные цветные LED-индикаторы (каскадная логика)
 * - Панель управления (круглые кнопки Старт/Пауза/Стоп)
 */
@Composable
fun DashboardScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    
    
    val instantSpeedUnit by viewModel.displayInstantSpeedUnit.collectAsStateWithLifecycle()
    val avgSpeed by viewModel.displayAvgSpeed.collectAsStateWithLifecycle()
    val totalDist by viewModel.displayTotalDistance.collectAsStateWithLifecycle()
    val distUnit by viewModel.displayDistanceUnit.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Отображение ошибок через Snackbar
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            
            Spacer(modifier = Modifier.height(16.dp))

            // ===== Блок метрик =====
            MetricsBlock(
                dayState = state.dayState,
                previousDurationSec = state.previousSegmentsDurationSec,
                currentSegmentStartMs = state.currentSegmentStartTimeMs,
                avgSpeed = avgSpeed,
                instantSpeedUnit = instantSpeedUnit,
                totalDistance = totalDist,
                distUnit = distUnit
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ===== Интерактивные LED-индикаторы & Прогноз =====
            if (activeProfile?.type == ProfileType.ELECTRIC) {
                InteractiveLedIndicators(
                    ledCount = state.ledCount,
                    maxCount = activeProfile?.ledCount ?: 5, // fallback 
                    onLedClick = { clickedIndex -> handleLedClick(clickedIndex, state.ledCount, viewModel) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                ForecastDisplay(forecastKm = state.forecastRemainingKm)

                // ===== Предупреждение о расхождении статистики =====
                val showStatWarning by viewModel.statisticsWarning.collectAsStateWithLifecycle()
                if (showStatWarning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_stat_warning),
                        color = Color(0xFFFB8C00),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFB8C00).copy(alpha = 0.1f))
                            .clickable { onNavigateToSettings() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ===== Панель управления =====
            val isDiagnosticsSkipped by viewModel.isDiagnosticsSkipped.collectAsStateWithLifecycle()

            DashboardControlPanel(
                dayState = state.dayState,
                isAutoTransportEnabled = state.isAutoTransportEnabled,
                isManualTransportActive = state.isManualTransportActive,
                onStartDayAndTrack = {
                    val status = viewModel.checkPreflight()
                    if (status.isAllClear || isDiagnosticsSkipped) {
                        viewModel.startDay()
                        viewModel.startTrack()
                    } else {
                        onNavigateToDiagnostics()
                    }
                },
                onEndDay = viewModel::endDay,
                onStartTrack = {
                    val status = viewModel.checkPreflight()
                    if (status.isAllClear || isDiagnosticsSkipped) {
                        viewModel.startTrack()
                    } else {
                        onNavigateToDiagnostics()
                    }
                },
                onStopTrack = viewModel::stopTrack,
                onToggleManualTransport = { viewModel.setManualTransport(!state.isManualTransportActive) },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Хук для расчета таймера.
 */
@Composable
private fun rememberTimerString(
    dayState: DayState,
    previousDurationSec: Long,
    currentSegmentStartMs: Long?,
): String {
    var elapsedSeconds by remember(previousDurationSec, currentSegmentStartMs) { 
        mutableStateOf(previousDurationSec) 
    }
    
    LaunchedEffect(dayState, previousDurationSec, currentSegmentStartMs) {
        if (dayState == DayState.RECORDING && currentSegmentStartMs != null) {
            while (true) {
                val currentAddedSec = (System.currentTimeMillis() - currentSegmentStartMs) / 1000
                elapsedSeconds = previousDurationSec + currentAddedSec
                delay(1000)
            }
        } else {
            elapsedSeconds = previousDurationSec
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Блок метрик согласно ТЗ 10.1 - 10.2
 * — Время в пути и Средняя скорость по бокам наверху
 * — Пробег крупно по центру
 */
@Composable
private fun MetricsBlock(
    dayState: DayState,
    previousDurationSec: Long,
    currentSegmentStartMs: Long?,
    avgSpeed: String,
    instantSpeedUnit: String,
    totalDistance: String,
    distUnit: String
) {
    val timeString = rememberTimerString(dayState, previousDurationSec, currentSegmentStartMs)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ряд: Время и Средняя скорость (по бокам)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Время в пути
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(R.string.dashboard_time_elapsed),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = timeString,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Средняя скорость
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.dashboard_avg_speed),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$avgSpeed $instantSpeedUnit",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Пробег (в центре, крупно)
        Text(
            text = stringResource(R.string.dashboard_mileage),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = totalDistance,
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = distUnit,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Карусель профилей (используется глобально из NavHost)
 */
@Composable
internal fun ProfileCarousel(
    profiles: List<ProfileEntity>,
    activeProfileId: Long?,
    onProfileClick: (Long) -> Unit,
    onEditProfileClick: (Long) -> Unit,
    onAddProfileClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        items(profiles) { profile ->
            val isActive = profile.id == activeProfileId
            val bgColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = bgColor, contentColor = contentColor),
                modifier = Modifier
                    .width(100.dp)
                    .height(80.dp)
                    .clickable { 
                        if (isActive) onEditProfileClick(profile.id) else onProfileClick(profile.id) 
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val defaultIcon = if (profile.type == com.figago.data.entity.ProfileType.ELECTRIC) R.drawable.ic_notification_preview else R.drawable.ic_wheelchair_manual
                    val safeIconId = if (profile.iconId != 0) profile.iconId else defaultIcon
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(safeIconId),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = profile.name, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .width(100.dp)
                    .height(80.dp)
                    .clickable { onAddProfileClick() }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Profile")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.carousel_add), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/**
 * Панель управления (Action Buttons)
 */
@Composable
private fun DashboardControlPanel(
    dayState: DayState,
    isAutoTransportEnabled: Boolean,
    isManualTransportActive: Boolean,
    onStartDayAndTrack: () -> Unit,
    onEndDay: () -> Unit,
    onStartTrack: () -> Unit,
    onStopTrack: () -> Unit,
    onToggleManualTransport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (dayState) {
            DayState.IDLE -> {
                Button(
                    onClick = onStartDayAndTrack,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_start_trip), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            DayState.RECORDING -> {
                if (!isAutoTransportEnabled) {
                    FabActionButton(
                        icon = Icons.Filled.DirectionsCar,
                        color = if (isManualTransportActive) Color(0xFF43A047) else Color.Gray,
                        contentDescription = if (isManualTransportActive) androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_manual_transport_stop) else androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_manual_transport_start),
                        onClick = onToggleManualTransport
                    )
                }
                
                // Круглая кнопка "Пауза"
                FabActionButton(
                    icon = Icons.Filled.Pause,
                    color = Color(0xFFFB8C00), // Orange
                    contentDescription = androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_pause),
                    onClick = onStopTrack
                )
            }
            DayState.PAUSED -> {
                // Две кнопки: "Старт/Возобновить" и "Стоп/Финиш"
                FabActionButton(
                    icon = Icons.Filled.Stop,
                    color = Color(0xFFE53935), // Red
                    contentDescription = androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_finish),
                    onClick = onEndDay
                )
                FabActionButton(
                    icon = Icons.Filled.PlayArrow,
                    color = Color(0xFF43A047), // Green
                    contentDescription = androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_resume),
                    onClick = onStartTrack
                )
            }
        }
    }
}

@Composable
private fun FabActionButton(icon: ImageVector, color: Color, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Обработка нажатия на LED-индикатор.
 */
private fun handleLedClick(clickedIndex: Int, currentLedCount: Int, viewModel: MainViewModel) {

    val isActive = clickedIndex <= currentLedCount

    if (isActive) {
        // Нажали на горящий → гасим его и все правее → ledCount = clickedIndex - 1
        viewModel.setLedCount(clickedIndex - 1)
    } else {
        // Нажали на потухший → зажигаем его и все левее → ledCount = clickedIndex
        viewModel.setLedCount(clickedIndex)
    }
}

// ===== Динамическая цветовая шкала LED-индикаторов =====

/**
 * Генерирует цветовую шкалу от красного (index=1) к зелёному (index=maxCount).
 */
private fun generateLedColor(index: Int, maxCount: Int): Color {
    // Доля от 0.0 (первая = красная) до 1.0 (последняя = зелёная)
    val fraction = if (maxCount <= 1) 1f else (index - 1).toFloat() / (maxCount - 1)
    return when {
        fraction < 0.33f -> Color(0xFFE53935) // красный
        fraction < 0.66f -> Color(0xFFFB8C00) // оранжевый
        else -> Color(0xFF43A047) // зелёный
    }
}

@Composable
private fun InteractiveLedIndicators(
    ledCount: Int,
    maxCount: Int,
    onLedClick: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        for (i in 1..maxCount) {
            val isActive = i <= ledCount

            val color by animateColorAsState(
                targetValue = if (isActive) {
                    generateLedColor(i, maxCount)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                },
                animationSpec = tween(durationMillis = 400, delayMillis = (i - 1) * 80),
                label = "ledColor$i",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .clickable { onLedClick(i) },
                contentAlignment = Alignment.Center
            ) {
                // Используем BoxWithConstraints для правильного размера текста
                androidx.compose.foundation.layout.BoxWithConstraints {
                    val fontSize = (maxWidth.value * 0.45f).sp
                    Text(
                        text = i.toString(),
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastDisplay(forecastKm: Double?) {

    if (forecastKm == null) return

    Text(
        text = String.format(androidx.compose.ui.res.stringResource(com.figago.R.string.dashboard_forecast_remaining), forecastKm),
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
