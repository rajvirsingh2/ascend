package com.ascend.app.data.remote.api

import com.ascend.app.data.remote.dto.NotificationListResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotificationApiService {
    @GET("notifications")
    suspend fun getNotifications(): NotificationListResponse

    @PUT("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    @PUT("notifications/read-all")
    suspend fun markAllRead()

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String)

    @DELETE("notifications/clear-all")
    suspend fun clearAll()
}
