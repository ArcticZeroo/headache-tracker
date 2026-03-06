package com.example.headachetracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.repository.HeadacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DayGroup(
    val dateLabel: String,
    val entries: List<HeadacheEntry>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HeadacheRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())

    val groupedEntries: StateFlow<List<DayGroup>> = repository.getAllEntries()
        .map { entries ->
            entries.groupBy { entry ->
                dateFormat.format(Date(entry.timestamp))
            }.map { (date, dayEntries) ->
                DayGroup(dateLabel = date, entries = dayEntries)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEntry(entry: HeadacheEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }
}
