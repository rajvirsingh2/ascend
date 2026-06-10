package com.ascend.app.data.repository

import android.content.Context
import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.remote.api.HabitApiService
import com.ascend.app.data.remote.dto.ApiEnvelope
import com.ascend.app.data.remote.dto.CompletionResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HabitRepositoryTest {

    private lateinit var api: HabitApiService
    private lateinit var dao: HabitDao
    private lateinit var pendingDao: PendingOperationDao
    private lateinit var context: Context
    private lateinit var repository: HabitRepository

    @Before
    fun setup() {
        api = mockk()
        dao = mockk(relaxed = true)
        pendingDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        repository = HabitRepository(api, dao, pendingDao, context)

        mockkObject(SyncWorker.Companion)
        justRun { SyncWorker.requestSync(any()) }
    }

    @After
    fun tearDown() {
        unmockkObject(SyncWorker.Companion)
    }

    @Test
    fun `completeHabit marks completed locally on success`() = runTest {
        coEvery { api.completeHabit("h-1") } returns
            ApiEnvelope(data = CompletionResponse(xpAwarded = 20), error = null)

        val result = repository.completeHabit("h-1")

        assertTrue(result is Result.Success)
        coVerify { dao.markCompleted("h-1") }
        coVerify(exactly = 0) { pendingDao.insert(any()) }
    }

    @Test
    fun `completeHabit offline applies optimistic update and queues replay`() = runTest {
        coEvery { api.completeHabit("h-1") } throws IOException("no network")

        val result = repository.completeHabit("h-1")

        assertTrue(result is Result.Success)
        coVerify { dao.markCompleted("h-1") }
        coVerify(exactly = 1) {
            pendingDao.insert(match {
                it.type == PendingOperationEntity.TYPE_COMPLETE_HABIT && it.targetId == "h-1"
            })
        }
        io.mockk.verify(exactly = 1) { SyncWorker.requestSync(any()) }
    }

    @Test
    fun `completeHabit server rejection is an error and is NOT queued`() = runTest {
        coEvery { api.completeHabit("h-1") } returns ApiEnvelope(data = null, error = "not found")

        val result = repository.completeHabit("h-1")

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { dao.markCompleted(any()) }
        coVerify(exactly = 0) { pendingDao.insert(any()) }
    }
}
