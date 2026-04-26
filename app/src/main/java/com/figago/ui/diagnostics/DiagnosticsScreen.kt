package com.figago.ui.diagnostics

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.figago.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    onForceStart: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val isSkipped by viewModel.isSkipped.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh status when returning to the screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.diagnostics_description),
                style = MaterialTheme.typography.bodyLarge
            )

            status?.let { st ->
                DiagnosticItem(
                    title = stringResource(R.string.diagnostics_gps_enabled),
                    isOk = st.isGpsEnabled,
                    onFix = { context.startActivity(viewModel.getGpsIntent()) }
                )

                DiagnosticItem(
                    title = stringResource(R.string.diagnostics_bg_location),
                    isOk = st.hasBackgroundLocation,
                    onFix = { context.startActivity(viewModel.getAppSettingsIntent()) }
                )

                DiagnosticItem(
                    title = stringResource(R.string.diagnostics_battery_exempt),
                    isOk = st.isIgnoringBatteryOptimizations,
                    onFix = { 
                        try {
                            context.startActivity(viewModel.getBatteryIntent())
                        } catch (e: android.content.ActivityNotFoundException) {
                            context.startActivity(viewModel.getAppSettingsIntent())
                        }
                    }
                )

                DiagnosticItem(
                    title = stringResource(R.string.diagnostics_activity_recognition),
                    isOk = st.hasActivityRecognition,
                    onFix = { context.startActivity(viewModel.getAppSettingsIntent()) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (st.isAllClear) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.diagnostics_all_clear))
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = isSkipped,
                            onCheckedChange = { viewModel.setSkipDiagnostics(it) }
                        )
                        Text(stringResource(R.string.diagnostics_skip_checkbox), modifier = Modifier.padding(start = 8.dp))
                    }
                    Button(
                        onClick = onForceStart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.diagnostics_force_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    title: String,
    isOk: Boolean,
    onFix: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isOk) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                if (!isOk) {
                    Text(text = stringResource(R.string.diagnostics_action_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (!isOk) {
                Button(onClick = onFix) {
                    Text(stringResource(R.string.diagnostics_fix))
                }
            }
        }
    }
}
