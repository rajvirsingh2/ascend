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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextPrimary


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
        onSocialSignIn = { credential -> viewModel.signInWithCredential(credential) },
        onSocialError = { error -> /* Let snackbar handle it if we want */ },
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
    onSocialSignIn: (com.google.firebase.auth.AuthCredential) -> Unit,
    onSocialError: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Halo throb behind logo
    val infiniteTransition = rememberInfiniteTransition(label = "register")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "halo"
    )

    val titleBrush = Brush.verticalGradient(
        0f to Color.White,
        0.55f to Color(0xFFC9B8FF),
        1f to CyanAccent
    )

    val googleSignInLauncher = rememberGoogleSignInLauncher(
        onSuccess = { token ->
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)
            onSocialSignIn(credential)
        },
        onError = onSocialError
    )

    // Lighter purple glow for panel (web: glow="var(--purple-2)")
    val purpleLight = Color(0xFFA78BFA)

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
                        letterSpacing = 6.5.sp,
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

            // ---------- PANEL (lighter purple glow) ----------
            SystemPanel(
                glowColor = purpleLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "◈ AWAKENING PROTOCOL",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = CyanAccent
                )
                Spacer(Modifier.height(18.dp))

                LabeledField(
                    label = "HUNTER NAME",
                    value = username,
                    onValueChange = onUsernameChanged,
                    placeholder = "KAIRO"
                )
                Spacer(Modifier.height(14.dp))

                LabeledField(
                    label = "EMAIL",
                    value = email,
                    onValueChange = onEmailChanged,
                    placeholder = "hunter@ascend.app",
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(14.dp))

                LabeledField(
                    label = "PASSWORD",
                    value = password,
                    onValueChange = onPasswordChanged,
                    placeholder = "create access key",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                Spacer(Modifier.height(10.dp))

                // Password rule hint
                Text(
                    text = "▸ MIN 8 CHARS · 1 NUMBER · 1 SYMBOL",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted.copy(alpha = 0.6f)
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(18.dp))

                // AWAKEN button — gradient + arrow-up icon, no pulse
                Button(
                    onClick = onSubmitRegister,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 14.dp,
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
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "AWAKEN",
                                    fontFamily = orbitron,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 3.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // ---------- LOGIN LINK ----------
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ALREADY AWAKENED? ",
                    fontFamily = jetBrainsMono,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted
                )
                Text(
                    text = "LOGIN",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    style = TextStyle(
                        shadow = Shadow(CyanAccent.copy(alpha = 0.7f), blurRadius = 10f)
                    ),
                    modifier = Modifier.clickable { onNavigateToLogin() }
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
            onNavigateToLogin = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToLogin = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToLogin = {},
            onSocialSignIn = {},
            onSocialError = {}
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
            onNavigateToLogin = {},
            onSocialSignIn = {},
            onSocialError = {}
        )
    }
}