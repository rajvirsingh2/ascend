package com.ascend.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.AuthApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { onIntent(SettingsIntent.Load) }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.Load -> { /* no-op for now */ }
            is SettingsIntent.Logout -> logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            tokenDataStore.clearToken()
            _effects.send(SettingsEffect.NavigateToLogin)
        }
    }

    fun deleteAccount(password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                authApi.requestDeletion(AuthApiService.DeleteAccountRequest(password = password))
                tokenDataStore.clearToken()
                _effects.send(SettingsEffect.ShowSnackbar(
                    "Account deletion scheduled. You have 30 days to cancel."))
                onDone()
            } catch (e: Exception) {
                _effects.send(SettingsEffect.ShowSnackbar("Incorrect password"))
                onDone()
            }
        }
    }
}