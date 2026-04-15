package com.ascend.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "ascend_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val accessTokenKey = stringPreferencesKey("access_token")

    val accessToken: Flow<String?>
        get() = context.dataStore.data.map { it[accessTokenKey] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[accessTokenKey] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(accessTokenKey) }
    }
}