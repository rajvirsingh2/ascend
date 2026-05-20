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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients

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
    initialStep: Int = 0 // Added to allow previewing both steps easily
) {
    var step         by remember { mutableIntStateOf(initialStep) }  // 0=email, 1=otp+new pass
    var email        by remember { mutableStateOf("") }
    var otp          by remember { mutableStateOf("") }
    var newPassword  by remember { mutableStateOf("") }
    var confirm      by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var error        by remember { mutableStateOf<String?>(null) }
    val snackbar     = remember { SnackbarHostState() }

    Scaffold(
        containerColor = DarkColors.Void,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("FORGOT PASSWORD", fontSize = 11.sp, color = DarkColors.Arcane,
                fontWeight = FontWeight.Medium, letterSpacing = 0.14.sp)
            Spacer(Modifier.height(8.dp))

            if (step == 0) {
                Text("Enter your email and we'll send a reset code.",
                    fontSize = 14.sp, color = DarkColors.TextMuted,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it; error = null },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = DarkColors.Ember) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = ascendTextFieldColors()
                )
                Spacer(Modifier.height(20.dp))
                AscendButton(
                    text = if (isLoading) "SENDING..." else "SEND RESET CODE",
                    onClick = {
                        isLoading = true
                        onSendResetCode(email,
                            { isLoading = false; step = 1; error = null },
                            { err -> isLoading = false; error = err }
                        )
                    },
                    enabled  = email.isNotBlank() && !isLoading,
                    gradient = Gradients.ArcaneFlow,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("Enter the 6-digit code sent to $email and your new password.",
                    fontSize = 13.sp, color = DarkColors.TextMuted,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = otp, onValueChange = { if (it.length <= 6) otp = it },
                    label = { Text("6-digit code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = ascendTextFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = newPassword, onValueChange = { newPassword = it; error = null },
                    label = { Text("New password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = ascendTextFieldColors()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it; error = null },
                    label = { Text("Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = DarkColors.Ember) } },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = ascendTextFieldColors()
                )
                Spacer(Modifier.height(20.dp))
                AscendButton(
                    text = if (isLoading) "RESETTING..." else "RESET PASSWORD",
                    onClick = {
                        if (newPassword != confirm) { error = "Passwords do not match"; return@AscendButton }
                        isLoading = true
                        onResetPassword(email, otp, newPassword,
                            { onResetSuccess() },
                            { err -> isLoading = false; error = err }
                        )
                    },
                    enabled  = otp.length == 6 && newPassword.length >= 8 && !isLoading,
                    gradient = Gradients.EnergyStream,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text("Back to login", color = DarkColors.TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ascendTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = DarkColors.Arcane,
    unfocusedBorderColor = DarkColors.Dusk,
    focusedTextColor     = DarkColors.TextPrimary,
    unfocusedTextColor   = DarkColors.TextPrimary,
    cursorColor          = DarkColors.Arcane,
    focusedLabelColor    = DarkColors.Arcane,
    unfocusedLabelColor  = DarkColors.TextMuted,
)

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