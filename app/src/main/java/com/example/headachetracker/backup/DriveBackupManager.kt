package com.example.headachetracker.backup

import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.HeadacheEntry
import com.example.headachetracker.data.local.WeatherDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupManager @Inject constructor(
    private val dao: HeadacheDao,
    private val weatherDao: WeatherDao
) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val entries = dao.getEntriesByDateRange(0, Long.MAX_VALUE)
        val jsonArray = JSONArray()

        entries.forEach { entry ->
            val entryJson = JSONObject().apply {
                put("id", entry.id)
                put("painLevel", entry.painLevel)
                put("timestamp", entry.timestamp)
                put("notes", entry.notes ?: JSONObject.NULL)
                put("latitude", entry.latitude ?: JSONObject.NULL)
                put("longitude", entry.longitude ?: JSONObject.NULL)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
            }

            // Include weather data if available
            val weather = weatherDao.getByEntryId(entry.id)
            if (weather != null) {
                entryJson.put("weather", JSONObject().apply {
                    put("date", weather.date)
                    put("temperatureMax", weather.temperatureMax ?: JSONObject.NULL)
                    put("temperatureMin", weather.temperatureMin ?: JSONObject.NULL)
                    put("pressureMean", weather.pressureMean ?: JSONObject.NULL)
                    put("rainSum", weather.rainSum ?: JSONObject.NULL)
                })
            }

            jsonArray.put(entryJson)
        }

        JSONObject().apply {
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("entries", jsonArray)
        }.toString(2)
    }

    suspend fun importFromJson(json: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        val entries = root.getJSONArray("entries")

        for (i in 0 until entries.length()) {
            val obj = entries.getJSONObject(i)
            val entry = HeadacheEntry(
                painLevel = obj.getInt("painLevel"),
                timestamp = obj.getLong("timestamp"),
                notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                latitude = if (obj.isNull("latitude")) null else obj.getDouble("latitude"),
                longitude = if (obj.isNull("longitude")) null else obj.getDouble("longitude"),
                createdAt = obj.getLong("createdAt"),
                updatedAt = obj.getLong("updatedAt")
            )
            dao.insert(entry)
        }
    }
}
