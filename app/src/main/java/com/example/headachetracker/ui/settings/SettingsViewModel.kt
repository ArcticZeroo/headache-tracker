package com.example.headachetracker.ui.settings

import androidx.lifecycle.ViewModel
import com.example.headachetracker.backup.DriveBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupManager: DriveBackupManager
) : ViewModel() {

    suspend fun exportData(): String = backupManager.exportToJson()
}
