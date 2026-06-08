package com.ascend.app.ui.dashboard

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.QuestStatus
import com.ascend.app.domain.model.QuestType
import com.ascend.app.domain.model.User
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.levelup.LevelUpModal
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DangerRed
import com.ascend.app.ui.theme.GoldAccent
import com.ascend.app.ui.theme.PanelMid
import com.ascend.app.ui.theme.PurpleLight
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.SuccessGreen
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextMuted
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLevelUpLevel by remember { mutableStateOf<Int?>(null) }
    var levelUpStatDeltas by remember { mutableStateOf<List<com.ascend.app.domain.model.StatDelta>>(emptyList()) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.ShowSnackbar -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
                is DashboardEffect.LevelUp -> {
                    showLevelUpLevel = effect.newLevel
                    levelUpStatDeltas = effect.statDeltas
                }
                is DashboardEffect.NavigateTo -> onNavigate(effect.route)
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardScreenContent(
            user = state.user,
            activeQuests = state.activeQuests,
            todayHabits = state.todayHabits,
            isGenerating = state.isGenerating,
            snackbarHostState = snackbarHostState,
            onIntent = viewModel::onIntent,
            onNavigate = onNavigate
        )

        showLevelUpLevel?.let { newLevel ->
            LevelUpModal(
                newLevel = newLevel,
                titleUnlocked = if (newLevel % 5 == 0) "Ascended Hunter" else null, // Mock title every 5 levels
                statDeltas = levelUpStatDeltas,
                onContinue = { showLevelUpLevel = null }
            )
        }
    }
}

@Composable
fun DashboardScreenContent(
    user: User?,
    activeQuests: List<Quest>,
    todayHabits: List<Habit>,
    isGenerating: Boolean = false,
    snackbarHostState: SnackbarHostState,
    onIntent: (DashboardIntent) -> Unit,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SystemBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── HUNTER HEADER (combined avatar + name + HP + XP) ──
            item {
                HunterHeaderPanel(user = user, onNotificationsClick = { onNavigate("notifications") })
            }

            // ── GENERATE QUESTS button ──
            item {
                GenerateQuestsButton(
                    isLoading = isGenerating,
                    onClick = { onIntent(DashboardIntent.GenerateQuests) }
                )
            }

            val dailyQuests = activeQuests.filter { it.type.equals("daily") }
            val otherQuests = activeQuests.filter { !it.type.equals("daily") }

            // ── DAILY QUESTS ──
            item {
                SectionHeader(
                    title = "DAILY QUESTS",
                    rightText = "${dailyQuests.size} ACTIVE"
                )
            }

            if (dailyQuests.isEmpty()) {
                item { EmptyQuestsPanel() }
            } else {
                items(dailyQuests, key = { it.id }) { quest ->
                    QuestCard(
                        quest = quest,
                        onComplete = { onIntent(DashboardIntent.CompleteQuest(quest.id)) },
                        onSkip = { onIntent(DashboardIntent.SkipQuest(quest.id)) }
                    )
                }
            }

            // ── OTHER QUESTS ──
            if (otherQuests.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionHeader(
                        title = "OTHER QUESTS",
                        rightText = "${otherQuests.size} ACTIVE"
                    )
                }
                items(otherQuests, key = { it.id }) { quest ->
                    QuestCard(
                        quest = quest,
                        onComplete = { onIntent(DashboardIntent.CompleteQuest(quest.id)) },
                        onSkip = { onIntent(DashboardIntent.SkipQuest(quest.id)) }
                    )
                }
            }

            // ── DAILY MISSIONS ──

            items(todayHabits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    onComplete = { onIntent(DashboardIntent.CompleteHabit(habit.id)) }
                )
            }
        }
    }
}

/* ============================================================
 *  HUNTER HEADER PANEL — avatar + name + rank/lvl + HP + XP
 * ============================================================ */
@Composable
fun HunterHeaderPanel(user: User?, onNotificationsClick: () -> Unit) {
    val username = user?.username ?: "HUNTER"
    val level = user?.level ?: 1
    val hp = user?.hp ?: 100
    val maxHp = user?.maxHp ?: 100
    val currentXp = user?.currentXp ?: 0
    val xpToNext = (user?.xpToNext ?: 1).coerceAtLeast(1)
    val rank = rankForLevel(level)

    // Throb for rank badge
    val infiniteTransition = rememberInfiniteTransition(label = "rankThrob")
    val rankGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rg"
    )

    val animatedXp by animateFloatAsState(
        targetValue = (currentXp.toFloat() / xpToNext).coerceIn(0f, 1f),
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "xp"
    )

    SystemPanel(glowColor = PurplePrimary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar square w/ first letter (web: 50×50 gradient bg)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(PurplePrimary, CyanAccent)))
                    .shadow(16.dp, RoundedCornerShape(10.dp),
                        ambientColor = PurplePrimary, spotColor = CyanAccent),
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatarUrl != null) {
                    coil.compose.AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        username.firstOrNull()?.uppercase() ?: "H",
                        fontFamily = orbitron,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Name + rank/lvl
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username.uppercase(Locale.ROOT),
                    fontFamily = orbitron,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    RankBadge(rank = rank, glow = rankGlow)
                    Text(
                        text = "LV ",
                        fontFamily = jetBrainsMono,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "$level",
                        fontFamily = jetBrainsMono,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent
                    )
                }
            }

            // HP cluster + Bell Icon
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = PurpleLight
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            null,
                            tint = DangerRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "HP $hp/$maxHp",
                            fontFamily = jetBrainsMono,
                            fontSize = 10.sp,
                            color = DangerRed
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                GlowBar(
                    fraction = (hp.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f),
                    gradient = listOf(DangerRed, Color(0xFFFF6B6B)),
                    height = 6.dp
                )
            }
        }

        // XP row
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "EXPERIENCE",
                fontFamily = jetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = PurpleLight,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatNum(currentXp),
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = CyanAccent
                )
                Text(
                    text = " / ${formatNum(xpToNext)} XP",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        GlowBar(
            fraction = animatedXp,
            gradient = listOf(PurplePrimary, CyanAccent),
            height = 8.dp,
            withLeadingEdge = true
        )
    }
}

@Composable
private fun RankBadge(rank: String, glow: Float) {
    val color = rankColor(rank)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .shadow(
                (glow * 8).dp,
                RoundedCornerShape(4.dp),
                ambientColor = color,
                spotColor = color
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = rank,
            fontFamily = orbitron,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            color = color
        )
    }
}

/* ============================================================
 *  GENERATE QUESTS button
 * ============================================================ */
@Composable
fun GenerateQuestsButton(
    isLoading: Boolean,
    isFailed: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_pulse")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "btn_glow"
    )

    val brush = if (isFailed)
        Brush.horizontalGradient(listOf(Color(0xFFDC143C), Color(0xFF8B0000)))
    else
        Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                (glow * 22).dp,
                RoundedCornerShape(10.dp),
                ambientColor = if (isFailed) DangerRed else PurplePrimary,
                spotColor = if (isFailed) DangerRed else CyanAccent
            )
            .clip(RoundedCornerShape(10.dp))
            .background(brush)
            .alpha(if (isLoading) 0.7f else 1f)
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "SUMMONING…",
                    fontFamily = orbitron,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isFailed) Icons.Filled.Warning else Icons.Filled.Bolt,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isFailed) "RETRY GENERATION" else "GENERATE NEW QUESTS",
                    fontFamily = orbitron,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color.White
                )
            }
        }
    }
}

/* ============================================================
 *  SECTION HEADER (web: SectionHead with right counter)
 * ============================================================ */
@Composable
fun SectionHeader(title: String, rightText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Rotated square accent (web: w-2 h-2 bg rotate-45)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(CyanAccent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = orbitron,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                color = TextPrimary
            )
        }
        if (rightText != null) {
            Text(
                text = rightText,
                fontFamily = jetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = TextMuted.copy(alpha = 0.7f)
            )
        }
    }
}

/* ============================================================
 *  QUEST CARD — rank chip + cat chip + xp chip + title + actions
 * ============================================================ */
@Composable
fun QuestCard(quest: Quest, onComplete: () -> Unit, onSkip: () -> Unit) {
    val rank = rankForDifficulty(quest.difficulty)
    val rankCol = rankColor(rank)
    val catCol = skillAreaColor(quest.skillArea)

    SystemPanel(glowColor = rankCol) {
        // Top row: rank chip + cat chip + XP chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rank chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(rankCol.copy(alpha = 0.15f))
                    .border(1.dp, rankCol.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$rank-RANK",
                    fontFamily = orbitron,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = rankCol
                )
            }
            // Category chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(catCol.copy(alpha = 0.12f))
                    .border(1.dp, catCol.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = quest.skillArea.uppercase(Locale.ROOT),
                    fontFamily = jetBrainsMono,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = catCol
                )
            }
            Spacer(Modifier.weight(1f))
            // XP chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldAccent.copy(alpha = 0.12f))
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "+${quest.xpReward} XP",
                    fontFamily = jetBrainsMono,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = GoldAccent
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        // Title
        Text(
            text = quest.title,
            fontFamily = orbitron,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(5.dp))
        // Desc
        Text(
            text = quest.description,
            fontFamily = jetBrainsMono,
            fontSize = 12.sp,
            color = TextSecondary.copy(alpha = 0.85f),
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(12.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // SKIP ghost
            Box(
                modifier = Modifier
                    .width(78.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BorderGlow, RoundedCornerShape(8.dp))
                    .clickable { onSkip() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SKIP",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TextMuted
                )
            }
            // COMPLETE QUEST gradient
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .shadow(12.dp, RoundedCornerShape(8.dp),
                        ambientColor = PurplePrimary, spotColor = CyanAccent)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)))
                    .clickable { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "COMPLETE QUEST",
                        fontFamily = orbitron,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  HABIT / MISSION CARD
 * ============================================================ */
@Composable
fun HabitCard(habit: Habit, onComplete: () -> Unit) {
    val glowColor = if (habit.completedToday) SuccessGreen else BorderGlow

    SystemPanel(glowColor = glowColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    fontFamily = orbitron,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (habit.completedToday) TextMuted else TextPrimary,
                    style = if (habit.completedToday)
                        TextStyle(textDecoration = TextDecoration.LineThrough) else TextStyle.Default
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (habit.currentStreak > 0) {
                        // Streak chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, GoldAccent.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 ${habit.currentStreak}d",
                                fontFamily = jetBrainsMono,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "+${habit.xpReward} XP",
                        fontFamily = jetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = TextMuted.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (habit.completedToday) {
                // Done box w/ green check
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(SuccessGreen.copy(alpha = 0.18f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                        .shadow(8.dp, RoundedCornerShape(9.dp),
                            ambientColor = SuccessGreen, spotColor = SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // DONE outline button
                Box(
                    modifier = Modifier
                        .width(78.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CyanAccent, RoundedCornerShape(8.dp))
                        .clickable { onComplete() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  EMPTY STATE
 * ============================================================ */
@Composable
private fun EmptyQuestsPanel() {
    SystemPanel(glowColor = BorderGlow) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⚔️", fontSize = 26.sp, modifier = Modifier.alpha(0.5f))
            Text(
                text = "All quests cleared. Generate more.",
                fontFamily = jetBrainsMono,
                fontSize = 12.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

/* ============================================================
 *  GLOW BAR — reusable progress bar with optional leading edge
 * ============================================================ */
@Composable
fun GlowBar(
    fraction: Float,
    gradient: List<Color>,
    height: androidx.compose.ui.unit.Dp,
    withLeadingEdge: Boolean = false // kept for compatibility, but we will apply the new pulse to all glow bars or just those that use it
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlowAnim")
    val translateX by infiniteTransition.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "translate"
    )
    val opacityFloat by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "opacity"
    )
    val pulseOpacity = 1f - kotlin.math.abs(1f - opacityFloat)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(PanelMid)
            .border(1.dp, BorderGlow.copy(alpha = 0.2f), RoundedCornerShape(height / 2))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(gradient), RoundedCornerShape(height / 2))
                .clip(RoundedCornerShape(height / 2))
        ) {
            if (fraction > 0.01f) {
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

/* ============================================================
 *  HELPERS — rank, color, formatting
 * ============================================================ */
fun rankForLevel(level: Int): String = when {
    level >= 80 -> "SS"
    level >= 60 -> "S"
    level >= 45 -> "A"
    level >= 30 -> "B"
    level >= 18 -> "C"
    level >= 8  -> "D"
    else        -> "E"
}

fun rankForDifficulty(difficulty: Int): String = when (difficulty) {
    1 -> "E"
    2 -> "D"
    3 -> "C"
    4 -> "B"
    5 -> "A"
    6 -> "S"
    else -> "SS"
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

fun skillAreaColor(area: String): Color = when (area.lowercase(Locale.ROOT)) {
    in listOf("strength", "endurance", "mobility", "physical", "running", "calisthenics") -> Color(0xFFFFB4AB)
    in listOf("coding", "tech", "technology", "ai", "ml", "devops") -> CyanAccent
    in listOf("mental", "focus", "meditation", "reading", "learning") -> PurpleLight
    in listOf("social", "networking", "speaking", "leadership") -> GoldAccent
    in listOf("finance", "budgeting", "investing", "saving") -> Color(0xFF732EE4)
    else -> CyanAccent
}

fun formatNum(n: Int): String =
    if (n >= 1000) "%,d".format(n) else n.toString()

/* ============================================================
 *  PREVIEW
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "Dashboard (Populated)")
@Composable
fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreenContent(
            user = User(
                id = "u1",
                email = "hunter@ascend.com",
                username = "Sung Jinwoo",
                level = 12,
                currentXp = 450,
                xpToNext = 1000,
                avatarUrl = null,
                totalXp = 4500,
                hp = 85,
                maxHp = 100
            ),
            activeQuests = listOf(
                Quest(
                    id = "q1",
                    title = "100 PUSH-UPS",
                    description = "Complete 100 push-ups consecutively or in sets of 20.",
                    type = QuestType.DAILY,
                    difficulty = 3,
                    xpReward = 50,
                    status = QuestStatus.ACTIVE,
                    skillArea = "Strength",
                    isAiGenerated = true
                ),
                Quest(
                    id = "q2",
                    title = "REFACTOR AUTH MODULE",
                    description = "Move OAuth flow into its own use case. Delete the old fragments.",
                    type = QuestType.DAILY,
                    difficulty = 5,
                    xpReward = 150,
                    status = QuestStatus.ACTIVE,
                    skillArea = "Coding",
                    isAiGenerated = true
                )
            ),
            todayHabits = listOf(
                Habit(
                    id = "h1",
                    title = "Morning Meditation",
                    frequency = "Daily",
                    xpReward = 15,
                    currentStreak = 4,
                    longestStreak = 12,
                    completedToday = false
                ),
                Habit(
                    id = "h2",
                    title = "Drink 2L Water",
                    frequency = "Daily",
                    xpReward = 5,
                    currentStreak = 12,
                    longestStreak = 15,
                    completedToday = true
                )
            ),
            isGenerating = false,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "Dashboard (Empty Quests)")
@Composable
fun DashboardEmptyPreview() {
    MaterialTheme {
        DashboardScreenContent(
            user = User(
                id = "u1", email = "", username = "Kairo",
                level = 1, currentXp = 0, xpToNext = 100,
                avatarUrl = null, totalXp = 0, hp = 100, maxHp = 100
            ),
            activeQuests = emptyList(),
            todayHabits = emptyList(),
            isGenerating = false,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "2. Quest Card")
@Composable
fun QuestCardPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            QuestCard(
                quest = Quest(
                    id = "q1",
                    title = "10 KM RUN",
                    description = "Push your limits. Maintain a steady pace and complete a 10 km run before midnight.",
                    type = QuestType.DAILY,
                    difficulty = 4,
                    xpReward = 150,
                    status = QuestStatus.ACTIVE,
                    skillArea = "Endurance",
                    isAiGenerated = false
                ),
                onComplete = {},
                onSkip = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "3. Habit Cards (States)")
@Composable
fun HabitCardPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Pending Habit
            HabitCard(
                habit = Habit(
                    id = "h1",
                    title = "Read 10 Pages",
                    frequency = "Daily",
                    xpReward = 10,
                    currentStreak = 4,
                    longestStreak = 10,
                    completedToday = false
                ),
                onComplete = {}
            )

            // Completed Habit
            HabitCard(
                habit = Habit(
                    id = "h2",
                    title = "Drink 2L Water",
                    frequency = "Daily",
                    xpReward = 5,
                    currentStreak = 12,
                    longestStreak = 15,
                    completedToday = true
                ),
                onComplete = {}
            )
        }
    }
}