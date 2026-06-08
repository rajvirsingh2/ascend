package com.ascend.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron

import com.ascend.app.ui.theme.*

// 1. Stateful Wrapper handling ViewModel and Effects
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SettingsScreenContent(
        isDeletingKey = state.isDeleting,
        error = state.error,
        snackbarHostState = snackbarHostState,
        onDeleteAccount = { password, onComplete -> viewModel.deleteAccount(password, onComplete) }
    )
}

// 2. Stateless UI Composable safe for Previews
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    isDeletingKey: Boolean,
    error: String?,
    snackbarHostState: SnackbarHostState,
    onDeleteAccount: (String, () -> Unit) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF07070B),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SYSTEM SETTINGS",
                        fontFamily = orbitron,
                        color = ReactPurple,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontSize = 16.sp,
                        style = TextStyle(shadow = Shadow(ReactPurple.copy(alpha = 0.5f), blurRadius = 10f))
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF07070B).copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .scanlineOverlay()
                .padding(padding)
        ) {
            // Ambient Danger Radial Aura
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ReactRed.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Danger zone card
                DangerZoneCard(
                    isDeletingKey = isDeletingKey,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }
    }
}

@Composable
private fun DangerZoneCard(
    isDeletingKey: Boolean,
    onDeleteAccount: (String, () -> Unit) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = ReactRed.copy(alpha = 0.25f),
                spotColor = ReactRed.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(ReactPanel)
            .border(1.dp, ReactRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = ReactRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "DANGER ZONE",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = ReactRed,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Deleting your account schedules permanent removal in 30 days. You can cancel within that window.",
                fontFamily = jetBrainsMono,
                fontSize = 12.sp,
                color = ReactInkDim,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            // Premium Refactored High-End Destructive CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ReactRed.copy(alpha = 0.1f))
                    .border(1.dp, ReactRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DELETE ACCOUNT",
                    fontFamily = orbitron,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = ReactRed,
                    style = TextStyle(shadow = Shadow(ReactRed.copy(alpha = 0.5f), blurRadius = 8f))
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isDeletingAccount) showDeleteDialog = false },
                    containerColor = ReactPanel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ReactPanelLine, RoundedCornerShape(12.dp)),
                    title = {
                        Text(
                            "CONFIRM SYSTEM REMOVAL",
                            fontFamily = orbitron,
                            color = ReactRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Enter your password to schedule account deletion. Your data will be permanently removed after 30 days.",
                                fontFamily = jetBrainsMono,
                                fontSize = 12.sp,
                                color = ReactInkDim,
                                lineHeight = 18.sp
                            )
                            OutlinedTextField(
                                value = deletePassword,
                                onValueChange = { deletePassword = it },
                                placeholder = {
                                    Text("Password", fontFamily = jetBrainsMono, fontSize = 13.sp, color = ReactInkFaint)
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = jetBrainsMono, fontSize = 14.sp, color = ReactInk),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ReactRed,
                                    unfocusedBorderColor = ReactPanelLine,
                                    focusedContainerColor = Color(0xFF0C0C16),
                                    unfocusedContainerColor = Color(0xFF0C0C16),
                                    cursorColor = ReactRed
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeletingAccount = true
                                onDeleteAccount(deletePassword) {
                                    showDeleteDialog = false
                                    isDeletingAccount = false
                                    deletePassword = ""
                                }
                            },
                            enabled = deletePassword.isNotBlank() && !isDeletingAccount
                        ) {
                            if (isDeletingAccount || isDeletingKey) {
                                CircularProgressIndicator(
                                    color = ReactRed,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    "DELETE",
                                    fontFamily = jetBrainsMono,
                                    color = if (deletePassword.isNotBlank()) ReactRed else ReactInkFaint,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false },
                            enabled = !isDeletingAccount
                        ) {
                            Text(
                                "CANCEL",
                                fontFamily = jetBrainsMono,
                                color = ReactInkDim,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                )
            }
        }
    }
}

/* ============================================================
 * HELPERS
 * ============================================================ */
fun Modifier.scanlineOverlay(): Modifier = drawWithCache {
    val lineSpacing = 4f
    onDrawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(
                Color.White.copy(alpha = 0.015f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += lineSpacing
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "1. Default Settings")
@Composable
fun SettingsScreenPreview_Default() {
    androidx.compose.material3.MaterialTheme {
        SettingsScreenContent(
            isDeletingKey = false,
            error = null,
            snackbarHostState = remember { SnackbarHostState() },
            onDeleteAccount = { _, _ -> }
        )
    }
}