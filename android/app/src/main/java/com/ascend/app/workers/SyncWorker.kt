package com.ascend.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.remote.api.HabitApiService
import com.ascend.app.data.remote.api.QuestApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Replays writes queued while the device was offline (quest complete/skip,
 * habit complete). Runs only with network, retries with exponential backoff.
 *
 * Replay is at-least-once: the backend rejects duplicate completions, and an
 * HTTP error response (as opposed to a transport failure) means the op was
 * received and judged — so it is dropped, never retried.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingDao: PendingOperationDao,
    private val questApi: QuestApiService,
    private val habitApi: HabitApiService,
    private val questDao: QuestDao,
    private val habitDao: HabitDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ops = pendingDao.getAll()
        if (ops.isEmpty()) return Result.success()

        var transportFailure = false
        for (op in ops) {
            if (op.attempts >= PendingOperationEntity.MAX_ATTEMPTS) {
                Log.w(TAG, "dropping op ${op.type}/${op.targetId} after ${op.attempts} attempts")
                pendingDao.delete(op.id)
                continue
            }
            try {
                replay(op)
                pendingDao.delete(op.id)
            } catch (e: IOException) {
                // Still offline / flaky network: keep the op, retry the whole
                // batch later. Stop here to preserve ordering.
                pendingDao.incrementAttempts(op.id)
                transportFailure = true
                break
            } catch (e: Exception) {
                // Server answered (e.g. HttpException 4xx: already completed,
                // expired quest) or response was malformed. Replaying cannot
                // succeed — drop the op and reconcile local state via refresh.
                Log.w(TAG, "op ${op.type}/${op.targetId} rejected: ${e.message}")
                pendingDao.delete(op.id)
            }
        }
        return if (transportFailure) Result.retry() else Result.success()
    }

    private suspend fun replay(op: PendingOperationEntity) {
        when (op.type) {
            PendingOperationEntity.TYPE_COMPLETE_QUEST -> {
                questApi.completeQuest(op.targetId)
                questDao.updateStatus(op.targetId, "completed")
            }
            PendingOperationEntity.TYPE_SKIP_QUEST -> {
                val response = questApi.skipQuest(op.targetId)
                if (!response.isSuccessful) {
                    // Server judged the request — treat like a rejection.
                    throw IllegalStateException("skip rejected: ${response.code()}")
                }
                questDao.updateStatus(op.targetId, "skipped")
            }
            PendingOperationEntity.TYPE_COMPLETE_HABIT -> {
                habitApi.completeHabit(op.targetId)
                habitDao.markCompleted(op.targetId)
            }
            else -> Log.w(TAG, "unknown pending op type: ${op.type}")
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_NAME = "ascend_pending_sync"

        /**
         * Schedules a replay as soon as the device has connectivity.
         * KEEP: if a sync is already queued, the new ops ride along with it.
         */
        fun requestSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
