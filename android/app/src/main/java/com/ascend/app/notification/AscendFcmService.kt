package com.ascend.app.notification

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AscendFcmService : FirebaseMessagingService() {

    @Inject lateinit var notifManager: AscendNotificationManager
    @Inject lateinit var userApi: com.ascend.app.data.remote.api.UserApiService

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            try {
                userApi.registerFCMToken(com.ascend.app.data.remote.dto.FCMTokenRequest(token))
            } catch (e: Exception) {
                // Token sync failed, maybe no active session. Will retry on next login.
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Only handle data payloads to maintain full control (no standard title/body keys)
        var data = message.data
        if (data.isEmpty()) return

        val id = data["id"] ?: System.currentTimeMillis().toString()
        val typeStr = data["type"] ?: "SYSTEM"
        val type = runCatching { NotifType.valueOf(typeStr) }.getOrDefault(NotifType.SYSTEM)
        val title = data["title"] ?: "Ascend"
        val body = data["body"] ?: ""
        val route = data["action_route"]
        val xpDelta = data["xp_delta"]?.toIntOrNull()

        val item = NotifItem(
            id = id,
            type = type,
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            actionRoute = route,
            xpDelta = xpDelta
        )

        scope.launch {
            // 1. Persist it locally
            // repo.insert(item)

            // 2. Post to system tray (if app is in background, or even foreground depending on preference)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("ascend://${route ?: "notifications"}") as Map<String?, String?>
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            // Use your vector drawable for smallIconRes
            notifManager.post(item, intent, android.R.drawable.ic_popup_reminder)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
