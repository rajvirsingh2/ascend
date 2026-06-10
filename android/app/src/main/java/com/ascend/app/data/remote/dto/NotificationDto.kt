package com.ascend.app.data.remote.dto

import com.ascend.app.notification.NotifItem
import com.ascend.app.notification.NotifType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire format of GET /api/v1/notifications. Kept separate from [NotifItem]
 * (the UI model): the backend sends snake_case keys, a string type, and an
 * RFC 3339 timestamp — none of which map onto NotifItem directly.
 */
@JsonClass(generateAdapter = true)
data class NotificationResponse(
    val id: String,
    @Json(name = "user_id") val userId: String? = null,
    val type: String,
    val title: String,
    val body: String,
    @Json(name = "xp_delta") val xpDelta: Int? = null,
    @Json(name = "action_route") val actionRoute: String? = null,
    @Json(name = "is_read") val isRead: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationListResponse(
    val data: List<NotificationResponse> = emptyList()
)

fun NotificationResponse.toNotifItem() = NotifItem(
    id = id,
    type = runCatching { NotifType.valueOf(type.uppercase()) }
        .getOrDefault(NotifType.SYSTEM),
    title = title,
    body = body,
    timestamp = parseTimestamp(createdAt),
    isRead = isRead,
    actionRoute = actionRoute,
    xpDelta = xpDelta
)

private fun parseTimestamp(raw: String?): Long {
    if (raw == null) return System.currentTimeMillis()
    return runCatching { java.time.Instant.parse(raw).toEpochMilli() }
        .recoverCatching { java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
}
