package com.example.headachetracker.data.repository

import com.example.headachetracker.data.local.HeadacheDao
import com.example.headachetracker.data.local.HeadacheEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeadacheRepository @Inject constructor(
    private val dao: HeadacheDao
) {

    fun getAllEntries(): Flow<List<HeadacheEntry>> = dao.getAllEntries()

    suspend fun getEntryById(id: Long): HeadacheEntry? = dao.getEntryById(id)

    suspend fun getEntriesByDateRange(start: Long, end: Long) =
        dao.getEntriesByDateRange(start, end)

    fun getEntriesByDateRangeFlow(start: Long, end: Long) =
        dao.getEntriesByDateRangeFlow(start, end)

    suspend fun insertEntry(entry: HeadacheEntry): Long = dao.insert(entry)

    suspend fun updateEntry(entry: HeadacheEntry) =
        dao.update(entry.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteEntry(entry: HeadacheEntry) = dao.delete(entry)

    suspend fun deleteEntryById(id: Long) = dao.deleteById(id)

    suspend fun getEntryCount(): Int = dao.getEntryCount()

    suspend fun getLatestEntry(): HeadacheEntry? = dao.getLatestEntry()
}
