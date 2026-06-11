package com.ascend.app.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ascend.app.domain.model.Achievement
import com.ascend.app.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ascend.app.domain.model.User
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.profile.share.ShareCardSheet
import java.util.Locale

import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProfileEffect.NavigateToLogin -> onNavigateToLogin()
                is ProfileEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ProfileScreenContent(
        isLoading = state.isLoading,
        user = state.user,
        completedQuestCount = state.completedQuestCount,
        achievements = state.achievements,
        isUploadingAvatar = isUploadingAvatar,
        currentStreak = state.currentStreak,
        snackbarHostState = snackbarHostState,
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
    currentStreak: Int,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onNavigateToPhysiqueSetup: () -> Unit,
    onNavigateToInterests: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToAttributes: () -> Unit,
    onIntent: (ProfileIntent) -> Unit,
) {
    var showShareSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF07070B),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM STATUS",
                        fontFamily = orbitron,
                        color = ReactPurple,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
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

        when {
            isLoading -> Box(
                Modifier.fillMaxSize().background(Color(0xFF07070B)).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ReactPurple, strokeWidth = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "DECRYPTING HUNTER DATA...",
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp, letterSpacing = 2.sp,
                        color = ReactInkDim, fontWeight = FontWeight.Black
                    )
                }
            }
            user == null -> Box(
                Modifier.fillMaxSize().background(Color(0xFF07070B)).padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 48.sp, modifier = Modifier.alpha(0.5f))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "CANNOT DECRYPT HUNTER DATA",
                        fontFamily = orbitron,
                        fontSize = 13.sp, letterSpacing = 2.sp,
                        color = ReactInkDim, fontWeight = FontWeight.Black
                    )
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07070B))
                    .gridBackground()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── HERO PANEL ──
                    item {
                        val scope = rememberCoroutineScope()
                        HeroPanel(
                            user = user,
                            isUploadingAvatar = isUploadingAvatar,
                            onAvatarSelected = { base64 ->
                                onIntent(ProfileIntent.UploadAvatar(base64))
                            },
                            onAvatarError = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    }

                    // ── 2×2 STATS GRID ──
                    item {
                        StatsGrid(
                            level = user.level,
                            totalXp = if (user.totalXp > 0) user.totalXp else user.currentXp,
                            quests = completedQuestCount,
                            streak = currentStreak
                        )
                    }

                    // ── ACHIEVEMENTS ──
                    item {
                        AchievementsRow(achievements = achievements)
                    }

                    // ── ACTIONS PANEL ──
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
            streak = currentStreak,
            onDismiss = { showShareSheet = false }
        )
    }
}

/* ============================================================
 *  HERO PANEL
 * ============================================================ */
@Composable
private fun HeroPanel(
    user: User,
    isUploadingAvatar: Boolean,
    onAvatarSelected: (String) -> Unit,
    onAvatarError: (String) -> Unit = {}
) {
    val rank = rankForLevel(user.level)
    val rankCol = rankColor(rank)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                scope.launch {
                    val base64 = withContext(Dispatchers.IO) {
                        ImageUtils.uriToBase64(context, uri)
                    }
                    if (base64 != null) {
                        onAvatarSelected(base64)
                    } else {
                        onAvatarError("Could not read image. Try a different photo.")
                    }
                }
            }
        }
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ringSpin")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing), RepeatMode.Restart),
        label = "outer"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(15_000, easing = LinearEasing), RepeatMode.Restart),
        label = "inner"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 15.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = ReactCyan.copy(alpha = 0.5f),
                spotColor = ReactCyan.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(ReactPanel.copy(alpha = 0.85f))
            .border(1.dp, ReactCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .scanlineHorizontal()
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.10f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ReactPurple.copy(alpha = 0.4f),
                            ReactCyan.copy(alpha = 0.4f),
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
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(outerRotation)
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    ReactPurple.copy(alpha = 0.5f),
                                    ReactPurple.copy(alpha = 0.0f),
                                    ReactPurple.copy(alpha = 0.5f)
                                )
                            ),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .rotate(innerRotation)
                        .border(
                            1.dp,
                            Brush.sweepGradient(
                                listOf(
                                    ReactCyan.copy(alpha = 0.6f),
                                    ReactCyan.copy(alpha = 0.0f),
                                    ReactCyan.copy(alpha = 0.6f)
                                )
                            ),
                            CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(15.dp, CircleShape, ambientColor = ReactPurple, spotColor = ReactPurple)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(ReactPurple, ReactCyan)))
                        .clickable { 
                            if (!isUploadingAvatar) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingAvatar) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    } else if (user.avatarUrl != null) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AvatarLetter(user.username)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(40.dp)
                        .shadow(12.dp, RoundedCornerShape(10.dp), ambientColor = rankCol, spotColor = rankCol)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ReactPanel)
                        .border(1.5.dp, rankCol, RoundedCornerShape(10.dp)),
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

            Text(
                user.username.uppercase(Locale.ROOT),
                fontFamily = orbitron,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                color = ReactInk,
                style = TextStyle(shadow = Shadow(ReactPurple.copy(alpha = 0.4f), blurRadius = 12f))
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AWAKENED PLAYER",
                fontFamily = jetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = ReactInkDim
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("HP", fontFamily = jetBrainsMono, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = ReactRed)
                Text("${user.hp} / ${user.maxHp}", fontFamily = jetBrainsMono, fontSize = 10.sp, letterSpacing = 1.sp, color = ReactInk)
            }
            Spacer(Modifier.height(8.dp))
            val hpFraction = (user.hp.toFloat() / user.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
            val translateX by infiniteTransition.animateFloat(
                initialValue = -20f, targetValue = 20f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                label = "translate"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ReactPanelLine)
                    .border(1.dp, ReactPanelLine, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(hpFraction)
                        .shadow(8.dp, RoundedCornerShape(3.dp), ambientColor = ReactRed, spotColor = ReactRed)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF93000A), ReactRed)),
                            RoundedCornerShape(3.dp)
                        )
                        .clip(RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(20.dp)
                            .absoluteOffset(x = translateX.dp)
                            .background(
                                Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f)))
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
 *  2×2 STATS GRID (With Smooth Rolling Odometer)
 * ============================================================ */
@Composable
private fun StatsGrid(level: Int, totalXp: Int, quests: Int, streak: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "LEVEL", value = level, accent = ReactPurple,
                bordered = true, modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "TOTAL XP", value = totalXp, accent = ReactInk,
                bordered = false, modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "QUESTS", value = quests, accent = ReactInk,
                bordered = false, modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "STREAK", value = streak, accent = ReactGold,
                bordered = true, topRightEmoji = "🔥", modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: Int,
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
            .reactStyleCard(selected = bordered, glowColor = accent, cornerRadius = 10.dp)
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
                color = ReactInkDim
            )
            Spacer(Modifier.height(6.dp))

            RollingDigitCounter(
                count = value,
                color = accent,
                textStyle = TextStyle(
                    fontFamily = orbitron,
                    fontSize = if (label == "LEVEL") 28.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum",
                    letterSpacing = 1.sp,
                    shadow = if (bordered) Shadow(accent.copy(alpha = 0.4f), blurRadius = 10f) else null
                )
            )
        }
    }
}



/* ============================================================
 *  ACHIEVEMENTS
 * ============================================================ */
@Composable
private fun AchievementsRow(achievements: List<Achievement>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ReactPanel)
            .border(1.dp, ReactPanelLine, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◈ ", fontFamily = orbitron, fontSize = 16.sp, color = ReactCyan)
                Text(
                    "ACHIEVEMENTS",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = ReactInk
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
                    repeat(3) { DiamondBadge(label = "???", icon = Icons.Filled.Lock, color = ReactInkDim, locked = true) }
                } else {
                    achievements.forEach { ach ->
                        DiamondBadge(
                            label = if (ach.earned) ach.title else "???",
                            icon = achievementIcon(ach.icon),
                            color = if (ach.earned) ReactGold else ReactInkDim,
                            locked = !ach.earned
                        )
                    }
                    if (achievements.size < 3) {
                        repeat(3 - achievements.size) {
                            DiamondBadge(label = "???", icon = Icons.Filled.Lock, color = ReactInkDim, locked = true)
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
        Box(
            modifier = Modifier
                .size(64.dp)
                .rotate(45f)
                .then(
                    if (!locked) Modifier.shadow(14.dp, RoundedCornerShape(10.dp), ambientColor = color, spotColor = color)
                    else Modifier
                )
                .clip(RoundedCornerShape(10.dp))
                .background(ReactPanel)
                .border(1.dp, if (locked) ReactPanelLine else color.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = color,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(-45f)
            )
        }
        Text(
            label,
            fontFamily = jetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = if (locked) ReactInkDim else ReactInk,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
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
    "sword", "blood"    -> Icons.Filled.Bolt
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
        ActionRow(icon = Icons.Filled.BarChart, label = "View Player Stats", accent = ReactPurple, onClick = onViewStats)
        ActionRow(icon = Icons.Filled.Bolt, label = "RPG Attributes", accent = ReactGold, onClick = onViewAttributes)
        ActionRow(icon = Icons.Filled.FitnessCenter, label = "Update Physique", accent = ReactPurple, onClick = onUpdatePhysique)
        ActionRow(icon = Icons.Filled.AutoAwesome, label = "Edit Quest Areas", accent = ReactCyan, onClick = onEditQuestAreas)
        ActionRow(icon = Icons.Filled.Share, label = "Share Hunter Rank", accent = ReactPurple, onClick = onShare)

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ReactRed.copy(alpha = 0.05f))
                .border(1.dp, ReactRed.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clickable { onLogout() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ReactRed, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "SYSTEM LOGOUT",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    color = ReactRed
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
            .background(ReactPanel)
            .border(1.dp, ReactPanelLine, RoundedCornerShape(10.dp))
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
                    color = ReactInk
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                tint = ReactInkDim.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/* ============================================================
 *  HELPERS
 * ============================================================ */


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
            currentStreak = 7,
            onNavigateToPhysiqueSetup = {},
            onNavigateToInterests = {},
            onNavigateToStats = {},
            onNavigateToAttributes = {},
            onIntent = {}
        )
    }
}