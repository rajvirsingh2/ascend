package com.ascend.app.ui.auth

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ascend.app.R
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.navigation.Routes
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextPrimary


val orbitron= FontFamily(
    Font(R.font.orbitron_black, FontWeight.Normal)
)

val jetBrainsMono=FontFamily(
    Font(R.font.jetbrainsmono_medium, FontWeight.Normal)
)
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
                is AuthEffect.NavigateToSplash -> onNavigateToDashboard()
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
        onSocialSignIn = { credential -> viewModel.signInWithCredential(credential) },
        onSocialError = { error -> /* Let snackbar handle it if we want, or do nothing */ },
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
    onSocialSignIn: (com.google.firebase.auth.AuthCredential) -> Unit,
    onSocialError: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    // Pulse glow for primary button (web: Btn kind="grad" pulse)
    val infiniteTransition = rememberInfiniteTransition(label = "login")
    val btnGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "btnGlow"
    )
    // Halo throb behind logo
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "halo"
    )

    val googleSignInLauncher = rememberGoogleSignInLauncher(
        onSuccess = { token ->
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)
            onSocialSignIn(credential)
        },
        onError = onSocialError
    )

    val titleBrush = Brush.verticalGradient(
        0f to Color.White,
        0.55f to Color(0xFFC9B8FF),
        1f to CyanAccent
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkColors.Void
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkColors.Void)
                .padding(padding)
                .padding(horizontal = 22.dp, vertical = 34.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------- LOGO ----------
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .blur(24.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7C3AED).copy(alpha = haloAlpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ASCEND",
                        fontFamily = orbitron,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.5.sp, // 54 * 0.12
                        style = TextStyle(
                            brush = titleBrush,
                            shadow = Shadow(
                                color = Color(0xFF7C3AED).copy(alpha = 0.6f),
                                blurRadius = 30f
                            )
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "LEVEL UP IN REAL LIFE",
                        fontFamily = jetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ---------- PANEL ----------
            SystemPanel(
                glowColor = PurplePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                // h-sys header (web: "HUNTER LOGIN")
                Text(
                    text = "◈ HUNTER LOGIN",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = CyanAccent
                )
                Spacer(Modifier.height(18.dp))

                // Email field
                LabeledField(
                    label = "EMAIL",
                    value = email,
                    onValueChange = onEmailChanged,
                    placeholder = "hunter@ascend.app",
                    keyboardType = KeyboardType.Email,
                    error = emailError
                )
                Spacer(Modifier.height(14.dp))

                // Password field
                LabeledField(
                    label = "PASSWORD",
                    value = password,
                    onValueChange = onPasswordChanged,
                    placeholder = "••••••••",
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                    error = passwordError
                )

                Spacer(Modifier.height(6.dp))

                // FORGOT ACCESS? — right-aligned link INSIDE panel
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "FORGOT ACCESS?",
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        style = TextStyle(
                            shadow = Shadow(CyanAccent.copy(alpha = 0.7f), blurRadius = 10f)
                        ),
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ENTER THE SYSTEM — gradient button with bolt + pulse glow
                Button(
                    onClick = onSubmitLogin,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = (btnGlow * 22).dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = PurplePrimary,
                            spotColor = CyanAccent
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Bolt,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.run { width(8.dp) })
                                Text(
                                    text = "ENTER THE SYSTEM",
                                    fontFamily = orbitron,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NEW HUNTER? ",
                    fontFamily = jetBrainsMono,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted
                )
                Text(
                    text = "REGISTER",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    style = TextStyle(
                        shadow = Shadow(CyanAccent.copy(alpha = 0.7f), blurRadius = 10f)
                    ),
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---------- SOCIAL LOGINS ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = googleSignInLauncher,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CONTINUE WITH GOOGLE", color = Color.Black, fontFamily = jetBrainsMono, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Field with caps mono label ABOVE the input (matches web <Field>).
 * Reusable for Register + OTP screens.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null
) {
    Column {
        Text(
            text = label,
            fontFamily = jetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            color = DarkColors.TextMuted
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = DarkColors.TextMuted.copy(alpha = 0.5f),
                    fontFamily = jetBrainsMono,
                    fontSize = 14.sp
                )
            },
            isError = error != null,
            supportingText = error?.let { { Text(it, fontSize = 10.sp) } },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            textStyle = TextStyle(
                fontFamily = jetBrainsMono,
                fontSize = 15.sp,
                color = TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = BorderGlow,
                focusedContainerColor = Color(0xFF0C0C16),
                unfocusedContainerColor = Color(0xFF0C0C16),
                cursorColor = CyanAccent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
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
            onNavigateToForgotPassword = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToForgotPassword = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToForgotPassword = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToForgotPassword = {},
            onSocialSignIn = {},
            onSocialError = {}
        )
    }
}