package com.example.headachetracker.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.headachetracker.ui.components.ContentCard
import com.example.headachetracker.ui.theme.Dimensions
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsState by viewModel.uiState.collectAsState()

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.checkHealthConnectStatus()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Backup & Data", style = MaterialTheme.typography.headlineSmall)
        }
        item { BackupSection() }
        item { ExportSection(onExport = { scope.launch {
            val json = viewModel.exportData()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, json)
                putExtra(Intent.EXTRA_SUBJECT, "Headache Tracker Export")
            }
            context.startActivity(Intent.createChooser(intent, "Share export"))
        }}) }
        item {
            Text(text = "Data Sources", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            HealthConnectCard(
                isAvailable = settingsState.healthConnectAvailable,
                isConnected = settingsState.healthConnectConnected,
                onInstall = {
                    Toast.makeText(context, "Please install Health Connect from the Play Store", Toast.LENGTH_LONG).show()
                },
                onConnect = {
                    healthConnectPermissionLauncher.launch(viewModel.healthConnectRepository.requiredPermissions)
                },
                onRefresh = { viewModel.checkHealthConnectStatus() }
            )
        }
    }
}

@Composable
private fun BackupSection() {
    ContentCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.height(Dimensions.GapSmall))
        Text(text = "Auto Backup Enabled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(modifier = Modifier.height(Dimensions.GapTight))
        Text(
            text = "Your headache data is automatically backed up to Google Drive via Android Auto Backup. It will be restored when you sign in to a new device with your Google account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ExportSection(onExport: () -> Unit) {
    ContentCard {
        Text(text = "Manual Export", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(Dimensions.GapTight))
        Text(
            text = "Export your data as a JSON file you can save or share.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimensions.GapMedium))
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(Dimensions.GapTight))
            Text("Export & Share")
        }
    }
}

@Composable
private fun HealthConnectCard(
    isAvailable: Boolean,
    isConnected: Boolean,
    onInstall: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    ContentCard(
        containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
            contentDescription = null,
            tint = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimensions.GapSmall))
        Text(text = "Health Connect", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(Dimensions.GapTight))
        Text(
            text = if (isConnected) {
                "Connected — reading sleep, steps, and exercise data for headache correlation analysis."
            } else {
                "Connect to Health Connect to correlate sleep, steps, and exercise data with your headaches."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimensions.GapMedium))
        when {
            !isAvailable -> OutlinedButton(onClick = onInstall) { Text("Install Health Connect") }
            !isConnected -> OutlinedButton(onClick = onConnect) { Text("Connect") }
            else         -> OutlinedButton(onClick = onRefresh) { Text("Refresh Status") }
        }
    }
}
