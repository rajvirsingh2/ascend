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
        hasApiKey = state.hasApiKey,
        isDeletingKey = state.isDeleting,
        selectedProvider = state.selectedProvider,
        apiKeyInput = state.apiKeyInput,
        modelInput = state.modelInput,
        error = state.error,
        isSaving = state.isSaving,
        snackbarHostState = snackbarHostState,
        onDeleteKey = { viewModel.onIntent(SettingsIntent.DeleteKey) },
        onProviderChanged = { viewModel.onIntent(SettingsIntent.ProviderChanged(it)) },
        onApiKeyChanged = { viewModel.onIntent(SettingsIntent.ApiKeyChanged(it)) },
        onModelChanged = { viewModel.onIntent(SettingsIntent.ModelChanged(it)) },
        onSaveKey = { viewModel.onIntent(SettingsIntent.SaveKey) },
        onDeleteAccount = { password, onComplete -> viewModel.deleteAccount(password, onComplete) }
    )
}

// 2. Stateless UI Composable safe for Previews
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    hasApiKey: Boolean,
    isDeletingKey: Boolean,
    selectedProvider: String,
    apiKeyInput: String,
    modelInput: String,
    error: String?,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onDeleteKey: () -> Unit,
    onProviderChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    onSaveKey: () -> Unit,
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

            // section header
            SectionCard(title = "AI QUEST ENGINE") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(
                        text = "Your API key is encrypted with AES-256-GCM and never stored in plain text. It is decrypted only during quest generation.",
                        fontSize = 12.sp,
                        color = DarkColors.TextMuted,
                        lineHeight = 18.sp
                    )

                    if (hasApiKey) {
                        // key already saved
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1A0D))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "API KEY ACTIVE",
                                    fontSize = 10.sp,
                                    color = Color(0xFF39FF14),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.1.sp
                                )
                                Text(
                                    text = "Quest generation is enabled",
                                    fontSize = 12.sp,
                                    color = DarkColors.TextMuted
                                )
                            }
                            if (isDeletingKey) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = DarkColors.Ember,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                AscendOutlinedButton(
                                    text = "REMOVE",
                                    onClick = onDeleteKey,
                                    borderColor = DarkColors.Ember
                                )
                            }
                        }
                    }

                    // provider selector
                    Text(
                        text = "AI PROVIDER",
                        fontSize = 10.sp,
                        color = DarkColors.TextMuted,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Uses your app's actual providerOptions list!
                        providerOptions.take(3).forEach { option ->
                            val selected = selectedProvider == option.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected)
                                            Brush.horizontalGradient(
                                                listOf(
                                                    DarkColors.Arcane.copy(0.3f),
                                                    DarkColors.Cyan.copy(0.3f)
                                                )
                                            )
                                        else Brush.horizontalGradient(
                                            listOf(DarkColors.Abyss, DarkColors.Deep)
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) DarkColors.Arcane
                                        else DarkColors.Dusk,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onProviderChanged(option.id) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Medium
                                    else FontWeight.Normal,
                                    color = if (selected) DarkColors.TextPrimary
                                    else DarkColors.TextMuted
                                )
                            }
                        }
                    }

                    // api key input
                    val currentOption = providerOptions
                        .firstOrNull { it.id == selectedProvider }
                        ?: providerOptions.first()

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = onApiKeyChanged,
                        label = { Text("API Key") },
                        placeholder = { Text(currentOption.hint,
                            color = DarkColors.TextHint) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        isError = error != null,
                        supportingText = error?.let {
                            { Text(it, color = DarkColors.Ember) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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

                    // optional model override
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = onModelChanged,
                        label = { Text("Model (optional)") },
                        placeholder = {
                            Text(
                                "Default: ${currentOption.defaultModel}",
                                color = DarkColors.TextHint
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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

                    AscendButton(
                        text = if (isSaving) "SAVING..." else
                            if (hasApiKey) "UPDATE KEY" else "SAVE KEY SECURELY",
                        onClick = onSaveKey,
                        enabled = !isSaving && apiKeyInput.isNotBlank(),
                        gradient = Gradients.ArcaneFlow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

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

@Preview(showBackground = true, name = "1. Default Settings (No Key)")
@Composable
fun SettingsScreenPreview_Default() {
    androidx.compose.material3.MaterialTheme {
        SettingsScreenContent(
            hasApiKey = false,
            isDeletingKey = false,
            selectedProvider = "gemini",
            apiKeyInput = "",
            modelInput = "",
            error = null,
            isSaving = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDeleteKey = {},
            onProviderChanged = {},
            onApiKeyChanged = {},
            onModelChanged = {},
            onSaveKey = {},
            onDeleteAccount = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "2. Active Key State")
@Composable
fun SettingsScreenPreview_ActiveKey() {
    androidx.compose.material3.MaterialTheme {
        SettingsScreenContent(
            hasApiKey = true,
            isDeletingKey = false,
            selectedProvider = "openai",
            apiKeyInput = "sk-abc123xyz456",
            modelInput = "",
            error = null,
            isSaving = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDeleteKey = {},
            onProviderChanged = {},
            onApiKeyChanged = {},
            onModelChanged = {},
            onSaveKey = {},
            onDeleteAccount = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "3. Error State")
@Composable
fun SettingsScreenPreview_Error() {
    androidx.compose.material3.MaterialTheme {
        SettingsScreenContent(
            hasApiKey = false,
            isDeletingKey = false,
            selectedProvider = "anthropic",
            apiKeyInput = "invalid_key",
            modelInput = "",
            error = "Invalid API key format.",
            isSaving = false,
            snackbarHostState = remember { SnackbarHostState() },
            onDeleteKey = {},
            onProviderChanged = {},
            onApiKeyChanged = {},
            onModelChanged = {},
            onSaveKey = {},
            onDeleteAccount = { _, _ -> }
        )
    }
}