package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.QuestResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface QuestApiService {
    @GET("quests")
    suspend fun getActiveQuests(): ApiEnvelope<List<QuestResponse>>

    @POST("quests/{id}/complete")
    suspend fun completeQuest(@Path("id") id: String): ApiEnvelope<Map<String, Int>>

    @POST("quests/{id}/skip")
    suspend fun skipQuest(@Path("id") id: String): ApiEnvelope<Unit>

    @POST("quests/generate")
    suspend fun generateQuests(): ApiEnvelope<List<QuestResponse>>
}