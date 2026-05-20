package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.CategoriesResponseDto
import com.ascend.app.data.remote.dto.SaveInterestsRequestDto
import com.ascend.app.data.remote.dto.SaveInterestsResponseDto
import com.ascend.app.data.remote.dto.UserInterestsResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface InterestsApiService{
    @GET("interests/categories")
    suspend fun getCategories(): ApiEnvelope<CategoriesResponseDto>

    @GET("interests")
    suspend fun getMyInterests(): ApiEnvelope<UserInterestsResponseDto>

    @POST("interests")
    suspend fun saveInterests(@Body request: SaveInterestsRequestDto): ApiEnvelope<SaveInterestsResponseDto>
}