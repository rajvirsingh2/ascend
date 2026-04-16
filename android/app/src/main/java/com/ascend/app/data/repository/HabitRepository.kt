package com.ascend.app.data.repository

import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.entity.HabitEntity
import com.ascend.app.data.remote.api.HabitApiService
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.toDomain
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Result
import com.ascend.app.domain.model.Result.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val api: HabitApiService,
    private val dao: HabitDao
) {
    fun observeHabits(): Flow<List<Habit>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh(): Result<Unit> {
        return try {
            val response = api.getHabits()
            val habits = response.data ?: emptyList()
            dao.upsertAll(habits.map { it.toEntity() })
            Success(Unit)
        } catch (e: Exception) {
            Error(e.message ?: "Network error")
        }
    }


    suspend fun completeHabit(id: String): Result<CompletionResponse> {
        return try {
            val response = api.completeHabit(id)
            if (response.data != null) {
                dao.markCompleted(id)
                Success(response.data)
            } else {
                Error(response.error ?: "Failed")
            }
        } catch (e: Exception) {
            Error(e.message ?: "Network error")
        }
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id, title = title, frequency = frequency,
    xpReward = xpReward, currentStreak = currentStreak,
    longestStreak = longestStreak, completedToday = completedToday
)

private fun com.ascend.app.data.remote.dto.HabitResponse.toEntity() = HabitEntity(
    id = id, title = title, frequency = frequency, xpReward = xpReward,
    currentStreak = currentStreak, longestStreak = longestStreak,
    completedToday = false
)