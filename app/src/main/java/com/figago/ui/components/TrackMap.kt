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
    modifier: Modifier = Modifier,
) {
    // Начальная позиция камеры (Анталья по умолчанию, обновится при получении GPS)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(36.9, 30.7), 14f)
    }

    // Автоцентрирование на последней точке трека
    val lastPoint = polylines.lastOrNull()?.points?.lastOrNull()

    LaunchedEffect(lastPoint) {

        if (lastPoint != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(lastPoint, 16f),
                durationMs = 500,
            )
        }
    }

    val mapProperties = MapProperties(
        isMyLocationEnabled = showMyLocation,
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
                Polyline(
                    points = polyline.points,
                    color = polyline.color,
                    width = 8f,
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
