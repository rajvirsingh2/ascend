package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class SaveApiKeyRequest(
    val provider: String,
    @Json(name = "api_key") val apiKey: String,
    val model: String = ""
)

@JsonClass(generateAdapter = true)
data class ApiKeyStatusResponse(
    @Json(name = "has_key") val hasKey: Boolean
)

interface SettingsApiService {
    @POST("settings/api-key")
    suspend fun saveApiKey(@Body request: SaveApiKeyRequest): ApiEnvelope<Map<String, String>>

    @GET("settings/api-key/status")
    suspend fun getKeyStatus(): ApiEnvelope<ApiKeyStatusResponse>

    @DELETE("settings/api-key")
    suspend fun deleteApiKey(): Unit
}