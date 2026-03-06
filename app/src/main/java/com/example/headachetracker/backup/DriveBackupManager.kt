package com.example.headachetracker.backup

import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.HeadacheEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupManager @Inject constructor(
    private val dao: HeadacheDao
) {

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val entries = dao.getEntriesByDateRange(0, Long.MAX_VALUE)
        val jsonArray = JSONArray()

        entries.forEach { entry ->
            jsonArray.put(JSONObject().apply {
                put("id", entry.id)
                put("painLevel", entry.painLevel)
                put("timestamp", entry.timestamp)
                put("notes", entry.notes ?: JSONObject.NULL)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
            })
        }

        JSONObject().apply {
            put("version", 1)
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
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            dao.insert(entry)
        }
    }
}
