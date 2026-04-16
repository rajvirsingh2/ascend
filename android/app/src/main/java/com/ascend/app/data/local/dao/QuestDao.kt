package com.ascend.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ascend.app.data.local.entity.QuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests WHERE status = 'active' ORDER BY cachedAt DESC")
    fun observeActive(): Flow<List<QuestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quests: List<QuestEntity>)

    @Query("UPDATE quests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM quests WHERE status != 'active'")
    suspend fun clearCompleted()
}