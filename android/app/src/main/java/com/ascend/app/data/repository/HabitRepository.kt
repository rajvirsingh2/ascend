package com.ascend.app.data.repository

import android.content.Context
import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.entity.HabitEntity
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.remote.api.HabitApiService
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.HabitResponse
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Result
import com.ascend.app.domain.model.Result.Error
import com.ascend.app.domain.model.Result.Success
import com.ascend.app.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val api: HabitApiService,
    private val dao: HabitDao,
    private val pendingDao: PendingOperationDao,
    @ApplicationContext private val context: Context
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
        } catch (e: IOException) {
            // Offline: apply optimistically and queue for replay.
            dao.markCompleted(id)
            pendingDao.insert(
                PendingOperationEntity(
                    type = PendingOperationEntity.TYPE_COMPLETE_HABIT,
                    targetId = id,
                    createdAt = System.currentTimeMillis()
                )
            )
            SyncWorker.requestSync(context)
            Success(CompletionResponse())
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

private fun HabitResponse.toEntity(): HabitEntity{
    val completedToday=lastCompletedAt?.let {
        try{
            val lastDate=java.time.Instant.parse(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            lastDate==java.time.LocalDate.now()
        }catch (e: Exception){false}
    }?:false
    return HabitEntity(
        id=id,
        title=title,
        frequency=frequency,
        xpReward=xpReward,
        currentStreak=currentStreak,
        longestStreak=longestStreak,
        completedToday=completedToday
    )
}