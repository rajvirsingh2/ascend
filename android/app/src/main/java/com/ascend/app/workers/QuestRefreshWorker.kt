package com.ascend.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ascend.app.data.repository.QuestRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pulls fresh quests from the API into Room. Enqueued when the midnight
 * DAILY_QUEST push arrives, so the dashboard is already up to date when the
 * user opens the app. WorkManager (not a raw coroutine) because the FCM
 * service process may be killed before an in-flight request completes.
 */
@HiltWorker
class QuestRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val questRepository: QuestRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return when (questRepository.refresh()) {
            is com.ascend.app.domain.model.Result.Success -> Result.success()
            else -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<QuestRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("ascend_quest_refresh", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
