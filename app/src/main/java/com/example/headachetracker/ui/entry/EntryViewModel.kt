package com.example.headachetracker.ui.entry

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.location.GeocodingProvider
import com.example.headachetracker.data.location.LocationProvider
import com.example.headachetracker.data.repository.HeadacheRepository
import com.example.headachetracker.data.weather.WeatherSyncWorker
import com.example.headachetracker.data.ml.MIN_TRAINING_ENTRIES
import com.example.headachetracker.data.ml.ModelTrainingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryUiState(
    val painLevel: Int? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEditing: Boolean = false,
    val entryId: Long? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repository: HeadacheRepository,
    private val locationProvider: LocationProvider,
    private val geocodingProvider: GeocodingProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryUiState())
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    fun setPainLevel(level: Int) {
        _uiState.value = _uiState.value.copy(painLevel = level)
    }

    fun setNotes(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun setTimestamp(timestamp: Long) {
        _uiState.value = _uiState.value.copy(timestamp = timestamp)
    }

    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            repository.getEntryById(entryId)?.let { entry ->
                _uiState.value = EntryUiState(
                    painLevel = entry.painLevel,
                    notes = entry.notes ?: "",
                    timestamp = entry.timestamp,
                    isEditing = true,
                    entryId = entry.id
                )
            }
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        val painLevel = state.painLevel ?: return

        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()
            val locationName = if (location != null) {
                geocodingProvider.getLocationName(location.latitude, location.longitude)
            } else null

            if (state.isEditing && state.entryId != null) {
                repository.updateEntry(
                    HeadacheEntry(
                        id = state.entryId,
                        painLevel = painLevel,
                        timestamp = state.timestamp,
                        notes = state.notes.ifBlank { null },
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationName = locationName
                    )
                )
            } else {
                repository.insertEntry(
                    HeadacheEntry(
                        painLevel = painLevel,
                        timestamp = state.timestamp,
                        notes = state.notes.ifBlank { null },
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationName = locationName
                    )
                )
            }
            if (location != null) {
                val syncRequest = OneTimeWorkRequestBuilder<WeatherSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueue(syncRequest)
            }

            // Retrain prediction model every 5 new entries once the threshold is met
            val totalCount = repository.getEntryCount()
            if (!state.isEditing && totalCount >= MIN_TRAINING_ENTRIES && totalCount % 5 == 0) {
                val trainRequest = OneTimeWorkRequestBuilder<ModelTrainingWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    ModelTrainingWorker.WORK_NAME,
                    androidx.work.ExistingWorkPolicy.KEEP,
                    trainRequest
                )
            }

            _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = EntryUiState()
    }
}
