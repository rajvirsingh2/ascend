package com.ascend.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ui/settings/SettingsViewModel.kt
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsApi: SettingsApiService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    fun saveApiKey(rawKey: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                // transmit once — backend encrypts and stores
                // the raw key never touches local storage
                settingsApi.saveApiKey(SaveApiKeyRequest(geminiApiKey = rawKey))
                _state.update { it.copy(isSaving = false, keySaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false,
                    error = "Failed to save key securely") }
            }
            // rawKey goes out of scope here — GC eligible
        }
    }
}