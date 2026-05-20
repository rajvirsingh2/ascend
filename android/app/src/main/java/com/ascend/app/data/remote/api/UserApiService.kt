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
}