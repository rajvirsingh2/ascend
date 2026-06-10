package com.ascend.app.data.repository

import com.ascend.app.data.local.AscendDatabase
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.local.dao.UserDao
import com.ascend.app.data.local.entity.UserEntity
import com.ascend.app.data.remote.api.AuthApiService
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.UserResponse
import com.ascend.app.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var api: AuthApiService
    private lateinit var dao: UserDao
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var database: AscendDatabase
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        api = mockk()
        dao = mockk(relaxed = true)
        tokenDataStore = mockk(relaxed = true)
        database = mockk(relaxed = true)

        repository = UserRepository(api, dao, tokenDataStore, database)
    }

    @Test
    fun `refresh fetches user and saves to database`() = runTest {
        // Arrange
        val mockResponse = UserResponse(
            id = "user-123",
            email = "test@example.com",
            username = "shadow_monarch",
            level = 10,
            currentXp = 500,
            xpToNext = 1000,
            hp = 85,
            maxHp = 100
        )
        coEvery { api.getMe() } returns ApiEnvelope(data = mockResponse, error = null)

        // Act
        val result = repository.refresh()

        // Assert
        assertTrue(result is Result.Success)
        coVerify { 
            dao.upsert(match { 
                it.id == "user-123" && it.hp == 85 && it.username == "shadow_monarch"
            }) 
        }
    }

    @Test
    fun `refresh returns Error on network failure`() = runTest {
        // Arrange
        coEvery { api.getMe() } throws RuntimeException("Network timeout")

        // Act
        val result = repository.refresh()

        // Assert
        assertTrue(result is Result.Error)
        assertEquals("Network timeout", (result as Result.Error).message)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `logout clears token and database`() = runTest {
        // Arrange
        coEvery { api.logout() } returns Unit

        // Act
        repository.logout()

        // Assert
        coVerify { tokenDataStore.clearToken() }
        coVerify { database.clearAllTables() }
    }
}
