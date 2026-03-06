package com.example.headachetracker.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.repository.AnalysisRepository
import com.example.headachetracker.data.repository.HeadacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class TimeRange(val days: Int, val label: String) {
    WEEK(7, "7 Days"),
    MONTH(30, "30 Days"),
    QUARTER(90, "90 Days")
}

data class AnalysisUiState(
    val selectedRange: TimeRange = TimeRange.MONTH,
    val seriesData: List<TimeSeriesData> = emptyList(),
    val calendarData: Map<String, List<HeadacheEntry>> = emptyMap(),
    val totalEntries: Int = 0,
    val averagePain: Float = 0f,
    val daysSinceLastHeadache: Int? = null,
    val currentMonth: Calendar = Calendar.getInstance(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val headacheRepository: HeadacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        loadData()
    }

    fun navigateMonth(forward: Boolean) {
        val cal = _uiState.value.currentMonth.clone() as Calendar
        cal.add(Calendar.MONTH, if (forward) 1 else -1)
        _uiState.value = _uiState.value.copy(currentMonth = cal)
        loadCalendarData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val now = System.currentTimeMillis()
            val rangeStart = now - (_uiState.value.selectedRange.days.toLong() * 24 * 60 * 60 * 1000)

            // Load all time series data
            val series = analysisRepository.getAllSeriesForRange(rangeStart, now)

            // Load stats
            val entries = headacheRepository.getEntriesByDateRange(rangeStart, now)
            val avg = if (entries.isNotEmpty()) {
                entries.map { it.painLevel }.average().toFloat()
            } else 0f

            val latest = headacheRepository.getLatestEntry()
            val daysSince = if (latest != null) {
                ((now - latest.timestamp) / (24 * 60 * 60 * 1000)).toInt()
            } else null

            _uiState.value = _uiState.value.copy(
                seriesData = series,
                totalEntries = entries.size,
                averagePain = avg,
                daysSinceLastHeadache = daysSince,
                isLoading = false
            )

            loadCalendarData()
        }
    }

    private fun loadCalendarData() {
        viewModelScope.launch {
            val cal = _uiState.value.currentMonth.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val monthStart = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            val entries = headacheRepository.getEntriesByDateRange(monthStart, monthEnd)

            val grouped = entries.groupBy { entry ->
                val entryCal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                entryCal.get(Calendar.DAY_OF_MONTH).toString()
            }

            _uiState.value = _uiState.value.copy(calendarData = grouped)
        }
    }
}
