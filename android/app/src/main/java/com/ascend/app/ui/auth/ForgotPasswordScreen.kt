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
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextPrimary

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    ForgotPasswordScreenContent(
        onBack = onBack,
        onResetSuccess = onResetSuccess,
        onSendResetCode = { email, onSuccess, onError ->
            viewModel.sendResetCode(email, onSuccess, onError)
        },
        onResetPassword = { email, otp, newPassword, onSuccess, onError ->
            viewModel.resetPassword(email, otp, newPassword, onSuccess, onError)
        }
    )
}

@Composable
fun ForgotPasswordScreenContent(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    onSendResetCode: (email: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onResetPassword: (email: String, otp: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    initialStep: Int = 0
) {
    var step by remember { mutableIntStateOf(initialStep) }
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // Halo throb
    val infiniteTransition = rememberInfiniteTransition(label = "forgot")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f, targetValue = 0.60f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "halo"
    )

    val titleBrush = Brush.verticalGradient(
        0f to Color.White,
        0.6f to Color(0xFFC9B8FF),
        1f to CyanAccent
    )

    Scaffold(
        containerColor = DarkColors.Void,
        snackbarHost = { SnackbarHost(snackbar) }
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
            // ---------- HEADER (key icon + title) ----------
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .blur(20.dp)
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
                    Icon(
                        imageVector = Icons.Filled.VpnKey,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier
                            .size(32.dp)
                            .shadow(10.dp, CircleShape, ambientColor = CyanAccent, spotColor = CyanAccent)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (step == 0) "RECOVER ACCESS" else "RESET PROTOCOL",
                    fontFamily = orbitron,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    style = TextStyle(
                        brush = titleBrush,
                        shadow = Shadow(Color(0xFF7C3AED).copy(alpha = 0.5f), blurRadius = 20f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (step == 0)
                        "▸ ENTER EMAIL TO DISPATCH RESET KEY"
                    else
                        "▸ 5-DIGIT KEY SENT TO ${email.uppercase()}",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // ---------- PANEL ----------
            SystemPanel(
                glowColor = PurplePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (step == 0) "▸ STEP 1 OF 2" else "▸ STEP 2 OF 2",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = CyanAccent
                )
                Spacer(Modifier.height(18.dp))

                if (step == 0) {
                    LabeledField(
                        label = "EMAIL",
                        value = email,
                        onValueChange = { email = it; error = null },
                        placeholder = "hunter@ascend.app",
                        keyboardType = KeyboardType.Email,
                        error = error
                    )

                    Spacer(Modifier.height(20.dp))

                    GradientPrimaryButton(
                        text = if (isLoading) "DISPATCHING..." else "SEND RESET KEY",
                        icon = Icons.Filled.Send,
                        enabled = email.isNotBlank() && !isLoading,
                        isLoading = isLoading,
                        onClick = {
                            isLoading = true
                            onSendResetCode(email,
                                { isLoading = false; step = 1; error = null },
                                { err -> isLoading = false; error = err }
                            )
                        }
                    )
                } else {
                    LabeledField(
                        label = "RESET CODE",
                        value = otp,
                        onValueChange = { if (it.length <= 5) otp = it.filter { c -> c.isDigit() } },
                        placeholder = "5-DIGIT KEY",
                        keyboardType = KeyboardType.NumberPassword
                    )
                    Spacer(Modifier.height(14.dp))

                    LabeledField(
                        label = "NEW ACCESS KEY",
                        value = newPassword,
                        onValueChange = { newPassword = it; error = null },
                        placeholder = "create new key",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )
                    Spacer(Modifier.height(14.dp))

                    LabeledField(
                        label = "CONFIRM KEY",
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        placeholder = "repeat key",
                        keyboardType = KeyboardType.Password,
                        isPassword = true,
                        error = error
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "▸ MIN 8 CHARS · 1 NUMBER · 1 SYMBOL",
                        fontFamily = jetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = DarkColors.TextMuted.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(18.dp))

                    GradientPrimaryButton(
                        text = if (isLoading) "RESETTING..." else "RESET ACCESS KEY",
                        icon = Icons.Filled.LockReset,
                        enabled = otp.length == 6 && newPassword.length >= 8 && !isLoading,
                        isLoading = isLoading,
                        onClick = {
                            if (newPassword != confirm) {
                                error = "Keys do not match"
                                return@GradientPrimaryButton
                            }
                            isLoading = true
                            onResetPassword(email, otp, newPassword,
                                { onResetSuccess() },
                                { err -> isLoading = false; error = err }
                            )
                        }
                    )
                }
            }

            // ---------- BACK ----------
            Text(
                text = "← BACK TO LOGIN",
                fontFamily = jetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = DarkColors.TextMuted,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(8.dp)
            )
        }
    }
}

/**
 * Reusable gradient button (purple → cyan) with leading icon + glow shadow.
 * Drop in Components.kt and share across screens.
 */
@Composable
fun GradientPrimaryButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                    RoundedCornerShape(10.dp)
                )
                .alpha(if (enabled) 1f else 0.45f),
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
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
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

@Preview(showBackground = true, name = "Step 0: Email Entry")
@Composable
fun ForgotPasswordScreenPreview_Step0() {
    MaterialTheme {
        ForgotPasswordScreenContent(
            onBack = {},
            onResetSuccess = {},
            onSendResetCode = { _, _, _ -> },
            onResetPassword = { _, _, _, _, _ -> },
            initialStep = 0
        )
    }
}

@Preview(showBackground = true, name = "Step 1: OTP & New Password")
@Composable
fun ForgotPasswordScreenPreview_Step1() {
    MaterialTheme {
        ForgotPasswordScreenContent(
            onBack = {},
            onResetSuccess = {},
            onSendResetCode = { _, _, _ -> },
            onResetPassword = { _, _, _, _, _ -> },
            initialStep = 1
        )
    }
}