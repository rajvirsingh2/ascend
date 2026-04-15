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
    @Json(name = "avatar_url") val avatarUrl: String?
)

@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(
    val data: T?,
    val error: String?
)