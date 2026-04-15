package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuestResponse(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: Int,
    @Json(name = "xp_reward") val xpReward: Int,
    val status: String,
    @Json(name = "skill_area") val skillArea: String,
    @Json(name = "is_ai_generated") val isAiGenerated: Boolean
)

@JsonClass(generateAdapter = true)
data class HabitResponse(
    val id: String,
    val title: String,
    val frequency: String,
    @Json(name = "xp_reward") val xpReward: Int,
    @Json(name = "current_streak") val currentStreak: Int,
    @Json(name = "longest_streak") val longestStreak: Int
)