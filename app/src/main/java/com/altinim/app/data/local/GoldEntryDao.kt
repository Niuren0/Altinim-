package com.altinim.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldEntryDao {

    @Insert
    suspend fun insert(entry: GoldEntry)

    @Delete
    suspend fun delete(entry: GoldEntry)

    @Query("SELECT * FROM gold_entries ORDER BY dateMillis DESC")
    fun getAll(): Flow<List<GoldEntry>>
}