package com.ascend.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ascend.app.MainActivity
import com.ascend.app.R
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.UserApiService
import com.ascend.app.data.remote.dto.FCMTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AscendFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var userApi: UserApiService
    @Inject lateinit var tokenDataStore: TokenDataStore

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // register with backend when token refreshes
        scope.launch {
            val accessToken = tokenDataStore.accessToken.firstOrNull()
            if (accessToken != null) {
                try { userApi.registerFCMToken(FCMTokenRequest(token)) }
                catch (e: Exception) { /* retry on next launch */ }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type    = message.data["type"] ?: ""
        val title   = message.notification?.title ?: "Ascend"
        val body    = message.notification?.body  ?: ""

        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val channelId = when (type) {
            "PUNISHMENT" -> "ascend_punishment"
            "REMINDER"   -> "ascend_reminder"
            else         -> "ascend_general"
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (type) {
                "PUNISHMENT" -> "Quest Punishment"
                "REMINDER"   -> "Daily Reminders"
                else         -> "General"
            }
            val importance = when (type) {
                "PUNISHMENT" -> NotificationManager.IMPORTANCE_HIGH
                else         -> NotificationManager.IMPORTANCE_DEFAULT
            }
            manager.createNotificationChannel(
                NotificationChannel(channelId, channelName, importance)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (type == "PUNISHMENT") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}