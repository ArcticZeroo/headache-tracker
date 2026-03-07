package com.example.headachetracker.ui.analysis

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.correlation.CorrelationRepository
import com.example.headachetracker.data.correlation.CorrelationResult
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import com.example.headachetracker.data.repository.AnalysisRepository
import com.example.headachetracker.data.repository.HeadacheRepository
import com.example.headachetracker.data.repository.ModelStatus
import com.example.headachetracker.data.repository.PredictionRepository
import com.example.headachetracker.data.repository.PredictionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
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
    val allSeries: Map<String, TimeSeriesData> = emptyMap(),
    val selectedSeriesName: String = "Pain Level",
    val availableSeriesNames: List<String> = listOf("Pain Level"),
    val calendarData: Map<String, List<HeadacheEntry>> = emptyMap(),
    val totalEntries: Int = 0,
    val averagePain: Float = 0f,
    val daysSinceLastHeadache: Int? = null,
    val currentMonth: Calendar = Calendar.getInstance(),
    val correlations: List<CorrelationResult> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingOverlays: Boolean = false,
    val prediction: PredictionUiState = PredictionUiState.Loading
)

sealed class PredictionUiState {
    data object Loading : PredictionUiState()
    data class NotEnoughData(val entryCount: Int, val threshold: Int) : PredictionUiState()
    data class Untrained(val entryCount: Int) : PredictionUiState()
    data class Ready(val result: PredictionResult) : PredictionUiState()
}

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val headacheRepository: HeadacheRepository,
    private val correlationRepository: CorrelationRepository,
    private val predictionRepository: PredictionRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(TimeRange.MONTH)
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    private val _uiState = MutableStateFlow(AnalysisUiState())

    val uiState: StateFlow<AnalysisUiState> = _uiState

    private var overlayJob: Job? = null
    private var correlationJob: Job? = null

    init {
        // React to range changes + DB changes → load chart data
        viewModelScope.launch {
            combine(_selectedRange, headacheRepository.getAllEntries()) { range, _ -> range }
                .collectLatest { range -> loadChartData(range) }
        }

        // React to month changes + DB changes → load calendar data independently
        viewModelScope.launch {
            combine(_currentMonth, headacheRepository.getAllEntries()) { month, _ -> month }
                .collectLatest { month -> loadCalendarData(month) }
        }

        // Load prediction once on init (and whenever entries change)
        viewModelScope.launch {
            headacheRepository.getAllEntries().collectLatest { loadPrediction() }
        }
    }

    private suspend fun loadPrediction() {
        _uiState.value = _uiState.value.copy(prediction = PredictionUiState.Loading)
        val result = try {
            predictionRepository.getPrediction()
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(prediction = PredictionUiState.Loading)
            return
        }
        val predictionUiState = when (result.status) {
            ModelStatus.NOT_ENOUGH_DATA -> PredictionUiState.NotEnoughData(
                entryCount = result.entryCount,
                threshold = com.example.headachetracker.data.ml.MIN_TRAINING_ENTRIES
            )
            ModelStatus.UNTRAINED -> PredictionUiState.Untrained(result.entryCount)
            ModelStatus.READY -> PredictionUiState.Ready(result)
        }
        _uiState.value = _uiState.value.copy(prediction = predictionUiState)
    }

    private suspend fun loadChartData(range: TimeRange) {
        val now = System.currentTimeMillis()
        val rangeStart = now - (range.days.toLong() * 24 * 60 * 60 * 1000)

        // Load pain data first (fast DB query)
        val entries = headacheRepository.getEntriesByDateRange(rangeStart, now)
        val painSeries = TimeSeriesData(
            seriesName = "Pain Level",
            color = Color(0xFFE53935),
            points = entries.map { entry ->
                TimeSeriesPoint(
                    timestamp = entry.timestamp,
                    value = entry.painLevel.toFloat(),
                    label = entry.notes
                )
            }
        )

        val avg = if (entries.isNotEmpty()) entries.map { it.painLevel }.average().toFloat() else 0f
        val latest = entries.maxByOrNull { it.timestamp }
        val daysSince = if (latest != null) {
            ((now - latest.timestamp) / (24 * 60 * 60 * 1000)).toInt()
        } else null

        _uiState.value = _uiState.value.copy(
            selectedRange = range,
            allSeries = mapOf("Pain Level" to painSeries),
            availableSeriesNames = listOf("Pain Level"),
            totalEntries = entries.size,
            averagePain = avg,
            daysSinceLastHeadache = daysSince,
            isLoading = false,
            isLoadingOverlays = true
        )

        // Load overlay series async
        overlayJob?.cancel()
        overlayJob = viewModelScope.launch {
            try {
                val allSeries = analysisRepository.getAllSeriesForRange(rangeStart, now)
                val overlaySeries = allSeries.filter { it.seriesName != "Pain Level" }
                val seriesMap = mapOf("Pain Level" to painSeries) + overlaySeries.associateBy { it.seriesName }
                _uiState.value = _uiState.value.copy(
                    allSeries = seriesMap,
                    availableSeriesNames = listOf("Pain Level") + overlaySeries.map { it.seriesName },
                    isLoadingOverlays = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingOverlays = false)
            }
        }

        // Load correlations async
        correlationJob?.cancel()
        correlationJob = viewModelScope.launch {
            try {
                val correlations = correlationRepository.computeCorrelations(rangeStart, now)
                _uiState.value = _uiState.value.copy(correlations = correlations)
            } catch (_: Exception) { }
        }
    }

    private suspend fun loadCalendarData(month: Calendar) {
        // Update month immediately (instant navigation)
        _uiState.value = _uiState.value.copy(currentMonth = month)

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
        _uiState.value = _uiState.value.copy(calendarData = calendarData)
    }

    fun setTimeRange(range: TimeRange) {
        _selectedRange.value = range
    }

    fun selectSeries(name: String) {
        _uiState.value = _uiState.value.copy(selectedSeriesName = name)
    }

    fun navigateMonth(forward: Boolean) {
        val cal = _currentMonth.value.clone() as Calendar
        cal.add(Calendar.MONTH, if (forward) 1 else -1)
        _currentMonth.value = cal
    }

    fun refreshPrediction() {
        viewModelScope.launch { loadPrediction() }
    }
}
