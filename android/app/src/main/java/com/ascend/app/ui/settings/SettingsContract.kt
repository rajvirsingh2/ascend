package com.ascend.app.ui.settings

data class SettingsUiState(
    val isLoading: Boolean = true,
    val hasApiKey: Boolean = false,
    val selectedProvider: String = "openai",
    val apiKeyInput: String = "",
    val modelInput: String = "",
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data class ProviderChanged(val provider: String) : SettingsIntent
    data class ApiKeyChanged(val value: String) : SettingsIntent
    data class ModelChanged(val value: String) : SettingsIntent
    data object SaveKey : SettingsIntent
    data object DeleteKey : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowSnackbar(val message: String) : SettingsEffect
}

data class ProviderOption(
    val id: String,
    val label: String,
    val hint: String,
    val defaultModel: String
)

val providerOptions = listOf(
    ProviderOption("openai",    "OpenAI",      "sk-...",       "gpt-4o"),
    ProviderOption("claude",    "Claude",      "sk-ant-...",   "claude-sonnet-4-6"),
    ProviderOption("gemini",    "Gemini",      "AIza...",      "gemini-1.5-flash"),
    ProviderOption("anthropic", "Anthropic",   "sk-ant-...",   "claude-sonnet-4-6")
)