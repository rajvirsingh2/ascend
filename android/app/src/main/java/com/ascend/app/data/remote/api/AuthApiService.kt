package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.LoginRequest
import com.ascend.app.data.remote.dto.RegisterRequest
import com.ascend.app.data.remote.dto.TokenResponse
import com.ascend.app.data.remote.dto.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiEnvelope<Map<String, String>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Unit

    @GET("me")
    suspend fun getMe(): ApiEnvelope<UserResponse>
}