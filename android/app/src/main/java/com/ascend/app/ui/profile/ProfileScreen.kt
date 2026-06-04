package com.ascend.app.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import com.ascend.app.ui.profile.share.ShareCardSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.domain.model.Achievement
import com.ascend.app.domain.model.User
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.theme.*
import java.util.Locale

/* ============================================================
 *  ROOT
 * ============================================================ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToPhysiqueSetup: () -> Unit,
    onNavigateToInterests: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAttributes: () -> Unit,
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
        bestStreak = state.bestStreak,
        onNavigateToPhysiqueSetup = onNavigateToPhysiqueSetup,
        onNavigateToInterests = onNavigateToInterests,
        onNavigateToStats = onNavigateToStats,
        onNavigateToAttributes = onNavigateToAttributes,
        onIntent = viewModel::onIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    isLoading: Boolean,
    user: User?,
    completedQuestCount: Int,
    achievements: List<Achievement>,
    isUploadingAvatar: Boolean,
    bestStreak: Int,
    onNavigateToPhysiqueSetup: () -> Unit,
    onNavigateToInterests: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAttributes: () -> Unit,
    onIntent: (ProfileIntent) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SystemBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM STATUS",
                        fontFamily = orbitron,
                        color = PurpleLight,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 16.sp,
                        style = TextStyle(shadow = Shadow(PurpleLight.copy(alpha = 0.5f), blurRadius = 10f))
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SystemBlack.copy(alpha = 0.85f)
                )
            )
        }
    ) { padding ->

        when {
            isLoading -> Box(
                Modifier.fillMaxSize().background(SystemBlack).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PurplePrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "DECRYPTING HUNTER DATA...",
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp, letterSpacing = 2.sp,
                        color = TextMuted, fontWeight = FontWeight.Black
                    )
                }
            }
            user == null -> Box(
                Modifier.fillMaxSize().background(SystemBlack).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp, modifier = Modifier.alpha(0.5f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "CANNOT DECRYPT HUNTER DATA",
                        fontFamily = orbitron,
                        fontSize = 13.sp, letterSpacing = 2.sp,
                        color = TextMuted, fontWeight = FontWeight.Black
                    )
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SystemBlack)
                    .gridBackground()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(com.ascend.app.ui.theme.LocalSpacing.current.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(com.ascend.app.ui.theme.LocalSpacing.current.itemSpacing)
                ) {
                    // ── HERO PANEL (avatar + rank + name + HP) ──
                    item {
                        HeroPanel(
                            user = user,
                            isUploadingAvatar = isUploadingAvatar,
                            onAvatarSelected = { base64 ->
                                onIntent(ProfileIntent.UploadAvatar(base64))
                            }
                        )
                    }

                    // ── 2×2 STATS GRID ──
                    item {
                        StatsGrid(
                            level = user.level,
                            totalXp = if (user.totalXp > 0) user.totalXp else user.currentXp,
                            quests = completedQuestCount,
                            streak = bestStreak
                        )
                    }

                    // ── ACHIEVEMENTS (horizontal scroll diamonds) ──
                    item {
                        AchievementsRow(achievements = achievements)
                    }

                    item {
                        ActionsPanel(
                            user = user,
                            onUpdatePhysique = onNavigateToPhysiqueSetup,
                            onEditQuestAreas = onNavigateToInterests,
                            onViewStats = onNavigateToStats,
                            onViewAttributes = onNavigateToAttributes,
                            onShare = { showShareSheet = true },
                            onLogout = { onIntent(ProfileIntent.Logout) }
                        )
                    }
                }
            }
        }
    }

    if (showShareSheet && user != null) {
        ShareCardSheet(
            user = user,
            completedQuests = completedQuestCount,
            streak = bestStreak,
            onDismiss = { showShareSheet = false }
        )
    }
}

/* ============================================================
 *  HERO PANEL
 *  Big centered avatar with spinning rings + rank badge + HP
 * ============================================================ */
@Composable
private fun HeroPanel(
    user: User,
    isUploadingAvatar: Boolean,
    onAvatarSelected: (String) -> Unit
) {
    val rank = rankForLevel(user.level)
    val rankCol = rankColor(rank)

    // Spinning rings (web: animate-spin 10s + reverse 15s)
    val infiniteTransition = rememberInfiniteTransition(label = "ringSpin")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(10_000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "outer"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(15_000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "inner"
    )

    // HP bar leading edge pulse
    val edgePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E0E1A).copy(alpha = 0.85f))
            .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .shadow(15.dp, RoundedCornerShape(14.dp),
                ambientColor = CyanAccent, spotColor = CyanAccent)
            .scanlineHorizontal()
            .padding(24.dp)
    ) {
        // Decorative radial halo behind
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.10f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = 0.4f),
                            CyanAccent.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .blur(32.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Avatar block w/ spinning rings + rank badge ──
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(outerRotation)
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    PurpleLight.copy(alpha = 0.5f),
                                    PurpleLight.copy(alpha = 0.0f),
                                    PurpleLight.copy(alpha = 0.5f)
                                )
                            ),
                            CircleShape
                        )
                )
                // Inner ring
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .rotate(innerRotation)
                        .border(
                            1.dp,
                            Brush.sweepGradient(
                                listOf(
                                    CyanAccent.copy(alpha = 0.6f),
                                    CyanAccent.copy(alpha = 0.0f),
                                    CyanAccent.copy(alpha = 0.6f)
                                )
                            ),
                            CircleShape
                        )
                )

                // Avatar (uses your AvatarPicker if available, fallback letter)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(PurplePrimary, CyanAccent))
                        )
                        .shadow(15.dp, CircleShape,
                            ambientColor = PurpleLight, spotColor = PurpleLight)
                        .clickable {
                            // Trigger your avatar picker callback path here, e.g. open picker
                            // For now we wire same flow as your AvatarPicker would; you can
                            // replace this Box content with: AvatarPicker(...)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else if (!user.avatarUrl.isNullOrBlank()) {
                        // If you have Coil set up:
                        // AsyncImage(model = user.avatarUrl, ...)
                        // Fallback: show first letter
                        AvatarLetter(user.username)
                    } else {
                        AvatarLetter(user.username)
                    }
                }

                // Rank badge bottom-right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SystemBlack)
                        .border(1.5.dp, rankCol, RoundedCornerShape(10.dp))
                        .shadow(12.dp, RoundedCornerShape(10.dp),
                            ambientColor = rankCol, spotColor = rankCol),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        rank,
                        fontFamily = orbitron,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = rankCol
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Name
            Text(
                user.username.uppercase(Locale.ROOT),
                fontFamily = orbitron,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = TextPrimary,
                style = TextStyle(shadow = Shadow(PurpleLight.copy(alpha = 0.4f), blurRadius = 12f))
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AWAKENED PLAYER",
                fontFamily = jetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = TextMuted
            )

            // HP bar
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "HP",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
                Text(
                    "${user.hp} / ${user.maxHp}",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            val hpFraction = (user.hp.toFloat() / user.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
            val infinitePulse = rememberInfiniteTransition(label = "pulseGlowAnim")
            val translateX by infinitePulse.animateFloat(
                initialValue = -20f, targetValue = 20f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                label = "translate"
            )
            val opacityFloat by infinitePulse.animateFloat(
                initialValue = 0f, targetValue = 2f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                label = "opacity"
            )
            val pulseOpacity = 1f - kotlin.math.abs(1f - opacityFloat)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF35343A))
                    .border(1.dp, BorderGlow.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(hpFraction)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF93000A), Color(0xFFFFB4AB))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                        .clip(RoundedCornerShape(3.dp))
                        .shadow(8.dp, RoundedCornerShape(3.dp),
                            ambientColor = DangerRed, spotColor = DangerRed)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(20.dp)
                            .absoluteOffset(x = translateX.dp)
                            .alpha(pulseOpacity)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarLetter(username: String) {
    Text(
        text = username.firstOrNull()?.uppercase() ?: "?",
        fontFamily = orbitron,
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.4f), blurRadius = 14f))
    )
}

/* ============================================================
 *  2×2 STATS GRID
 * ============================================================ */
@Composable
private fun StatsGrid(level: Int, totalXp: Int, quests: Int, streak: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "LEVEL",
                value = level.toString(),
                accent = PurpleLight,
                bordered = true,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "TOTAL XP",
                value = formatNum(totalXp),
                accent = TextPrimary,
                bordered = false,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "QUESTS",
                value = quests.toString(),
                accent = TextPrimary,
                bordered = false,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "STREAK",
                value = streak.toString(),
                accent = GoldAccent,
                bordered = true,
                topRightEmoji = "🔥",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    bordered: Boolean,
    modifier: Modifier = Modifier,
    topRightEmoji: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emojiBlink")
    val emojiAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "a"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A2E).copy(alpha = 0.85f))
            .border(
                1.dp,
                if (bordered) accent.copy(alpha = 0.4f) else BorderGlow.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .scanlineHorizontal()
            .padding(16.dp)
    ) {
        if (topRightEmoji != null) {
            Text(
                topRightEmoji,
                fontSize = 10.sp,
                color = accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .alpha(emojiAlpha)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontFamily = jetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontFamily = orbitron,
                fontSize = if (label == "LEVEL") 28.sp else 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = accent,
                style = if (bordered) TextStyle(
                    shadow = Shadow(accent.copy(alpha = 0.4f), blurRadius = 10f)
                ) else TextStyle.Default
            )
        }
    }
}

/* ============================================================
 *  ACHIEVEMENTS (diamond row)
 * ============================================================ */
@Composable
private fun AchievementsRow(achievements: List<Achievement>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A2E).copy(alpha = 0.80f))
            .border(1.dp, BorderGlow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "◈ ",
                    fontFamily = orbitron,
                    fontSize = 16.sp,
                    color = CyanAccent
                )
                Text(
                    "ACHIEVEMENTS",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (achievements.isEmpty()) {
                    // Show 3 locked placeholders
                    repeat(3) {
                        DiamondBadge(
                            label = "???",
                            icon = Icons.Filled.Lock,
                            color = TextMuted,
                            locked = true
                        )
                    }
                } else {
                    achievements.forEach { ach ->
                        DiamondBadge(
                            label = if (ach.earned) ach.title else "???",
                            icon = achievementIcon(ach.icon),
                            color = if (ach.earned) GoldAccent else TextMuted,
                            locked = !ach.earned
                        )
                    }
                    // Pad with 1-2 locked if few
                    if (achievements.size < 3) {
                        repeat(3 - achievements.size) {
                            DiamondBadge(
                                label = "???",
                                icon = Icons.Filled.Lock,
                                color = TextMuted,
                                locked = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiamondBadge(
    label: String,
    icon: ImageVector,
    color: Color,
    locked: Boolean
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .alpha(if (locked) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Outer rotated box
        Box(
            modifier = Modifier
                .size(64.dp)
                .rotate(45f)
                .clip(RoundedCornerShape(10.dp))
                .background(SystemBlack)
                .border(1.dp, color.copy(alpha = if (locked) 0.5f else 0.6f), RoundedCornerShape(10.dp))
                .then(
                    if (!locked) Modifier.shadow(
                        14.dp, RoundedCornerShape(10.dp),
                        ambientColor = color, spotColor = color
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = color,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(-45f)  // counter-rotate icon
            )
        }
        Text(
            label,
            fontFamily = jetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = if (locked) TextMuted else TextSecondary,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 13.sp,
            maxLines = 2
        )
    }
}

private fun achievementIcon(key: String?): ImageVector = when (key?.lowercase(Locale.ROOT)) {
    "fire", "streak"    -> Icons.Filled.LocalFireDepartment
    "bolt", "speed"     -> Icons.Filled.Bolt
    "shield", "iron"    -> Icons.Filled.Shield
    "eye", "awakened"   -> Icons.Filled.Visibility
    "sword", "blood"    -> Icons.Filled.Bolt  // swap if you got sword icon
    null, "lock"        -> Icons.Filled.Lock
    else                -> Icons.Filled.Star
}

/* ============================================================
 *  ACTIONS PANEL
 * ============================================================ */
@Composable
private fun ActionsPanel(
    user: User,
    onUpdatePhysique: () -> Unit,
    onEditQuestAreas: () -> Unit,
    onViewStats: () -> Unit,
    onViewAttributes: () -> Unit,
    onShare: () -> Unit,
    onLogout: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionRow(
            icon = Icons.Filled.BarChart,
            label = "View Player Stats",
            accent = PurpleLight,
            onClick = onViewStats
        )
        ActionRow(
            icon = Icons.Filled.Bolt,
            label = "RPG Attributes",
            accent = GoldAccent,
            onClick = onViewAttributes
        )
        ActionRow(
            icon = Icons.Filled.FitnessCenter,
            label = "Update Physique",
            accent = PurpleLight,
            onClick = onUpdatePhysique
        )
        ActionRow(
            icon = Icons.Filled.AutoAwesome,  // closest to "sword" iconography
            label = "Edit Quest Areas",
            accent = CyanAccent,
            onClick = onEditQuestAreas
        )
        ActionRow(
            icon = Icons.Filled.Share,
            label = "Share Hunter Rank",
            accent = PurpleLight,
            onClick = onShare
        )

        Spacer(Modifier.height(6.dp))

        // Logout (destructive)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DangerRed.copy(alpha = 0.05f))
                .border(1.dp, DangerRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout, null,
                    tint = DangerRed,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "SYSTEM LOGOUT",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    color = DangerRed
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1F1F25).copy(alpha = 0.6f),
                        Color(0xFF2A292F).copy(alpha = 0.6f)
                    )
                )
            )
            .border(
                1.dp,
                accent.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Text(
                    label,
                    fontFamily = orbitron,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TextPrimary
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/* ============================================================
 *  HELPERS
 * ============================================================ */
fun Modifier.scanlineHorizontal(): Modifier = drawWithCache {
    val spacing = 4f
    onDrawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(
                Color.Black.copy(alpha = 0.18f),
                start = Offset(0f, y + 1.5f),
                end = Offset(size.width, y + 1.5f),
                strokeWidth = 2f
            )
            y += spacing
        }
    }
}

/** Grid background pattern (web: linear-gradient overlay) */
fun Modifier.gridBackground(): Modifier = drawWithCache {
    val spacing = 32f
    val color = Color(0xFFD2BBFF).copy(alpha = 0.03f)
    onDrawWithContent {
        drawContent()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += spacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += spacing
        }
    }
}

fun rankForLevel(level: Int): String = when {
    level >= 80 -> "SS"
    level >= 60 -> "S"
    level >= 45 -> "A"
    level >= 30 -> "B"
    level >= 18 -> "C"
    level >= 8  -> "D"
    else        -> "E"
}

fun rankColor(rank: String): Color = when (rank) {
    "E"  -> Color(0xFF8B9DA8)
    "D"  -> Color(0xFF7DB0E8)
    "C"  -> CyanAccent
    "B"  -> Color(0xFF8B5CF6)
    "A"  -> PurplePrimary
    "S"  -> GoldAccent
    "SS" -> Color(0xFFFF3D7F)
    else -> CyanAccent
}

fun formatNum(n: Int): String =
    if (n >= 1000) "%,d".format(n) else n.toString()

/* ============================================================
 *  PREVIEWS
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Profile - Active")
@Composable
fun ProfileScreenPreview_Success() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = false,
            user = User(
                id = "usr_101", email = "hunter@ascend.app",
                username = "Kairo",
                level = 23,
                currentXp = 680, xpToNext = 1200,
                avatarUrl = null, totalXp = 4250,
                hp = 85, maxHp = 100
            ),
            completedQuestCount = 142,
            achievements = listOf(
                Achievement("a1", "First Blood",  "BLOOD",    "fire", true, "2026-05-10"),
                Achievement("a2", "Speed Demon",  "SPEED",    "bolt", true, "2026-05-11"),
                Achievement("a3", "Iron Will",    "IRON",     "shield", false, null)
            ),
            isUploadingAvatar = false,
            bestStreak = 7,
            onNavigateToPhysiqueSetup = {},
            onNavigateToInterests = {},
            onNavigateToStats = {},
            onNavigateToAttributes = {},
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Profile - Loading")
@Composable
fun ProfileScreenPreview_Loading() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = true, user = null,
            completedQuestCount = 0, achievements = emptyList(),
            isUploadingAvatar = false,
            bestStreak = 0,
            onNavigateToPhysiqueSetup = {}, onNavigateToInterests = {},
            onNavigateToStats = {}, onNavigateToAttributes = {}, onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Profile - Null")
@Composable
fun ProfileScreenPreview_NullUser() {
    MaterialTheme {
        ProfileScreenContent(
            isLoading = false, user = null,
            completedQuestCount = 0, achievements = emptyList(),
            isUploadingAvatar = false,
            bestStreak = 0,
            onNavigateToPhysiqueSetup = {}, onNavigateToInterests = {},
            onNavigateToStats = {}, onNavigateToAttributes = {}, onIntent = {}
        )
    }
}