package com.example.headachetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HeadacheDao {

    @Insert
    suspend fun insert(entry: HeadacheEntry): Long

    @Update
    suspend fun update(entry: HeadacheEntry)

    @Delete
    suspend fun delete(entry: HeadacheEntry)

    @Query("SELECT * FROM headache_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HeadacheEntry>>

    @Query("SELECT * FROM headache_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): HeadacheEntry?

    @Query("SELECT * FROM headache_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    suspend fun getEntriesByDateRange(start: Long, end: Long): List<HeadacheEntry>

    @Query("SELECT * FROM headache_entries WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getEntriesByDateRangeFlow(start: Long, end: Long): Flow<List<HeadacheEntry>>

    @Query("SELECT COUNT(*) FROM headache_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT * FROM headache_entries ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEntry(): HeadacheEntry?

    @Query("DELETE FROM headache_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM headache_entries WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp ASC")
    suspend fun getEntriesWithLocation(): List<HeadacheEntry>
}
