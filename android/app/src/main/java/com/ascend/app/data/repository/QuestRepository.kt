package com.ascend.app.data.repository

import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.entity.QuestEntity
import com.ascend.app.data.remote.api.QuestApiService
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.toDomain
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepository @Inject constructor(
    private val api: QuestApiService,
    private val dao: QuestDao
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
            dao.upsertAll(quests.map { it.toEntity() })
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
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
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun skipQuest(id: String): Result<Unit> {
        return try {
            api.skipQuest(id)
            dao.updateStatus(id, "skipped")
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun generateQuests(): Result<Unit> {
        return try {
            val response = api.generateQuests()
            val quests = response.data ?: emptyList()
            dao.upsertAll(quests.map { it.toEntity() })
            Result.Success(Unit)
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