package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AvatarUploadRequest(@Json(name = "image_base64") val imageBase64: String)

@JsonClass(generateAdapter = true)
data class AchievementResponse(
    val key: String, val title: String,
    val tag: String, val icon: String,
    val earned: Boolean,
    @Json(name = "earned_at") val earnedAt: String? = null
)