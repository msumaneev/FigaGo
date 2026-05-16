package com.figago.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.figago.domain.model.LocationPoint
import com.figago.domain.model.LedEvent

/**
 * Данные для отображения одного отрезка пути на карте.
 *
 * @property points     GPS-точки отрезка
 * @property color      цвет полилинии
 */
data class TrackPolylineData(
    val points: List<LatLng>,
    val color: Color = Color(0xFFFF9800),
    val segmentId: Long? = null,
)

/**
 * Данные для маркера LED-события на карте.
 *
 * @property position         координаты маркера
 * @property ledCountRemaining оставшееся число индикаторов
 * @property title            подпись маркера
 */
data class LedMarkerData(
    val position: LatLng,
    val ledCountRemaining: Int,
    val title: String,
)

/**
 * Compose-обёртка над Google Maps для отображения треков.
 *
 * Поддерживает:
 * - Отображение текущей позиции пользователя
 * - Отрисовку полилиний (TrackSegment)
 * - Маркеры LED-событий
 * - Автоцентрирование на последней точке
 *
 * @param polylines   список полилиний для отрисовки
 * @param ledMarkers  список маркеров LED-событий
 * @param showMyLocation отображать текущую геопозицию
 * @param modifier    Modifier
 */
@Composable
fun TrackMap(
    polylines: List<TrackPolylineData> = emptyList(),
    ledMarkers: List<LedMarkerData> = emptyList(),
    showMyLocation: Boolean = true,
    selectedSegmentId: Long? = null,
    modifier: Modifier = Modifier,
) {
    // Начальная позиция камеры (Анталья по умолчанию, обновится при получении GPS)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(36.9, 30.7), 14f)
    }

    // Автоцентрирование на выбранном сегменте или последней точке
    LaunchedEffect(selectedSegmentId, polylines) {
        if (selectedSegmentId != null) {
            val selectedPolyline = polylines.find { it.segmentId == selectedSegmentId }
            if (selectedPolyline != null && selectedPolyline.points.isNotEmpty()) {
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.builder()
                selectedPolyline.points.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                
                // If it's a single point or very tight bounds, we might need a fallback, but bounds usually works.
                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 150),
                        durationMs = 500,
                    )
                } catch (e: Exception) {
                    // Fallback to zoom if bounds are too small or view not laid out
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(selectedPolyline.points.first(), 16f),
                        durationMs = 500,
                    )
                }
            }
        } else {
            val lastPoint = polylines.lastOrNull()?.points?.lastOrNull()
            if (lastPoint != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(lastPoint, 16f),
                    durationMs = 500,
                )
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
    androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val mapProperties = MapProperties(
        isMyLocationEnabled = showMyLocation && hasLocationPermission,
        mapType = MapType.NORMAL,
    )

    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        myLocationButtonEnabled = showMyLocation,
        compassEnabled = true,
    )

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
    ) {

        // Отрисовка полилиний (отрезки пути)
        for (polyline in polylines) {
            if (polyline.points.size >= 2) {
                val isSelected = selectedSegmentId == null || polyline.segmentId == selectedSegmentId
                val polyColor = if (isSelected) polyline.color else polyline.color.copy(alpha = 0.3f)
                val polyWidth = if (selectedSegmentId != null && polyline.segmentId == selectedSegmentId) 12f else 8f

                Polyline(
                    points = polyline.points,
                    color = polyColor,
                    width = polyWidth,
                    zIndex = if (selectedSegmentId != null && polyline.segmentId == selectedSegmentId) 1f else 0f,
                )
            }
        }

        // Маркеры LED-событий
        for (marker in ledMarkers) {
            Marker(
                state = MarkerState(position = marker.position),
                title = marker.title,
                snippet = stringResource(com.figago.R.string.map_snippet_led).format(marker.ledCountRemaining),
            )
        }
    }
}
