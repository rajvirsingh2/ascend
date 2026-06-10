package com.ascend.app.notification

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

        // Backend sends data-only messages, so this runs in foreground AND
        // background and we control the tray notification + tap intent.
        val data = message.data
        if (data.isEmpty()) return

        val id = data["id"] ?: System.currentTimeMillis().toString()
        val type = parseNotifType(data["type"])
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

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setData(Uri.parse("ascend://${route ?: "notifications"}"))
            setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        notifManager.post(item, intent, com.ascend.app.R.drawable.ic_notification)

        // Midnight quest drop: pull the new quests into Room immediately so
        // they're already on the dashboard when the user opens the app.
        // WorkManager survives this service being killed mid-request.
        if (type == NotifType.DAILY_QUEST) {
            com.ascend.app.workers.QuestRefreshWorker.enqueue(applicationContext)
        }
    }

    /** Accepts NotifType enum names plus legacy backend strings. */
    private fun parseNotifType(raw: String?): NotifType {
        if (raw == null) return NotifType.SYSTEM
        runCatching { return NotifType.valueOf(raw.uppercase()) }
        return when (raw.lowercase()) {
            "level_up" -> NotifType.LEVEL_UP
      