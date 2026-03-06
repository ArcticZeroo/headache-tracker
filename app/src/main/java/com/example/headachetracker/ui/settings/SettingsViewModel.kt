package com.example.headachetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.backup.DriveBackupManager
import com.example.headachetracker.data.health.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val healthConnectAvailable: Boolean = false,
    val healthConnectConnected: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: DriveBackupManager,
    val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        checkHealthConnectStatus()
    }

    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val available = healthConnectRepository.isAvailable()
            val connected = if (available) healthConnectRepository.hasAllPermissions() else false
            _uiState.value = SettingsUiState(
                healthConnectAvailable = available,
                healthConnectConnected = connected
            )
        }
    }

    suspend fun exportData(): String = backupManager.exportToJson()
}
