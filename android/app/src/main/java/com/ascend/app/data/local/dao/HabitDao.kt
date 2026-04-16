package com.ascend.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ascend.app.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY cachedAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query("UPDATE habits SET completedToday = 1 WHERE id = :id")
    suspend fun markCompleted(id: String)
}