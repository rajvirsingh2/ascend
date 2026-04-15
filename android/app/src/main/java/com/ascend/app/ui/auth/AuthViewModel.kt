package com.ascend.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.AuthApiService
import com.ascend.app.data.remote.dto.LoginRequest
import com.ascend.app.data.remote.dto.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApi: AuthApiService,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState = _registerState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onLoginIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged ->
                _loginState.update { it.copy(email = intent.value, emailError = null) }
            is AuthIntent.PasswordChanged ->
                _loginState.update { it.copy(password = intent.value, passwordError = null) }
            is AuthIntent.SubmitLogin -> submitLogin()
            else -> Unit
        }
    }

    fun onRegisterIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.EmailChanged ->
                _registerState.update { it.copy(email = intent.value) }
            is AuthIntent.PasswordChanged ->
                _registerState.update { it.copy(password = intent.value) }
            is AuthIntent.UsernameChanged ->
                _registerState.update { it.copy(username = intent.value) }
            is AuthIntent.SubmitRegister -> submitRegister()
            else -> Unit
        }
    }

    private fun submitLogin() {
        val state = _loginState.value
        if (state.email.isBlank()) {
            _loginState.update { it.copy(emailError = "Email is required") }
            return
        }
        if (state.password.length < 8) {
            _loginState.update { it.copy(passwordError = "Minimum 8 characters") }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true) }
            try {
                val response = authApi.login(
                    LoginRequest(email = state.email, password = state.password)
                )
                if (response.data != null) {
                    tokenDataStore.saveToken(response.data.accessToken)
                    _effects.send(AuthEffect.NavigateToDashboard)
                } else {
                    _effects.send(AuthEffect.ShowError(response.error ?: "Login failed"))
                }
            } catch (e: Exception) {
                _effects.send(AuthEffect.ShowError(e.message.toString()))
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun submitRegister() {
        val state = _registerState.value
        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true) }
            try {
                val response = authApi.register(
                    RegisterRequest(
                        email = state.email,
                        password = state.password,
                        username = state.username
                    )
                )
                if (response.data != null) {
                    _effects.send(AuthEffect.NavigateToDashboard)
                } else {
                    _registerState.update { it.copy(error = response.error ?: "Registration failed") }
                }
            } catch (e: Exception) {
                _registerState.update { it.copy(error=e.message.toString()) }
            } finally {
                _registerState.update { it.copy(isLoading = false) }
            }
        }
    }
}