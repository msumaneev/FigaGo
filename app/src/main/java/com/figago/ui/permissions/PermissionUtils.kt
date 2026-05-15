package com.figago.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.figago.R
import androidx.compose.ui.res.stringResource

/**
 * Набор необходимых runtime-разрешений в зависимости от версии Android.
 */
fun getRequiredPermissions(): List<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    // Android 13+ — разрешение на уведомления
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }



    return permissions
}

/**
 * Проверяет, все ли необходимые разрешения предоставлены.
 */
fun hasAllPermissions(context: Context): Boolean {
    return getRequiredPermissions().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Composable-обёртка для запроса runtime-разрешений при первом запуске.
 *
 * Отображает информативный диалог при отказе, объясняя зачем нужны разрешения.
 *
 * @param onAllGranted callback, вызываемый когда все разрешения получены
 */
@Composable
fun RequestPermissionsEffect(onAllGranted: () -> Unit = {}) {
    var showRationale by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->

        val allGranted = results.values.all { it }

        if (allGranted) {
            permissionsGranted = true
            onAllGranted()
        } else {
            showRationale = true
        }
    }

    // Запрос разрешений при первом отображении
    LaunchedEffect(Unit) {
        launcher.launch(getRequiredPermissions().toTypedArray())
    }

    // Диалог-объяснение при отказе
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(stringResource(R.string.permission_required_title)) },
            text = {
                Text(stringResource(R.string.permission_required_text))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(getRequiredPermissions().toTypedArray())
                    },
                ) {
                    Text(stringResource(R.string.permission_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) {
                    Text(stringResource(R.string.permission_close))
                }
            },
        )
    }
}
