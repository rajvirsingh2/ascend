package com.ascend.app.data.repository

import com.ascend.app.data.remote.api.InterestsApiService
import com.ascend.app.data.remote.dto.SaveInterestsRequestDto
import com.ascend.app.data.remote.dto.toDomain
import com.ascend.app.data.remote.dto.toDto
import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.UserInterest
import com.ascend.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class InterestsRepository @Inject constructor(
    private val api: InterestsApiService
) {
    suspend fun getCategories(): Result<List<InterestCategory>> = runCatching {
        api.getCategories().data!!.categories.map { it.toDomain() }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message?:"Failed to load categories")}
    )
    suspend fun getMyInterests(): Result<Pair<Boolean,List<UserInterest>>> = runCatching {
        val response=api.getMyInterests().data!!
        Pair(response.configured,response.interests.map { it.toDomain() })
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message?:"Failed to load interests") }
    )
    suspend fun saveInterests(interests:List<UserInterest>): Result<Unit> = runCatching {
        api.saveInterests(SaveInterestsRequestDto(interests=interests.map { it.toDto() }))
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(it.message?:"Failed to save interests") }
    )
}