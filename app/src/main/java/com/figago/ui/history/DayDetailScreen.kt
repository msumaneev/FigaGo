package com.figago.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.figago.ui.components.LedMarkerData
import com.figago.ui.components.TrackMap
import com.figago.ui.components.TrackPolylineData
import com.google.android.gms.maps.model.LatLng

/**
 * Экран деталей дня — карта со всеми отрезками, маркеры LED-событий, статистика.
 *
 * @param sessionId     id выбранной суточной сессии
 * @param onNavigateBack callback для кнопки «Назад»
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    sessionId: Long,
    showAllProfiles: Boolean = false,
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val dayStats by viewModel.dayStats.collectAsStateWithLifecycle()
    val polylines by viewModel.polylines.collectAsStateWithLifecycle()

    // Загрузка данных при первом отображении
    LaunchedEffect(sessionId, showAllProfiles) {
        viewModel.toggleShowAllProfiles(showAllProfiles)
        viewModel.loadDayStats(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dayStats?.session?.date ?: stringResource(com.figago.R.string.day_detail_title_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.figago.R.string.content_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {

            // ===== Панель статистики =====
            val stats = dayStats

            if (stats != null) {
                StatsPanel(stats = stats)
            }

            // ===== Карта =====
            val selectedSegmentId by viewModel.selectedSegmentId.collectAsStateWithLifecycle()
            
            TrackMap(
                polylines = polylines, 
                ledMarkers = stats?.ledEvents?.mapIndexed { index, event ->
                    LedMarkerData(
                        // Пока нет точной координаты (можно искать ближайшую точку по timestamp) - ставим 0,0 или последнюю
                        position = polylines.lastOrNull()?.points?.lastOrNull() ?: LatLng(0.0, 0.0), 
                        ledCountRemaining = event.ledCountRemaining,
                        title = "LED #${index + 1}",
                    )
                } ?: emptyList(),
                showMyLocation = false,
                selectedSegmentId = selectedSegmentId,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
            )
            
            // ===== Список сегментов =====
            val segments = stats?.segments ?: emptyList()
            if (segments.size > 1) {
                Text(
                    text = stringResource(com.figago.R.string.stat_segments),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                segments.forEachIndexed { index, segment ->
                    val isSelected = segment.id == selectedSegmentId
                    val segmentColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    
                    val segmentDistanceKm = segment.segmentDistance / 1000.0
                    val title = stringResource(com.figago.R.string.history_segment_name, index + 1)
                    val distanceText = String.format("%.2f %s", segmentDistanceKm, stringResource(com.figago.R.string.unit_km))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable {
                                if (isSelected) viewModel.selectSegment(null) else viewModel.selectSegment(segment.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = segmentColor,
                            contentColor = contentColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = distanceText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Панель статистики дня: дистанция, отрезки, время, LED-события.
 */
@Composable
private fun StatsPanel(stats: com.figago.domain.usecase.DayStats) {
    val distanceKm = stats.totalDistance / 1000.0

    // Форматирование времени в движении
    val hours = stats.totalMovingTimeMs / 3_600_000
    val minutes = (stats.totalMovingTimeMs % 3_600_000) / 60_000

    val timeText = when {
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes}min"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            Text(
                text = stringResource(com.figago.R.string.day_detail_stats_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = stringResource(com.figago.R.string.stat_distance), value = String.format("%.2f %s", distanceKm, stringResource(com.figago.R.string.unit_km)))
                StatItem(label = stringResource(com.figago.R.string.stat_moving_time), value = timeText)
            }
            
            val walkHours = stats.walkTimeMs / 3_600_000
            val walkMinutes = (stats.walkTimeMs % 3_600_000) / 60_000
            val walkTimeText = if (walkHours > 0) "${walkHours}h ${walkMinutes}m" else "${walkMinutes}m"
            val walkKm = stats.walkDistance / 1000.0

            val transportHours = stats.transportTimeMs / 3_600_000
            val transportMinutes = (stats.transportTimeMs % 3_600_000) / 60_000
            val transportTimeText = if (transportHours > 0) "${transportHours}h ${transportMinutes}m" else "${transportMinutes}m"
            val transportKm = stats.transportDistance / 1000.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = stringResource(com.figago.R.string.history_stat_walk), value = String.format("%.2f %s   •   %s", walkKm, stringResource(com.figago.R.string.unit_km), walkTimeText))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = stringResource(com.figago.R.string.history_stat_transport), value = String.format("%.2f %s   •   %s", transportKm, stringResource(com.figago.R.string.unit_km), transportTimeText))
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem(label = stringResource(com.figago.R.string.stat_segments), value = "${stats.segmentCount}")
                StatItem(label = "LED", value = "${stats.ledEventCount}")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
