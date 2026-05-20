package com.ascend.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.navigation.Routes
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurpleLight
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToSplash -> onNavigateToDashboard() // The callback is still named onNavigateToDashboard in the composable signature, but we'll route it to splash from MainActivity
                is AuthEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LoginScreenContent(
        email = state.email,
        password = state.password,
        emailError = state.emailError,
        passwordError = state.passwordError,
        isLoading = state.isLoading,
        snackbarHostState = snackbarHostState,
        onEmailChanged = { viewModel.onLoginIntent(AuthIntent.EmailChanged(it)) },
        onPasswordChanged = { viewModel.onLoginIntent(AuthIntent.PasswordChanged(it)) },
        onSubmitLogin = { viewModel.onLoginIntent(AuthIntent.SubmitLogin) },
        onNavigateToRegister = onNavigateToRegister,
        onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
    )
}

@Composable
fun LoginScreenContent(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSubmitLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkColors.Void) // Added background to match the theme context
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp)) // Added top padding

            Text(
                text = "Ascend",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp
            )
            Text(
                text = "LEVEL UP IN REAL LIFE",
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = CyanAccent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            SystemPanel(glowColor = PurplePrimary, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "HUNTER LOGIN", fontSize = 10.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp, color = PurpleLight
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChanged,
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderGlow,
                        focusedLabelColor = PurpleLight,
                        cursorColor = CyanAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = BorderGlow,
                        focusedLabelColor = PurpleLight,
                        cursorColor = CyanAccent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSubmitLogin,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(
                            16.dp, RoundedCornerShape(8.dp),
                            ambientColor = PurplePrimary, spotColor = CyanAccent
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        else
                            Text(
                                "ENTER THE SYSTEM", fontSize = 13.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 2.sp
                            )
                    }
                }
            }

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "NEW HUNTER? REGISTER", fontSize = 11.sp,
                    color = CyanAccent, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Forgot password?", color = DarkColors.TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Default State")
@Composable
fun LoginScreenPreview_Default() {
    MaterialTheme {
        LoginScreenContent(
            email = "",
            password = "",
            emailError = null,
            passwordError = null,
            isLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitLogin = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Active Entry State")
@Composable
fun LoginScreenPreview_Active() {
    MaterialTheme {
        LoginScreenContent(
            email = "hunter@ascend.com",
            password = "password123",
            emailError = null,
            passwordError = null,
            isLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitLogin = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Loading State")
@Composable
fun LoginScreenPreview_Loading() {
    MaterialTheme {
        LoginScreenContent(
            email = "hunter@ascend.com",
            password = "password123",
            emailError = null,
            passwordError = null,
            isLoading = true,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitLogin = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}

@Preview(showBackground = true, name = "4. Error State")
@Composable
fun LoginScreenPreview_Error() {
    MaterialTheme {
        LoginScreenContent(
            email = "invalid-email",
            password = "short",
            emailError = "Invalid email format",
            passwordError = "Password too short",
            isLoading = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChanged = {},
            onPasswordChanged = {},
            onSubmitLogin = {},
            onNavigateToRegister = {},
            onNavigateToForgotPassword = {}
        )
    }
}