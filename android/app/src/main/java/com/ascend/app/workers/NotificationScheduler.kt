package com.ascend.app.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val STREAK_WORK_NAME = "StreakReminderWork"
    private const val QUEST_WORK_NAME = "QuestDropWork"

    fun scheduleLocalNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Streak Reminder (e.g., everyday at 8:00 PM)
        val streakInitialDelay = calculateInitialDelay(20, 0) // 20:00 (8 PM)
        val streakWorkRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(streakInitialDelay, TimeUnit.MILLISECONDS)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            STREAK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            streakWorkRequest
        )

        // 2. Quest Drop (e.g., everyday at 8:00 AM)
        val questInitialDelay = calculateInitialDelay(8, 0) // 08:00 AM
        val questWorkRequest = PeriodicWorkRequestBuilder<QuestDropWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(questInitialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            QUEST_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            questWorkRequest
        )
    }

    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
