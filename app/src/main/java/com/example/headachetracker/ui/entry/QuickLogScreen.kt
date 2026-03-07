package com.example.headachetracker.ui.entry

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.headachetracker.ui.components.PainLevelSelector

@Composable
fun QuickLogScreen(
    isExpanded: Boolean = false,
    onEntryLogged: () -> Unit = {},
    viewModel: EntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var locationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var backgroundPermissionRequested by rememberSaveable { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Best-effort — widget location works if granted */ }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // After coarse location is granted, request background location for widget support
        if (granted && !backgroundPermissionRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBgPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasBgPermission) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            backgroundPermissionRequested = true
        }
    }

    // Request location permission once per screen lifecycle
    LaunchedEffect(Unit) {
        if (!locationPermissionRequested) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            } else if (!backgroundPermissionRequested && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Already have coarse, check for background
                val hasBgPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasBgPermission) {
                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                backgroundPermissionRequested = true
            }
            locationPermissionRequested = true
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Entry logged!", Toast.LENGTH_SHORT).show()
            viewModel.resetState()
            onEntryLogged()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val contentWidth = if (isExpanded) Modifier.widthIn(max = 480.dp) else Modifier.fillMaxWidth()

        Column(
            modifier = contentWidth.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "How's your head?",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            PainLevelSelector(
                selectedLevel = state.painLevel,
                onLevelSelected = { viewModel.setPainLevel(it) },
                enabled = !state.isSaving
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                placeholder = { Text("e.g., took ibuprofen, after screen time...") },
                enabled = !state.isSaving
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveEntry() },
                enabled = state.painLevel != null && !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Saving…")
                } else {
                    Text("Log Entry")
                }
            }
        }
    }
}
