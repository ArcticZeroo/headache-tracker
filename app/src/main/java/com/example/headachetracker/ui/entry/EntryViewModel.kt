package com.example.headachetracker.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.repository.HeadacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isSaved: Boolean = false
)

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val repository: HeadacheRepository
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

        viewModelScope.launch {
            if (state.isEditing && state.entryId != null) {
                repository.updateEntry(
                    HeadacheEntry(
                        id = state.entryId,
                        painLevel = painLevel,
                        timestamp = state.timestamp,
                        notes = state.notes.ifBlank { null }
                    )
                )
            } else {
                repository.insertEntry(
                    HeadacheEntry(
                        painLevel = painLevel,
                        timestamp = state.timestamp,
                        notes = state.notes.ifBlank { null }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun resetState() {
        _uiState.value = EntryUiState()
    }
}
