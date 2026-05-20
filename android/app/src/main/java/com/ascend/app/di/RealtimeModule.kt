package com.ascend.app.di

import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.realtime.WebSocketManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RealtimeModule {

    @Provides
    @Singleton
    fun provideWebSocketManager(
        client: OkHttpClient,
        tokenDataStore: TokenDataStore
    ): WebSocketManager = WebSocketManager(client, tokenDataStore)
}