package com.ascend.app.ui.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowSnackbar(val message: String) : SettingsEffect
}