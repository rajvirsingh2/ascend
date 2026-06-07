package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.QuestResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

import com.ascend.app.data.remote.dto.CompletionResponse

import com.squareup.moshi.JsonClass
import com.ascend.app.data.remote.dto.StatDeltaResponse

@JsonClass(generateAdapter = true)
data class SkipResponse(
    val hp_damage: Int,
    val hp_after: Int,
    val skips_used: Int,
    val died: Boolean,
    val stat_deltas: List<StatDeltaResponse>?
)

interface QuestApiService {
    @GET("quests")
    suspend fun getActiveQuests(): ApiEnvelope<List<QuestResponse>>

    @POST("quests/{id}/complete")
    suspend fun completeQuest(@Path("id") id: String): ApiEnvelope<CompletionResponse>

    @POST("quests/{id}/skip")
    suspend fun skipQuest(@Path("id") id: String): ApiEnvelope<SkipResponse>

    @POST("quests/generate")
    suspend fun generateQuests(): ApiEnvelope<List<QuestResponse>>

    @GET("quests/heatmap")
    suspend fun getHeatmap(): ApiEnvelope<List<com.ascend.app.data.remote.dto.HeatmapPointResponse>>
}