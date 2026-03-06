package com.example.headachetracker.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class DailySleepData(
    val date: LocalDate,
    val totalDurationHours: Double
)

data class DailyFitnessData(
    val date: LocalDate,
    val steps: Long,
    val exerciseMinutes: Long
)

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getSleepData(startMillis: Long, endMillis: Long): List<DailySleepData> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasAllPermissions()) return emptyList()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startMillis),
                        Instant.ofEpochMilli(endMillis)
                    )
                )
            )

            val zone = ZoneId.systemDefault()
            response.records
                .groupBy { record ->
                    // Group by the date the sleep session ended (the morning)
                    record.endTime.atZone(zone).toLocalDate()
                }
                .map { (date, sessions) ->
                    val totalMinutes = sessions.sumOf { session ->
                        Duration.between(session.startTime, session.endTime).toMinutes()
                    }
                    DailySleepData(
                        date = date,
                        totalDurationHours = totalMinutes / 60.0
                    )
                }
                .sortedBy { it.date }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFitnessData(startMillis: Long, endMillis: Long): List<DailyFitnessData> {
        val client = healthConnectClient ?: return emptyList()
        if (!hasAllPermissions()) return emptyList()

        return try {
            val zone = ZoneId.systemDefault()
            val startInstant = Instant.ofEpochMilli(startMillis)
            val endInstant = Instant.ofEpochMilli(endMillis)
            val timeFilter = TimeRangeFilter.between(startInstant, endInstant)

            val stepsResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeFilter
                )
            )

            val exerciseResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeFilter
                )
            )

            val stepsByDate = stepsResponse.records.groupBy { record ->
                record.startTime.atZone(zone).toLocalDate()
            }.mapValues { (_, records) -> records.sumOf { it.count } }

            val exerciseByDate = exerciseResponse.records.groupBy { record ->
                record.startTime.atZone(zone).toLocalDate()
            }.mapValues { (_, records) ->
                records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
            }

            val allDates = (stepsByDate.keys + exerciseByDate.keys).toSortedSet()
            allDates.map { date ->
                DailyFitnessData(
                    date = date,
                    steps = stepsByDate[date] ?: 0L,
                    exerciseMinutes = exerciseByDate[date] ?: 0L
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
