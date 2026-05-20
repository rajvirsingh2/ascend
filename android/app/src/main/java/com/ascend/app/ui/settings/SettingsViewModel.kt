package com.ascend.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.AuthApiService
import com.ascend.app.data.remote.api.SaveApiKeyRequest
import com.ascend.app.data.remote.api.SettingsApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: SettingsApiService,
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
            is SettingsIntent.Load            -> load()
            is SettingsIntent.ProviderChanged ->
                _state.update { it.copy(selectedProvider = intent.provider) }
            is SettingsIntent.ApiKeyChanged   ->
                _state.update { it.copy(apiKeyInput = intent.value, error = null) }
            is SettingsIntent.ModelChanged    ->
                _state.update { it.copy(modelInput = intent.value) }
            is SettingsIntent.SaveKey         -> saveKey()
            is SettingsIntent.DeleteKey       -> deleteKey()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val resp = api.getKeyStatus()
                _state.update {
                    it.copy(isLoading = false,
                        hasApiKey = resp.data?.hasKey == true)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun saveKey() {
        val s = _state.value
        if (s.apiKeyInput.length < 20) {
            _state.update { it.copy(error = "API key appears too short") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                api.saveApiKey(
                    SaveApiKeyRequest(
                        provider = s.selectedProvider,
                        apiKey   = s.apiKeyInput,
                        model    = s.modelInput.ifBlank {
                            providerOptions.first { it.id == s.selectedProvider }.defaultModel
                        }
                    )
                )
                _state.update {
                    it.copy(isSaving = false, hasApiKey = true, apiKeyInput = "")
                }
                _effects.send(SettingsEffect.ShowSnackbar("API key saved securely"))
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Failed to save key") }
            }
        }
    }

    private fun deleteKey() {
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            try {
                api.deleteApiKey()
                _state.update { it.copy(isDeleting = false, hasApiKey = false) }
                _effects.send(SettingsEffect.ShowSnackbar("API key removed"))
            } catch (e: Exception) {
                _state.update { it.copy(isDeleting = false) }
                _effects.send(SettingsEffect.ShowSnackbar("Failed to remove key"))
            }
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