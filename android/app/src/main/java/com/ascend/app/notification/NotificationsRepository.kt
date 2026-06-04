package com.ascend.app.notification

import com.ascend.app.data.remote.api.NotificationApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val api: NotificationApiService
) {
    fun getNotifications(): Flow<List<NotifItem>> = flow {
        val response = api.getNotifications()
        emit(response.data)
    }

    suspend fun markRead(id: String) {
        api.markRead(id)
    }

    suspend fun markAllRead() {
        api.markAllRead()
    }

    suspend fun delete(id: String) {
        api.deleteNotification(id)
    }

    suspend fun clearAll() {
        api.clearAll()
    }
}
