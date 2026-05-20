package com.ascend.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.ascend.app.domain.model.User
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.util.Locale

// 1. Stateful Wrapper handling ViewModel lifecycle & Navigation Side Effects
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToPhysiqueSetup: () -> Unit,
    onNavigateToInterests: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    ProfileScreenContent(
        isLoading = state.isLoading,
        user = state.user,
        completedQuestCount = state.completedQuestCount,
        achievements = state.achievements,
        isUploadingAvatar = isUploadingAvatar,
        onNavigateToPhysiqueSetup = onNavigateToPhysiqueSetup,
        onNavigateToInterests = onNavigateToInterests,
        onIntent = viewModel::onIntent
    )
}

// 2. Stateless UI Composable safe for Previews
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    isLoading: Boolean,
    user: User?,
    completedQuestCount: Int,
    achievements: List<AchievementItem>,
    isUploadingAvatar: Boolean,
    onNavigateToPhysiqueSetup: () -> Unit,
    onNavigateToInterests: () -> Unit,
    onIntent: (ProfileIntent) -> Unit,
) {
    Scaffold(
        containerColor = DarkColors.Void,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM STATUS",
                        color = DarkColors.TextPrimary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        fontSize = 16.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkColors.Abyss)
            )
        }
    ) { padding ->

        // ── System Loading State ─────────────────────────────────────
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(DarkColors.Void), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkColors.Arcane)
            }
            return@Scaffold
        }

        // ── Error / Null State ───────────────────────────────────────
        if (user == null) {
            Box(Modifier.fillMaxSize().background(DarkColors.Void), contentAlignment = Alignment.Center) {
                Text("⚠️ CANNOT DECRYPT HUNTER DATA", color = DarkColors.TextMuted, fontWeight = FontWeight.Bold)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkColors.Void)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Status Window Card ──────────────────────────────────
            item {
                SystemPanel(glowColor = DarkColors.Arcane) {
                    Text(
                        text = "◈ STATUS WINDOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = DarkColors.Arcane
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AvatarPicker(
                            currentUrl = user.avatarUrl,
                            username = user.username,
                            isUploading = isUploadingAvatar,
                            onImageSelected = { base64 -> onIntent(ProfileIntent.UploadAvatar(base64)) }
                        )
                        Column {
                            Text(
                                text = user.username.uppercase(Locale.ROOT),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = DarkColors.TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkColors.Arcane.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${user.rankTitle} · ${user.classTitle}".uppercase(Locale.ROOT),
                                    fontSize = 10.sp,
                                    color = DarkColors.Arcane,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Hunter Stats Rows
                    StatRow("LEVEL", "${user.level}", DarkColors.Arcane)
                    StatRow("TOTAL XP", "${if (user.totalXp > 0) user.totalXp else user.currentXp} XP", DarkColors.Gold)
                    StatRow("NEXT LEVEL", "${user.xpToNext - user.currentXp} XP", DarkColors.Cyan)
                    StatRow("QUESTS COMPLETED", "$completedQuestCount", PurpleLight)
                    StatRow("HP", "${user.hp} / ${user.maxHp}", DangerRed)
                }
            }

            // ── XP Progress Bar Card ─────────────────────────────────
            item {
                SystemPanel(glowColor = DarkColors.Cyan) {
                    Text(
                        text = "◈ XP PROGRESS",
                        fontSize = 10.sp,
                        color = DarkColors.TextMuted,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(14.dp))
                    StatBar(
                        label      = "LVL ${user.level} → ${user.level + 1}",
                        fraction   = user.xpFraction,
                        current    = user.currentXp,
                        max        = user.xpToNext,
                        gradient   = Gradients.EnergyStream,
                        labelColor = DarkColors.Cyan
                    )
                }
            }

            // ── Achievements Section ─────────────────────────────────
            item {
                AchievementsSection(achievements = achievements)
            }

            // ── System Settings / Actions Card ────────────────────────
            item {
                SystemPanel(glowColor = BorderGlow) {
                    Text(
                        text = "◈ SETTINGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = DarkColors.TextMuted
                    )
                    Spacer(Modifier.height(12.dp))

                    // Edit quest areas / interests
                    AscendButton(
                        text     = "⚔  EDIT QUEST AREAS",
                        onClick  = onNavigateToInterests,
                        gradient = listOf(CyanAccent, PurplePrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Edit physique profile
                    AscendButton(
                        text     = "UPDATE PHYSIQUE PROFILE",
                        onClick  = onNavigateToPhysiqueSetup,
                        gradient = listOf(PurplePrimary, CyanAccent),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onIntent(ProfileIntent.Logout) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed)
                    ) {
                        Text(
                            text = "LOGOUT SYSTEM",
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DarkColors.TextMuted,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = accentColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Preview(showBackground = true, name = "1. Active Profile Decrypted")
@Composable
fun ProfileScreenPreview_Success() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = false,
            user = User(
                id = "usr_101",
                email = "hunter@ascend.app",
                username = "Sung Jinwoo",
                level = 26,
                currentXp = 680,
                xpToNext = 1200,
                avatarUrl = null,
                totalXp = 24500,
                hp = 100,
                maxHp = 100
            ),
            completedQuestCount = 42,
            achievements = listOf(
                AchievementItem("a1", "Enter the System", "AWAKENED", "👁️", true, "2026-05-10"),
                AchievementItem("a2", "Clear 1st Quest", "FIRST BLOOD", "⚔️", true, "2026-05-11"),
                AchievementItem("a3", "7-Day Streak", "IRON WILL", "🛡️", false, null)
            ),
            isUploadingAvatar = false,
            onNavigateToPhysiqueSetup = {},
            onNavigateToInterests = {},
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Decryption Error State")
@Composable
fun ProfileScreenPreview_NullUser() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = false,
            user = null,
            completedQuestCount = 0,
            achievements = emptyList(),
            isUploadingAvatar = false,
            onNavigateToPhysiqueSetup = {},
            onNavigateToInterests = {},
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Decrypting Data Loader")
@Composable
fun ProfileScreenPreview_Loading() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = true,
            user = null,
            completedQuestCount = 0,
            achievements = emptyList(),
            isUploadingAvatar = false,
            onNavigateToPhysiqueSetup = {},
            onNavigateToInterests = {},
            onIntent = {}
        )
    }
}