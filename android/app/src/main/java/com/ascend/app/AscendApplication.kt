package com.ascend.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ascend.app.workers.NotificationScheduler
import javax.inject.Inject

@HiltAndroidApp
class AscendApplication: Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Initialize periodic local notifications
        NotificationScheduler.scheduleLocalNotifications(this)
    }
}