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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
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

import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.components.AscendOutlinedButton
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients

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
        containerColor = DarkColors.Void,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = DarkColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkColors.Abyss
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {



            Spacer(Modifier.height(80.dp))

            // Danger zone card
            SectionCard(title = "DANGER ZONE") {
                var showDeleteDialog by remember { mutableStateOf(false) }
                var deletePassword by remember { mutableStateOf("") }
                var isDeletingAccount by remember { mutableStateOf(false) }

                Text(
                    text = "Deleting your account schedules permanent removal in 30 days. You can cancel within that window.",
                    fontSize = 12.sp, color = DarkColors.TextMuted, lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                AscendButton(
                    text     = "DELETE ACCOUNT",
                    onClick  = { showDeleteDialog = true },
                    gradient = listOf(Color(0xFF991111), Color(0xFFFF2D78)),
                    modifier = Modifier.fillMaxWidth()
                )

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        containerColor   = DarkColors.Abyss,
                        title = {
                            Text("Confirm account deletion",
                                color = DarkColors.TextPrimary, fontWeight = FontWeight.Medium)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Enter your password to schedule account deletion. Your data will be permanently removed after 30 days.",
                                    fontSize = 13.sp, color = DarkColors.TextMuted
                                )
                                OutlinedTextField(
                                    value = deletePassword,
                                    onValueChange = { deletePassword = it },
                                    label = { Text("Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor   = DarkColors.Ember,
                                        unfocusedBorderColor = DarkColors.Dusk,
                                        focusedTextColor     = DarkColors.TextPrimary,
                                        unfocusedTextColor   = DarkColors.TextPrimary,
                                    )
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
                                    }
                                },
                                enabled = deletePassword.isNotBlank() && !isDeletingAccount
                            ) {
                                Text("DELETE", color = DarkColors.Ember, fontWeight = FontWeight.Medium)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("CANCEL", color = DarkColors.TextMuted)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkColors.Abyss)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DarkColors.Arcane,
            letterSpacing = 0.1.sp
        )
        Spacer(Modifier.height(14.dp))
        content()
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, name = "1. Default Settings")
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