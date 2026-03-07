package com.example.headachetracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.headachetracker.data.health.HealthConnectRepository
import com.example.headachetracker.data.local.DailyWeatherDao
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.repository.HeadacheRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DayContext(
    val highTemp: Double? = null,
    val lowTemp: Double? = null,
    val rainMm: Double? = null,
    val steps: Long? = null,
    val sleepHours: Double? = null
)

data class DayGroup(
    val dateLabel: String,
    val entries: List<HeadacheEntry>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HeadacheRepository,
    private val dailyWeatherDao: DailyWeatherDao,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    private val isoDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val _undoEvent = MutableSharedFlow<HeadacheEntry>()
    val undoEvent: SharedFlow<HeadacheEntry> = _undoEvent.asSharedFlow()

    private val _dayContexts = MutableStateFlow<Map<String, DayContext>>(emptyMap())
    val dayContexts: StateFlow<Map<String, DayContext>> = _dayContexts

    val groupedEntries: StateFlow<List<DayGroup>> = repository.getAllEntries()
        .map { entries ->
            val groups = entries.groupBy { entry ->
                dateFormat.format(Date(entry.timestamp))
            }.map { (date, dayEntries) ->
                DayGroup(dateLabel = date, entries = dayEntries)
            }
            // Load day context async after entries are ready
            loadDayContexts(entries)
            groups
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadDayContexts(entries: List<HeadacheEntry>) {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val entriesByDate = entries.groupBy { dateFormat.format(Date(it.timestamp)) }

            // Look up weather by ISO date string for each date group
            val contexts = mutableMapOf<String, DayContext>()
            for ((dateLabel, dayEntries) in entriesByDate) {
                val isoDate = Instant.ofEpochMilli(dayEntries.first().timestamp)
                    .atZone(zone).toLocalDate().format(isoDateFormatter)
                val weather = try {
                    dailyWeatherDao.getByDate(isoDate)
                } catch (_: Exception) { null }

                contexts[dateLabel] = DayContext(
                    highTemp = weather?.temperatureMax,
                    lowTemp = weather?.temperatureMin,
                    rainMm = weather?.rainSum
                )
            }
            _dayContexts.value = contexts

            // Load Health Connect data async (may be slow)
            try {
                if (entries.isNotEmpty()) {
                    val minTime = entries.minOf { it.timestamp }
                    val maxTime = entries.maxOf { it.timestamp }
                    val startOfRange = minTime - 24 * 60 * 60 * 1000 // buffer for sleep
                    val endOfRange = maxTime + 24 * 60 * 60 * 1000

                    val fitnessData = healthConnectRepository.getFitnessData(startOfRange, endOfRange)
                    val stepsByDate = fitnessData.associate { it.date to it.steps }

                    val sleepData = healthConnectRepository.getSleepData(startOfRange, endOfRange)
                    val sleepByDate = sleepData.associate { it.date to it.totalDurationHours }

                    val updatedContexts = contexts.toMutableMap()
                    for ((dateLabel, dayEntries) in entriesByDate) {
                        val localDate = dayEntries.first().let {
                            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
                        }
                        val existing = updatedContexts[dateLabel] ?: DayContext()
                        updatedContexts[dateLabel] = existing.copy(
                            steps = stepsByDate[localDate],
                            sleepHours = sleepByDate[localDate]
                        )
                    }
                    _dayContexts.value = updatedContexts
                }
            } catch (_: Exception) { }
        }
    }

    fun deleteEntry(entry: HeadacheEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            _undoEvent.emit(entry)
        }
    }

    fun undoDelete(entry: HeadacheEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }
}
