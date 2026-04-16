package com.ascend.app.data.repository

import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.local.dao.UserDao
import com.ascend.app.data.local.entity.UserEntity
import com.ascend.app.data.remote.api.AuthApiService
import com.ascend.app.data.remote.dto.toDomain
import com.ascend.app.domain.model.Result
import com.ascend.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: AuthApiService,
    private val dao: UserDao,
    private val tokenDataStore: TokenDataStore
) {
    fun observeUser(): Flow<User?> =
        dao.observe().map { it?.toDomain() }

    suspend fun refresh(): Result<Unit> {
        return try {
            val response = api.getMe()
            response.data?.let { dao.upsert(it.toEntity()) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenDataStore.clearToken()
        dao.clear()
    }

    fun hasToken(): Flow<Boolean> =
        tokenDataStore.accessToken.map { it != null }
}

private fun UserEntity.toDomain() = User(
    id = id, email = email, username = username,
    level = level, currentXp = currentXp,
    xpToNext = xpToNext, avatarUrl = avatarUrl
)

private fun com.ascend.app.data.remote.dto.UserResponse.toEntity() = UserEntity(
    id = id, email = email, username = username,
    level = level, currentXp = currentXp,
    xpToNext = xpToNext, avatarUrl = avatarUrl
)