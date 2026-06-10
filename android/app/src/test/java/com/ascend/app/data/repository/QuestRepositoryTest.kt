package com.ascend.app.data.repository

import android.content.Context
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.remote.api.QuestApiService
import com.ascend.app.data.remote.api.SkipResponse
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.CompletionResponse
import com.ascend.app.data.remote.dto.QuestResponse
import com.ascend.app.domain.model.Result
import com.ascend.app.workers.SyncWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class QuestRepositoryTest {

    private lateinit var api: QuestApiService
    private lateinit var dao: QuestDao
    private lateinit var pendingDao: PendingOperationDao
    private lateinit var context: Context
    private lateinit var repository: QuestRepository

    @Before
    fun setup() {
        api = mockk()
        dao = mockk(relaxed = true)
        pendingDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        repository = QuestRepository(api, dao, pendingDao, context)

        // WorkManager is not available on the JVM — stub the scheduling call.
        mockkObject(SyncWorker.Companion)
        justRun { SyncWorker.requestSync(any()) }
    }

    @After
    fun tearDown() {
        unmockkObject(SyncWorker.Companion)
    }

    private fun quest(id: String) = QuestResponse(
        id = id,
        title = "100 Pushups",
        description = "Do 100 pushups today.",
        type = "daily",
        difficulty = 3,
        xpReward = 50,
        status = "active",
        skillArea = "Strength",
        isAiGenerated = true
    )

    @Test
    fun `refresh fetches active quests and syncs with Room database`() = runTest {
        coEvery { api.getActiveQuests() } returns ApiEnvelope(data = listOf(quest("q-1")), error = null)

        val result = repository.refresh()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.clearAll() }
        coVerify(exactly = 1) {
            dao.upsertAll(match { list -> list.size == 1 && list.first().id == "q-1" })
        }
    }

    @Test
    fun `completeQuest updates local DB on success`() = runTest {
        val mockResponse = CompletionResponse(xpAwarded = 50, xpAfter = 250, leveledUp = false)
        coEvery { api.completeQuest("q-1") } returns ApiEnvelope(data = mockResponse, error = null)

        val result = repository.completeQuest("q-1")

        assertTrue(result is Result.Success)
        assertEquals(50, (result as Result.Success).data.xpAwarded)
        coVerify { dao.updateStatus("q-1", "completed") }
        coVerify(exactly = 0) { pendingDao.insert(any()) }
    }

    @Test
    fun `completeQuest offline applies optimistic update and queues replay`() = runTest {
        coEvery { api.completeQuest("q-1") } throws IOException("no network")

        val result = repository.completeQuest("q-1")

        // Optimistic success so the UI marks the quest done immediately.
        assertTrue(result is Result.Success)
        coVerify { dao.updateStatus("q-1", "completed") }
        coVerify(exactly = 1) {
            pendingDao.insert(match {
                it.type == PendingOperationEntity.TYPE_COMPLETE_QUEST && it.targetId == "q-1"
            })
        }
        io.mockk.verify(exactly = 1) { SyncWorker.requestSync(any()) }
    }

    @Test
    fun `completeQuest server rejection is an error and is NOT queued`() = runTest {
        coEvery { api.completeQuest("q-1") } returns ApiEnvelope(data = null, error = "already completed")

        val result = repository.completeQuest("q-1")

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { pendingDao.insert(any()) }
        coVerify(exactly = 0) { dao.updateStatus(any(), any()) }
    }

    @Test
    fun `skipQuest calls API and returns success result`() = runTest {
        val envelope = ApiEnvelope(
            data = SkipResponse(hp_damage = 5, hp_after = 95, skips_used = 6, died = false),
            error = null
        )
        coEvery { api.skipQuest("q-1") } returns Response.success(envelope)

        val result = repository.skipQuest("q-1")

        assertTrue(result is Result.Success)
        assertEquals(95, (result as Result.Success).data.hp_after)
        coVerify { dao.updateStatus("q-1", "skipped") }
    }

    @Test
    fun `skipQuest offline applies optimistic update and queues replay`() = runTest {
        coEvery { api.skipQuest("q-1") } throws IOException("no network")

        val result = repository.skipQuest("q-1")

        assertTrue(result is Result.Success)
        coVerify { dao.updateStatus("q-1", "skipped") }
        coVerify(exactly = 1) {
            pendingDao.insert(match {
                it.type == PendingOperationEntity.TYPE_SKIP_QUEST && it.targetId == "q-1"
            })
        }
    }

    @Test
    fun `clearLocalCache also clears the pending queue`() = runTest {
        repository.clearLocalCache()

        coVerify(exactly = 1) { dao.clearAll() }
        coVerify(exactly = 1) { pendingDao.clearAll() }
    }
}
