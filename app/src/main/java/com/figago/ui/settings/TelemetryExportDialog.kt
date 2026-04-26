package com.figago.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.figago.R

/**
 * Идентификаторы категорий экспортируемых данных.
 */
object TelemetryCategory {
    const val DEVICE_INFO = "device_info"      // 🔒 всегда
    const val SETTINGS = "settings"             // 🔒 всегда
    const val BATTERY_DATA = "battery_data"     // по умолчанию ВКЛ
    const val TRIP_HISTORY = "trip_history"      // по умолчанию ВКЛ
    const val ERROR_LOG = "error_log"           // по умолчанию ВКЛ
    const val GPS_COORDS = "gps_coords"         // по умолчанию ВЫКЛ
}

/**
 * Диалог экспорта телеметрии с чекбоксами категорий.
 *
 * Категории «Устройство» и «Настройки» всегда включены (заблокированы).
 * GPS-координаты выключены по умолчанию.
 */
@Composable
fun TelemetryExportDialog(
    onDismiss: () -> Unit,
    onExport: (Set<String>) -> Unit,
) {
    var batteryChecked by remember { mutableStateOf(true) }
    var tripChecked by remember { mutableStateOf(true) }
    var errorChecked by remember { mutableStateOf(true) }
    var gpsChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.telemetry_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.telemetry_dialog_subtitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Устройство — 🔒 заблокировано
                LockedCategoryRow(
                    label = stringResource(R.string.telemetry_cat_device),
                    description = stringResource(R.string.telemetry_cat_device_desc),
                )

                // 2. Настройки — 🔒 заблокировано
                LockedCategoryRow(
                    label = stringResource(R.string.telemetry_cat_settings),
                    description = stringResource(R.string.telemetry_cat_settings_desc),
                )

                // 3. Батарея
                CategoryCheckboxRow(
                    label = stringResource(R.string.telemetry_cat_battery),
                    description = stringResource(R.string.telemetry_cat_battery_desc),
                    checked = batteryChecked,
                    onCheckedChange = { batteryChecked = it }
                )

                // 4. История поездок
                CategoryCheckboxRow(
                    label = stringResource(R.string.telemetry_cat_trips),
                    description = stringResource(R.string.telemetry_cat_trips_desc),
                    checked = tripChecked,
                    onCheckedChange = { tripChecked = it }
                )

                // 5. Журнал ошибок
                CategoryCheckboxRow(
                    label = stringResource(R.string.telemetry_cat_errors),
                    description = stringResource(R.string.telemetry_cat_errors_desc),
                    checked = errorChecked,
                    onCheckedChange = { errorChecked = it }
                )

                // 6. GPS-координаты
                CategoryCheckboxRow(
                    label = stringResource(R.string.telemetry_cat_gps),
                    description = stringResource(R.string.telemetry_cat_gps_desc),
                    checked = gpsChecked,
                    onCheckedChange = { gpsChecked = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val categories = mutableSetOf(
                    TelemetryCategory.DEVICE_INFO,
                    TelemetryCategory.SETTINGS
                )
                if (batteryChecked) categories.add(TelemetryCategory.BATTERY_DATA)
                if (tripChecked) categories.add(TelemetryCategory.TRIP_HISTORY)
                if (errorChecked) categories.add(TelemetryCategory.ERROR_LOG)
                if (gpsChecked) categories.add(TelemetryCategory.GPS_COORDS)
                onExport(categories)
            }) {
                Text(stringResource(R.string.telemetry_btn_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun LockedCategoryRow(label: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = true, onCheckedChange = null, enabled = false)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).height(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryCheckboxRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
