package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.AchievementResponse
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.QuestResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ProgressLogEntry(
    @Json(name = "event_type")   val eventType: String,
    @Json(name = "xp_delta")     val xpDelta: Int,
    @Json(name = "level_before") val levelBefore: Int,
    @Json(name = "level_after")  val levelAfter: Int,
    @Json(name = "created_at")   val createdAt: String
)

interface HistoryApiService {
    @GET("quests/history")
    suspend fun getQuestHistory(): ApiEnvelope<List<QuestResponse>>

    @GET("me/progress")
    suspend fun getProgressLog(): ApiEnvelope<List<ProgressLogEntry>>

    @GET("me/achievements")
    suspend fun getAchievements(): ApiEnvelope<List<AchievementResponse>>
}