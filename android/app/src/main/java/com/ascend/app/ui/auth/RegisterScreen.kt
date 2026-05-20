package com.ascend.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun RegisterScreen(
    navController: NavController,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.registerState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToSplash -> {
                    // after register — go to OTP screen, not dashboard
                    navController.navigate("otp/${state.email}")
                }
                is AuthEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    RegisterScreenContent(
        username = state.username,
        email = state.email,
        password = state.password,
        isLoading = state.isLoading,
        error = state.error,
        snackbarHostState = snackbarHostState,
        onUsernameChanged = { viewModel.onRegisterIntent(AuthIntent.UsernameChanged(it)) },
        onEmailChanged = { viewModel.onRegisterIntent(AuthIntent.EmailChanged(it)) },
        onPasswordChanged = { viewModel.onRegisterIntent(AuthIntent.PasswordChanged(it)) },
        onSubmitRegister = { viewModel.onRegisterIntent(AuthIntent.SubmitRegister) },
        onNavigateToLogin = onNavigateToLogin
    )
}

@Composable
fun RegisterScreenContent(
    username: String,
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    snackbarHostState: SnackbarHostState,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitRegister: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create account", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChanged,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChanged,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSubmitRegister,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp)
                    )
                } else {
                    Text("Create account")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Log in")
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Default State")
@Composable
fun RegisterScreenPreview_Default() {
    MaterialTheme {
        RegisterScreenContent(
            username = "",
            email = "",
            password = "",
            isLoading = false,
            error = null,
            snackbarHostState = remember { SnackbarHostState() },
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitRegister = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Active Entry State")
@Composable
fun RegisterScreenPreview_Active() {
    MaterialTheme {
        RegisterScreenContent(
            username = "SungJinwoo",
            email = "jinwoo@shadow.com",
            password = "arise",
            isLoading = false,
            error = null,
            snackbarHostState = remember { SnackbarHostState() },
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitRegister = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Loading State")
@Composable
fun RegisterScreenPreview_Loading() {
    MaterialTheme {
        RegisterScreenContent(
            username = "SungJinwoo",
            email = "jinwoo@shadow.com",
            password = "arise",
            isLoading = true,
            error = null,
            snackbarHostState = remember { SnackbarHostState() },
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitRegister = {},
            onNavigateToLogin = {}
        )
    }
}

@Preview(showBackground = true, name = "4. Error State")
@Composable
fun RegisterScreenPreview_Error() {
    MaterialTheme {
        RegisterScreenContent(
            username = "Sung",
            email = "invalid-email",
            password = "123",
            isLoading = false,
            error = "Password must be at least 8 characters long",
            snackbarHostState = remember { SnackbarHostState() },
            onUsernameChanged = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitRegister = {},
            onNavigateToLogin = {}
        )
    }
}