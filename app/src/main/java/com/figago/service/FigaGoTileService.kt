package com.figago.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.figago.domain.repository.SessionRepository
import com.figago.domain.repository.TrackRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tile Service для шторки (Quick Settings).
 * Отвечает за быстрый старт/паузу записи трека.
 */
@AndroidEntryPoint
class FigaGoTileService : TileService() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var trackRepository: TrackRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        scope.launch {
            val session = sessionRepository.getActiveSession()
            if (session == null) {
                // Если сессии нет -> стартуем всё через VOICE_START_TRACK (он сам начнёт день и трек)
                startTrackingService(TrackingService.ACTION_VOICE_START_TRACK)
                
                // Оптимистичное обновление UI
                tile.state = Tile.STATE_ACTIVE
                tile.label = "FigaGo: Пауза"
            } else {
                // Сессия есть -> проверяем активно ли сейчас пишется трек
                val activeSegment = trackRepository.getActiveSegment(session.id)
                if (activeSegment != null) {
                    // Идёт запись -> ставим на паузу
                    startTrackingService(TrackingService.ACTION_STOP_TRACK)
                    
                    // Оптимистичное обновление UI
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "FigaGo: Старт"
                } else {
                    // На паузе -> возобновляем запись
                    startTrackingService(TrackingService.ACTION_START_TRACK)
                    
                    // Оптимистичное обновление UI
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "FigaGo: Пауза"
                }
            }
            tile.updateTile()
        }
    }

    private fun startTrackingService(actionType: String) {
        val intent = Intent(this, TrackingService::class.java).apply {
            action = actionType
        }
        startService(intent)
    }

    private fun updateTileState() {
        scope.launch {
            val tile = qsTile ?: return@launch
            
            val session = sessionRepository.getActiveSession()
            if (session == null) {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "FigaGo: Старт"
            } else {
                val activeSegment = trackRepository.getActiveSegment(session.id)
                if (activeSegment != null) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "FigaGo: Пауза"
                } else {
                    tile.state = Tile.STATE_INACTIVE
                    tile.label = "FigaGo: Старт"
                }
            }
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
