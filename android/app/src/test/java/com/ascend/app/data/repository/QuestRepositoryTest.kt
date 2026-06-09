package com.ascend.app.data.repository

import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.entity.QuestEntity
import com.ascend.app.data.remote.api.QuestApiService
import com.ascend.app.data.remote.api.SkipResponse
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.QuestDto
import com.ascend.app.domain.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class QuestRepositoryTest {

    private lateinit var api: QuestApiService
    private lateinit var dao: QuestDao
    private lateinit var repository: QuestRepository

    @Before
    fun setup() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = QuestRepository(api, dao)
    }

    @Test
    fun `refresh fetches active quests and syncs with Room database`() = runTest {
        // Arrange
        val mockQuests = listOf(
            QuestDto(
                id = "q-1",
                title = "100 Pushups",
                description = "Do 100 pushups today.",
                difficulty = "Medium",
                xpReward = 50,
                skillArea = "Strength",
                status = "active",
                questType = "daily",
                isAiGenerated = true
            )
        )
        coEvery { api.getActiveQuests() } returns ApiEnvelope(data = mockQuests, error = null)

        // Act
        val result = repository.refresh()

        // Assert
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.clearAll() }
        coVerify(exactly = 1) { 
            dao.upsertAll(match { list -> list.size == 1 && list.first().id == "q-1" }) 
        }
    }

    @Test
    fun `completeQuest updates local DB on success`() = runTest {
        // Arrange
        val mockResponse = CompletionResponse(xpAwarded = 50, xpAfter = 250, leveledUp = false)
        coEvery { api.completeQuest("q-1") } returns ApiEnvelope(data = mockResponse, error = null)

        // Act
        val result = repository.completeQuest("q-1")

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(50, (result as Result.Success).data.xpAwarded)
        coVerify { dao.updateStatus("q-1", "completed") }
    }

    @Test
    fun `skipQuest calls API and returns success result`() = runTest {
        // Arrange
        val mockSkipResponse = SkipResponse(hp_damage = 5, hp_after = 95, skips_used = 6, died = false)
        coEvery { api.skipQuest("q-1") } returns Response.success(mockSkipResponse)

        // Act
        val result = repository.skipQuest("q-1")

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(95, (result as Result.Success).data.hp_after)
    }
}
