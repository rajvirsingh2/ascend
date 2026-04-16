package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.HabitResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

import com.ascend.app.data.remote.dto.CompletionResponse

interface HabitApiService {
    @GET("habits")
    suspend fun getHabits(): ApiEnvelope<List<HabitResponse>>

    @POST("habits/{id}/complete")
    suspend fun completeHabit(@Path("id") id: String): ApiEnvelope<CompletionResponse>
}