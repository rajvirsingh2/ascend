package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.CreateGoalRequest
import com.ascend.app.data.remote.dto.GoalResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GoalApiService {
    @GET("goals")
    suspend fun getGoals(): ApiEnvelope<List<GoalResponse>>

    @POST("goals")
    suspend fun createGoal(@Body request: CreateGoalRequest): ApiEnvelope<GoalResponse>

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: String): Unit
}