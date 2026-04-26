package com.figago.ui.diagnostics

import androidx.lifecycle.ViewModel
import com.figago.domain.usecase.PreflightChecker
import com.figago.domain.usecase.PreflightStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val preflightChecker: PreflightChecker,
    private val settingsRepository: com.figago.domain.repository.SettingsRepository
) : ViewModel() {

    private val _status = MutableStateFlow<PreflightStatus?>(null)
    val status: StateFlow<PreflightStatus?> = _status.asStateFlow()

    val isSkipped: StateFlow<Boolean> = settingsRepository.observeSkipDiagnostics()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), false)

    fun checkStatus() {
        _status.value = preflightChecker.check()
    }

    fun setSkipDiagnostics(skip: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSkipDiagnostics(skip)
        }
    }

    fun getGpsIntent() = preflightChecker.getGpsSettingsIntent()
    fun getAppSettingsIntent() = preflightChecker.getAppSettingsIntent()
    fun getBatteryIntent() = preflightChecker.getBatteryOptimizationSettingsIntent()
}
