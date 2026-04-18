package com.ascend.app.data.realtime

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WsEvent {
    data class LevelUp(val newLevel: Int, val xpAwarded: Int) : WsEvent
    data class GuildQuestCompleted(val memberName: String,
                                   val questTitle: String,
                                   val rarity: String) : WsEvent
    data class XpAwarded(val amount: Int) : WsEvent
    data object Disconnected : WsEvent
}

@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val tokenDataStore: com.ascend.app.data.local.TokenDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var retryCount = 0

    fun connect(baseUrl: String) {
        scope.launch {
            val token = tokenDataStore.accessToken.firstOrNull() ?: return@launch
            val wsUrl = baseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://") + "/api/v1/ws"

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    retryCount = 0
                    Log.i("WS", "connected")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    parseAndEmit(text)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e("WS", "failure: ${t.message}")
                    _events.tryEmit(WsEvent.Disconnected)
                    scheduleReconnect(baseUrl)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    _events.tryEmit(WsEvent.Disconnected)
                }
            })
        }
    }

    private fun parseAndEmit(text: String) {
        try {
            val adapter = moshi.adapter(Map::class.java)
            val frame = adapter.fromJson(text) ?: return
            val type = frame["type"] as? String ?: return
            @Suppress("UNCHECKED_CAST")
            val payload = frame["payload"] as? Map<String, Any> ?: emptyMap<String, Any>()

            val event = when (type) {
                "LEVEL_UP" -> WsEvent.LevelUp(
                    newLevel = (payload["new_level"] as? Double)?.toInt() ?: 0,
                    xpAwarded = (payload["xp_awarded"] as? Double)?.toInt() ?: 0
                )
                "GUILD_QUEST" -> WsEvent.GuildQuestCompleted(
                    memberName = payload["member_name"] as? String ?: "",
                    questTitle = payload["quest_title"] as? String ?: "",
                    rarity = payload["rarity"] as? String ?: "common"
                )
                "XP_AWARDED" -> WsEvent.XpAwarded(
                    amount = (payload["amount"] as? Double)?.toInt() ?: 0
                )
                else -> null
            } ?: return

            _events.tryEmit(event)
        } catch (e: Exception) {
            Log.e("WS", "parse error: ${e.message}")
        }
    }

    private fun scheduleReconnect(baseUrl: String) {
        val delayMs = minOf(1000L * (1 shl retryCount), 30_000L) // exponential, max 30s
        retryCount++
        scope.launch {
            delay(delayMs)
            connect(baseUrl)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "user logout")
        webSocket = null
    }
}