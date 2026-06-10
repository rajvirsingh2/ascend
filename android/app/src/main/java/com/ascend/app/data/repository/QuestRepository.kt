package com.ascend.app.data.repository

import android.content.Context
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.local.entity.QuestEntity
import com.ascend.app.data.remote.api.QuestApiService
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.toDomain
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.Result
import com.ascend.app.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepository @Inject constructor(
    private val api: QuestApiService,
    private val dao: QuestDao,
    private val pendingDao: PendingOperationDao,
    @ApplicationContext private val context: Context
) {
    // UI observes this — always from Room
    fun observeActiveQuests(): Flow<List<Quest>> =
        dao.observeActive().map { entities ->
            entities.map { it.toDomain() }
        }

    // call this to sync from network into Room
    suspend fun refresh(): Result<Unit> {
        return try {
            val response = api.getActiveQuests()
            val quests = response.data ?: emptyList()
            dao.clearAll()                          // wipe stale quests before replacing
            dao.upsertAll(quests.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    // call on logout to prevent stale data appearing for next user
    suspend fun clearLocalCache() {
        dao.clearAll()
        // Never replay the previous user's queued writes under a new token.
        pendingDao.clearAll()
    }


    suspend fun completeQuest(id: String): Result<CompletionResponse> {
        return try {
            val response = api.completeQuest(id)
            if (response.data != null) {
                dao.updateStatus(id, "completed")
                Result.Success(response.data)
            } else {
                Result.Error(response.error ?: "Failed")
            }
        } catch (e: IOException) {
            // Offline: apply optimistically, queue for replay when network
            // returns. XP/level data arrives later via sync + WS events.
            dao.updateStatus(id, "completed")
            enqueue(PendingOperationEntity.TYPE_COMPLETE_QUEST, id)
            Result.Success(CompletionResponse())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    private suspend fun enqueue(type: String, targetId: String) {
        pendingDao.insert(
            PendingOperationEntity(
                type = type,
                targetId = targetId,
                createdAt = System.currentTimeMillis()
            )
        )
        SyncWorker.requestSync(context)
    }

    suspend fun skipQuest(id: String): Result<com.ascend.app.data.remote.api.SkipResponse> {
        return try {
            val response = api.skipQuest(id)
            if (response.isSuccessful) {
                dao.updateStatus(id, "skipped")
                val body = response.body()
                if (body != null && body.data != null) {
                    Result.Success(body.data)
                } else {
                    // Fallback if the server returned 204 or an empty body
                    Result.Success(com.ascend.app.data.remote.api.SkipResponse())
                }
            } else {
                Result.Error("Failed to skip: ${response.code()}")
            }
        } catch (e: IOException) {
            dao.updateStatus(id, "skipped")
            enqueue(PendingOperationEntity.TYPE_SKIP_QUEST, id)
            Result.Success(com.ascend.app.data.remote.api.SkipResponse())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun generateQuests(): Result<Unit> {
        return try {
            val response = api.generateQuests()
            val quests = response.data ?: emptyList()
            // Append new quests to the existing ones
            dao.upsertAll(quests.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getHeatmap(): Result<List<com.ascend.app.data.remote.dto.HeatmapPointResponse>> {
        return try {
            val response = api.getHeatmap()
            Result.Success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

private fun QuestEntity.toDomain() = com.ascend.app.data.remote.dto.QuestResponse(
    id = id, title = title, description = description,
    type = type, difficulty = difficulty, xpReward = xpReward,
    status = status, skillArea = skillArea, isAiGenerated = isAiGenerated
).toDomain()

private fun com.ascend.app.data.remote.dto.QuestResponse.toEntity() = QuestEntity(
    id = id, title = title, description = description,
    type = type, difficulty = difficulty, xpReward = xpReward,
    status = status, skillArea = skillArea, isAiGenerated = isAiGenerated
)