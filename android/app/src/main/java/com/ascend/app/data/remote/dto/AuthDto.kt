package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val level: Int,
    @Json(name = "current_xp") val currentXp: Int,
    @Json(name = "xp_to_next") val xpToNext: Int,
    @Json(name="total_xp") val totalXp: Int = 0,
    val hp: Int = 100,
    @Json(name="max_hp") val maxHp: Int = 100,
    val strength: Int = 0,
    val agility: Int = 0,
    val mana: Int = 0,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(
    val data: T?,
    val error: String?
)

@JsonClass(generateAdapter = true)
data class VerifyEmailRequest(val email: String, val otp: String)

@JsonClass(generateAdapter = true)
data class ResendOtpRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(val email: String)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    @Json(name = "new_password") val newPassword: String
)