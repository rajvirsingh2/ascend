package com.ascend.app.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isVerifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    OtpVerificationScreenContent(
        email = email,
        isVerifying = isVerifying,
        error = error,
        onVerifyClick = { otpCode ->
            if (otpCode.length == 6) {
                isVerifying = true
                viewModel.verifyOtp(email, otpCode,
                    onSuccess = { onVerified() },
                    onError = { msg -> 
                        isVerifying = false
                        error = msg 
                    }
                )
            }
        },
        onResendClick = { viewModel.resendOtp(email) },
        onErrorCleared = { error = null },
        onBack = onBack
    )
}

@Composable
fun OtpVerificationScreenContent(
    email: String,
    isVerifying: Boolean,
    error: String?,
    onVerifyClick: (String) -> Unit,
    onResendClick: () -> Unit,
    onErrorCleared: () -> Unit,
    onBack: () -> Unit
) {
    // 6 separate digit slots (web: array of 6 inputs)
    val digits = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val snackbarHostState = remember { SnackbarHostState() }

    // Resend countdown (web: 42s)
    var secs by remember { mutableIntStateOf(42) }
    LaunchedEffect(secs) {
        if (secs > 0) {
            delay(1000)
            secs -= 1
        }
    }

    // Auto-submit when all filled
    val filled = digits.count { it.isNotEmpty() }
    val animatedProgress by animateFloatAsState(
        targetValue = filled / 6f,
        animationSpec = tween(durationMillis = 350),
        label = "OtpProgress"
    )
    LaunchedEffect(filled) {
        if (filled == 6) {
            delay(350)
            onVerifyClick(digits.joinToString(""))
        }
    }

    // Mask email: k•••o@ascend.app
    val maskedEmail = remember(email) {
        val at = email.indexOf('@')
        if (at <= 1) email
        else {
            val name = email.substring(0, at)
            val domain = email.substring(at)
            "${name.first()}•••${name.last()}$domain"
        }
    }

    val titleBrush = Brush.verticalGradient(
        0f to Color.White,
        0.6f to Color(0xFFC9B8FF),
        1f to CyanAccent
    )

    Scaffold(
        containerColor = DarkColors.Void,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            // ---------- HEADER (lock icon + title + sub) ----------
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier
                        .size(30.dp)
                        .shadow(10.dp, CircleShape, ambientColor = CyanAccent, spotColor = CyanAccent)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "VERIFICATION CODE SENT",
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
                    text = "▸ 6-DIGIT KEY DISPATCHED TO",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = maskedEmail,
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = DarkColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // ---------- PANEL with 6 digit boxes + progress ----------
            SystemPanel(
                glowColor = PurplePrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    digits.forEachIndexed { i, d ->
                        OtpDigitBox(
                            value = d,
                            focusRequester = focusRequesters[i],
                            onValueChange = { newChar ->
                                val clean = newChar.filter { it.isDigit() }.takeLast(1)
                                digits[i] = clean
                                if (clean.isNotEmpty() && i < 5) {
                                    focusRequesters[i + 1].requestFocus()
                                }
                                if (error != null) onErrorCleared()
                            },
                            onBackspace = {
                                if (digits[i].isEmpty() && i > 0) {
                                    focusRequesters[i - 1].requestFocus()
                                    digits[i - 1] = ""
                                }
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // Progress bar (filled / 6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A1A2E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent))
                            )
                    )
                }

                if (isVerifying) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            color = CyanAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "VERIFYING...",
                            fontFamily = jetBrainsMono,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = CyanAccent
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = it,
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ---------- RESEND ----------
            if (secs > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RESEND KEY IN ",
                        fontFamily = jetBrainsMono,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = DarkColors.TextMuted
                    )
                    Text(
                        text = "0:${secs.toString().padStart(2, '0')}",
                        fontFamily = jetBrainsMono,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                }
            } else {
                Text(
                    text = "RESEND CODE",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    style = TextStyle(
                        shadow = Shadow(CyanAccent.copy(alpha = 0.7f), blurRadius = 10f)
                    ),
                    modifier = Modifier.clickable {
                        secs = 42
                        onResendClick()
                    }
                )
            }

            // ---------- BACK ----------
            Text(
                text = "← BACK",
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
 * Single OTP digit box. Web: 44×56, dark bg, cyan border when filled,
 * cyan glow shadow when filled.
 */
@Composable
fun OtpDigitBox(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val filled = value.isNotEmpty()
    val borderColor = if (filled) CyanAccent else BorderGlow
    val glow = if (filled) 14.dp else 0.dp

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = orbitron,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(CyanAccent),
        modifier = Modifier
            .size(width = 44.dp, height = 56.dp)
            .shadow(glow, RoundedCornerShape(10.dp), ambientColor = CyanAccent, spotColor = CyanAccent)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C0C16))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    value.isEmpty()
                ) {
                    onBackspace()
                    true
                } else false
            },
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                inner()
            }
        }
    )
}

@Preview(showBackground = true, name = "1. Default State")
@Composable
fun OtpVerificationScreenPreview_Default() {
    MaterialTheme {
        OtpVerificationScreenContent(
            email = "hunter@ascend.com",
            isVerifying = false,
            error = null,
            onVerifyClick = {},
            onResendClick = {},
            onErrorCleared = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Loading/Verifying State")
@Composable
fun OtpVerificationScreenPreview_Verifying() {
    MaterialTheme {
        OtpVerificationScreenContent(
            email = "hunter@ascend.com",
            isVerifying = true,
            error = null,
            onVerifyClick = {},
            onResendClick = {},
            onErrorCleared = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Error State")
@Composable
fun OtpVerificationScreenPreview_Error() {
    MaterialTheme {
        OtpVerificationScreenContent(
            email = "hunter@ascend.com",
            isVerifying = false,
            error = "Invalid or expired code. Please try again.",
            onVerifyClick = {},
            onResendClick = {},
            onErrorCleared = {},
            onBack = {}
        )
    }
}