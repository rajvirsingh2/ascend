package com.ascend.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ascend.app.notification.AscendNotificationManager
import com.ascend.app.notification.NotifType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class QuestDropWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: AscendNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Trigger a system notification for daily quests dropping.
        val item = com.ascend.app.notification.NotifItem(
            id = "quest_drop_${System.currentTimeMillis()}",
            type = NotifType.DAILY_QUEST,
            title = "Daily Quests Dropped!",
            body = "The System has issued your daily missions. Complete them to earn XP.",
            timestamp = System.currentTimeMillis(),
            actionRoute = "dashboard"
        )
        notificationManager.post(item, smallIconRes = com.ascend.app.R.drawable.ic_notification)
        return Result.success()
    }
}
