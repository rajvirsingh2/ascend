package com.ascend.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponse(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String,
    @SerialName("xp_delta") val xpDelta: Int? = null,
    @SerialName("action_route") val actionRoute: String? = null,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class NotificationListResponse(
    val data: List<NotificationResponse>
)
