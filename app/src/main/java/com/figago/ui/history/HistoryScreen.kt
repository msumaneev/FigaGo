package com.figago.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsWalk
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.figago.domain.model.DaySession
import com.figago.R
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Экран истории — список всех суточных сессий.
 *
 * При клике на сессию — навигация на экран деталей дня.
 *
 * @param onNavigateBack callback для кнопки «Назад»
 * @param onDaySelected  callback при выборе дня (передаёт sessionId)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    onDaySelected: (Long, Boolean) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val showAllProfiles by viewModel.showAllProfiles.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.history_show_all_profiles),
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.Switch(
                    checked = showAllProfiles,
                    onCheckedChange = { viewModel.toggleShowAllProfiles(it) }
                )
            }
        },
    ) { paddingValues ->

        if (showClearDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(androidx.compose.ui.res.stringResource(id = R.string.history_clear_dialog_title)) },
                text = { Text(androidx.compose.ui.res.stringResource(id = R.string.history_clear_dialog_text)) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            viewModel.deleteAllSessions()
                            showClearDialog = false
                        }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(id = R.string.dialog_yes), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearDialog = false }) {
                        Text(androidx.compose.ui.res.stringResource(id = R.string.dialog_cancel))
                    }
                }
            )
        }

        if (sessions.isEmpty()) {

            // Пустое состояние
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.height(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(com.figago.R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        iconId = activeProfile?.iconId,
                        onClick = { onDaySelected(session.id, showAllProfiles) },
                        onDelete = { viewModel.deleteSession(session.id) },
                    )
                }

                if (sessions.isNotEmpty()) {
                    item {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { showClearDialog = true }
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.DeleteSweep,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(id = R.string.history_clear_all),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка одной сессии в списке истории.
 */
@Composable
private fun SessionCard(
    session: DaySession,
    iconId: Int?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val distanceKm = session.totalDistance / 1000.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            val finalIconId = if (iconId != null && iconId != 0) iconId else R.drawable.ic_notification_preview
            androidx.compose.foundation.Image(
                painter = painterResource(id = finalIconId),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .height(56.dp)
                    .width(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp) // внутренний отступ для иконки внутри черного квадрата
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = String.format("%.2f %s", distanceKm, stringResource(R.string.unit_km)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }

            // Статус сессии или кнопка удаления
            if (session.isActive) {
                Text(
                    text = stringResource(com.figago.R.string.history_active_session),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                        contentDescription = androidx.compose.ui.res.stringResource(id = R.string.history_delete_track_title),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
