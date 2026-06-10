package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.AchievementResponse
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.AvatarUploadRequest
import com.ascend.app.data.remote.dto.FCMTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApiService{
    @POST("me/avatar")
    suspend fun uploadAvatar(@Body req: AvatarUploadRequest): ApiEnvelope<Map<String, String>>
    @GET("me/achievements")
    suspend fun getAchievements(): ApiEnvelope<List<AchievementResponse>>

    @POST("me/fcm-token")
    suspend fun registerFCMToken(@Body req: FCMTokenRequest): ApiEnvelope<Unit>

    @GET("me/stats")
    suspend fun getStats(): ApiEnvelope<StatsResponse>
}

data class XpHistoryPointResponse(
    val date: String,
    val xp: Int
)

data class SkillCountResponse(
    val skill_area: String,
    val count: Int
)

data class StatsResponse(
    val total_xp: Int,
    val level: Int,
    val total_quests: Int,
    val habits_completed: Int,
    val streak_freezes: Int,
    val current_streak: Int = 0,
    val best_streak: Int,
    val xp_history: List<XpHistoryPointResponse>,
    val quest_distribution: List<SkillCountResponse>,
    val quests_this_week: Int,
    val quests_skipped: Int,
    val on_time_percentage: Float
)