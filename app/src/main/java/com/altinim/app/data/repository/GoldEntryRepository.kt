package com.altinim.app.data.repository

import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.local.GoldEntryDao
import kotlinx.coroutines.flow.Flow

class GoldEntryRepository(
    private val dao: GoldEntryDao
) {
    fun getAllEntries(): Flow<List<GoldEntry>> = dao.getAll()

    suspend fun addEntry(entry: GoldEntry) = dao.insert(entry)

    suspend fun deleteEntry(entry: GoldEntry) = dao.delete(entry)
}