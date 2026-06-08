package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.SavePhysiqueRequest
import retrofit2.http.Body
import retrofit2.http.POST


interface PhysiqueApiService {
    @POST("physique")
    suspend fun savePhysique(@Body request: SavePhysiqueRequest): ApiEnvelope<Map<String, Any>>

    @POST("physique/generate-quests")
    suspend fun generateExerciseQuests(): ApiEnvelope<Map<String, String>>
}