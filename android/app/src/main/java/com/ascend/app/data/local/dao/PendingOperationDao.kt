package com.ascend.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ascend.app.data.local.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(op: PendingOperationEntity): Long

    /** Oldest first — replay must preserve user action order. */
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<PendingOperationEntity>

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_operations SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun count(): Int

    @Query("DELETE FROM pending_operations")
    suspend fun clearAll()
}
