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
class StreakReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: AscendNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // In a full implementation, you would query the local DB or DataStore here
        // to check if the user has incomplete habits for the day.
        // For now, we fire the notification.
        val item = com.ascend.app.notification.NotifItem(
            id = "streak_reminder_${System.currentTimeMillis()}",
            type = NotifType.STREAK_REMINDER,
            title = "Streak at Risk!",
            body = "You have incomplete daily habits. The System advises completion before midnight.",
            timestamp = System.currentTimeMillis(),
            actionRoute = "dashboard"
        )
        notificationManager.post(item, smallIconRes = com.ascend.app.R.drawable.ic_notification)
        return Result.success()
    }
}
