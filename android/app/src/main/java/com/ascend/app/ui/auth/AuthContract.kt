package com.ascend.app.ui.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null
)

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface AuthIntent {
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data class UsernameChanged(val value: String) : AuthIntent
    data object SubmitLogin : AuthIntent
    data object SubmitRegister : AuthIntent
}

sealed interface AuthEffect {
    data object NavigateToDashboard : AuthEffect
    data class ShowError(val message: String) : AuthEffect
}