package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.ForgotPasswordRequest
import com.ascend.app.data.remote.dto.LoginRequest
import com.ascend.app.data.remote.dto.RegisterRequest
import com.ascend.app.data.remote.dto.ResendOtpRequest
import com.ascend.app.data.remote.dto.ResetPasswordRequest
import com.ascend.app.data.remote.dto.TokenResponse
import com.ascend.app.data.remote.dto.UserResponse
import com.ascend.app.data.remote.dto.VerifyEmailRequest
import com.squareup.moshi.JsonClass
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

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): ApiEnvelope<Map<String, String>>

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtpRequest): ApiEnvelope<Map<String, String>>

    @JsonClass(generateAdapter = true)
    data class DeleteAccountRequest(val password: String)

    @POST("me/delete")
    suspend fun requestDeletion(@Body req: DeleteAccountRequest): ApiEnvelope<Map<String, Any>>

    @POST("me/cancel-delete")
    suspend fun cancelDeletion(): ApiEnvelope<Map<String, String>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body req: ForgotPasswordRequest): ApiEnvelope<Map<String, String>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body req: ResetPasswordRequest): ApiEnvelope<Map<String, String>>
}