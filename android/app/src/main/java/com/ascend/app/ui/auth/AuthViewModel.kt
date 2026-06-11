package com.ascend.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.local.TokenDataStore
import com.ascend.app.data.remote.api.AuthApiService
import com.ascend.app.data.remote.api.UserApiService
import com.ascend.app.data.remote.dto.FCMTokenRequest
import com.ascend.app.data.remote.dto.ForgotPasswordRequest
import com.ascend.app.data.remote.dto.LoginRequest
import com.ascend.app.data.remote.dto.RegisterRequest
import com.ascend.app.data.remote.dto.ResendOtpRequest
import com.ascend.app.data.remote.dto.ResetPasswordRequest
import com.ascend.app.data.remote.dto.VerifyEmailRequest
import com.google.firebase.messaging.FirebaseMessaging
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
    private val tokenDataStore: TokenDataStore,
    private val userApi: UserApiService
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _loginState.update { it.copy(emailError = "Invalid email address") }
            return
        }
        if (state.password.length < 8) {
            _loginState.update { it.copy(passwordError = "Minimum 8 characters") }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true) }
            try {
                val response = authApi.login(LoginRequest(state.email, state.password))
                if (response.data != null) {
                    tokenDataStore.saveToken(response.data.accessToken)
                    // Register FCM token for push notifications
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                        viewModelScope.launch {
                            try { userApi.registerFCMToken(FCMTokenRequest(fcmToken)) }
                            catch (e: Exception) { /* non-fatal */ }
                        }
                    }
                    _effects.send(AuthEffect.NavigateToSplash)
                } else {
                    val error = response.error ?: "Login failed"
                    if (error.contains("not verified", ignoreCase = true)) {
                        _effects.send(AuthEffect.NavigateToOtp(state.email))
                    } else {
                        _effects.send(AuthEffect.ShowError(error))
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                if (e.code() == 403 && errorBody?.contains("not verified", ignoreCase = true) == true) {
                    _effects.send(AuthEffect.NavigateToOtp(state.email))
                } else {
                    _effects.send(AuthEffect.ShowError("Login failed: ${e.message()}"))
                }
            } catch (e: Exception) {
                _effects.send(AuthEffect.ShowError(e.message ?: "Login failed"))
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun submitRegister() {
        val state = _registerState.value
        if (state.username.isBlank() || state.username.length < 3 || state.username.length > 20 ||
            !state.username.all { it.isLetterOrDigit() || it == '_' }) {
            _registerState.update { it.copy(error = "Username must be 3–20 alphanumeric characters") }
            return
        }
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _registerState.update { it.copy(error = "Invalid email address") }
            return
        }
        if (state.password.length < 8) {
            _registerState.update { it.copy(error = "Password must be at least 8 characters") }
            return
        }
        if (!state.password.any { it.isDigit() }) {
            _registerState.update { it.copy(error = "Password must contain at least 1 number") }
            return
        }
        if (!state.password.any { !it.isLetterOrDigit() }) {
            _registerState.update { it.copy(error = "Password must contain at least 1 symbol") }
            return
        }

        viewModelScope.launch {
            _registerState.update { it.copy(isLoading = true) }
            try {
                val response = authApi.register(
                    RegisterRequest(state.email, state.password, state.username)
                )
                if (response.data != null) {
                    _effects.send(AuthEffect.NavigateToOtp(state.email))
                } else {
                    _registerState.update { it.copy(error = response.error ?: "Registration failed") }
                }
            } catch (e: Exception) {
                _registerState.update { it.copy(error = e.message ?: "Registration failed") }
            } finally {
                _registerState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun verifyOtp(email:String, code:String, onSuccess: () -> Unit, onError: (String)->Unit){
        viewModelScope.launch {
            try {
                authApi.verifyEmail(VerifyEmailRequest(email = email, otp = code))
                onSuccess()
            } catch (e: Exception) {
                onError("Invalid or expired code")
            }
        }
    }

    fun resendOtp(email: String) {
        viewModelScope.launch {
            try {
                authApi.resendOtp(ResendOtpRequest(email = email))
            } catch (_: Exception) {}
        }
    }

    fun sendResetCode(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authApi.forgotPassword(ForgotPasswordRequest(email))
                onSuccess()
            } catch (e: Exception) { onError("Request failed — check your email") }
        }
    }

    fun resetPassword(email: String, otp: String, newPass: String,
                      onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                authApi.resetPassword(ResetPasswordRequest(email, otp, newPass))
                onSuccess()
            } catch (e: Exception) { onError("Reset failed — check your code") }
        }
    }

}