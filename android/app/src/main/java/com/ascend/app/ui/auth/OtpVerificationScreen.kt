package com.ascend.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients

@Composable
fun OtpVerificationScreen(
    email: String,
    onVerified: () -> Unit,
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
                viewModel.verifyOtp(email, otpCode,
                    onSuccess = {
                        onVerified()
                    },
                    onError = { _ ->
                    }
                )
            } else {
                println("OTP IS LESS THAN 6 DIGITS")
            }
        },
        onResendClick = { viewModel.resendOtp(email) },
        onErrorCleared = { }
    )
}

@Composable
fun OtpVerificationScreenContent(
    email: String,
    isVerifying: Boolean,
    error: String?,
    onVerifyClick: (String) -> Unit,
    onResendClick: () -> Unit,
    onErrorCleared: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = DarkColors.Void,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "VERIFY EMAIL",
                fontSize = 11.sp,
                color = DarkColors.Arcane,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.14.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Enter the 6-digit code sent to",
                fontSize = 16.sp,
                color = DarkColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = email,
                fontSize = 14.sp,
                color = DarkColors.Cyan,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = otpCode,
                onValueChange = {
                    if (it.length <= 6) {
                        otpCode = it
                        if (error != null) onErrorCleared()
                    }
                },
                label = { Text("6-digit code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error != null,
                supportingText = error?.let { { Text(it, color = DarkColors.Ember) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.3.sp,
                    color = DarkColors.TextPrimary
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DarkColors.Arcane,
                    unfocusedBorderColor = DarkColors.Dusk,
                    focusedTextColor     = DarkColors.TextPrimary,
                    unfocusedTextColor   = DarkColors.TextPrimary,
                    cursorColor          = DarkColors.Arcane,
                    focusedLabelColor    = DarkColors.Arcane,
                    unfocusedLabelColor  = DarkColors.TextMuted,
                )
            )

            Spacer(Modifier.height(24.dp))

            AscendButton(
                text = if (isVerifying) "VERIFYING..." else "VERIFY",
                onClick = { onVerifyClick(otpCode) },
                enabled = !isVerifying && otpCode.length == 6,
                gradient = Gradients.ArcaneFlow,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onResendClick) {
                Text(
                    "Resend code",
                    color = DarkColors.TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
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
            onErrorCleared = {}
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
            onErrorCleared = {}
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
            onErrorCleared = {}
        )
    }
}