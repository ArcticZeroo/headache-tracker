package com.example.headachetracker.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.correlation.CorrelationRepository
import com.example.headachetracker.data.correlation.CorrelationResult
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.repository.AnalysisRepository
import com.example.headachetracker.data.repository.HeadacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val correlations: List<CorrelationResult> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val headacheRepository: HeadacheRepository,
    private val correlationRepository: CorrelationRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(TimeRange.MONTH)
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())

    // Re-emits whenever the DB changes or the selected range changes
    val uiState: StateFlow<AnalysisUiState> = combine(
        _selectedRange,
        _currentMonth,
        headacheRepository.getAllEntries()
    ) { range, month, _ ->
        // allEntries trigger is just to detect DB changes;
        // we query the specific range below for accuracy
        Triple(range, month, Unit)
    }.flatMapLatest { (range, month, _) ->
        val now = System.currentTimeMillis()
        val rangeStart = now - (range.days.toLong() * 24 * 60 * 60 * 1000)

        headacheRepository.getEntriesByDateRangeFlow(rangeStart, now)
            .map { rangeEntries ->
                val series = analysisRepository.getAllSeriesForRange(rangeStart, now)

                val avg = if (rangeEntries.isNotEmpty()) {
                    rangeEntries.map { it.painLevel }.average().toFloat()
                } else 0f

                val latest = rangeEntries.maxByOrNull { it.timestamp }
                val daysSince = if (latest != null) {
                    ((now - latest.timestamp) / (24 * 60 * 60 * 1000)).toInt()
                } else null

                // Calendar data for the selected month
                val cal = month.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val monthEnd = cal.timeInMillis

                val monthEntries = headacheRepository.getEntriesByDateRange(monthStart, monthEnd)
                val calendarData = monthEntries.groupBy { entry ->
                    val entryCal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                    entryCal.get(Calendar.DAY_OF_MONTH).toString()
                }

                val correlations = correlationRepository.computeCorrelations(rangeStart, now)

                AnalysisUiState(
                    selectedRange = range,
                    seriesData = series,
                    calendarData = calendarData,
                    totalEntries = rangeEntries.size,
                    averagePain = avg,
                    daysSinceLastHeadache = daysSince,
                    currentMonth = month,
                    correlations = correlations,
                    isLoading = false
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState())

    fun setTimeRange(range: TimeRange) {
        _selectedRange.value = range
    }

    fun navigateMonth(forward: Boolean) {
        val cal = _currentMonth.value.clone() as Calendar
        cal.add(Calendar.MONTH, if (forward) 1 else -1)
        _currentMonth.value = cal
    }
}
