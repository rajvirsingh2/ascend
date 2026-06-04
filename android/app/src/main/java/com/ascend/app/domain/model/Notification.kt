package com.ascend.app.domain.model

data class Notification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val xpDelta: Int?,
    val actionRoute: String?,
    val isRead: Boolean,
    val createdAt: String
)
