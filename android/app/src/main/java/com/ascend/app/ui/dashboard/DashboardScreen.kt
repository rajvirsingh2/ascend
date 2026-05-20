package com.ascend.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.QuestStatus
import com.ascend.app.domain.model.QuestType
import com.ascend.app.domain.model.User
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.LevelUp ->
                    snackbarHostState.showSnackbar("⬆ LEVEL UP! You are now Level ${effect.newLevel}")
                is DashboardEffect.NavigateTo -> onNavigate(effect.route)
                else -> { /* Handle ShowToast / ShowSnackbar based on your actual DashboardEffect setup */ }
            }
        }
    }

    DashboardScreenContent(
        user = state.user,
        activeQuests = state.activeQuests,
        todayHabits = state.todayHabits,
        isGeneratingQuest = state.isGeneratingQuest,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun DashboardScreenContent(
    user: User?,
    activeQuests: List<Quest>,
    todayHabits: List<Habit>,
    isGeneratingQuest: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (DashboardIntent) -> Unit
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ─────────────────────────────────────────────
            item {
                DashboardHeader(
                    username = user?.username ?: "HUNTER",
                    level = user?.level ?: 1,
                    hp = user?.hp ?: 100,
                    maxHp = user?.maxHp ?: 100
                )
            }

            // ── XP Bar ─────────────────────────────────────────────
            user?.let { currentUser ->
                item {
                    SystemPanel(glowColor = CyanAccent) {
                        val animatedFraction by animateFloatAsState(
                            targetValue = currentUser.xpFraction,
                            animationSpec = tween(1000, easing = EaseOutCubic),
                            label = "xp_fraction"
                        )
                        // Make sure AscendXpBar is accessible and implemented correctly
                        AscendXpBar(
                            user = currentUser,
                            animatedFraction = animatedFraction,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Generate Quests Button ──────────────────────────────
            item {
                GenerateQuestsButton(
                    isLoading = isGeneratingQuest,
                    onClick = { onIntent(DashboardIntent.RequestNewQuests) }
                )
            }

            // ── Active Quests ───────────────────────────────────────
            if (activeQuests.isNotEmpty()) {
                item {
                    SectionHeader("◈ ACTIVE QUESTS", PurpleLight)
                }
                items(activeQuests, key = { it.id }) { quest ->
                    QuestCard(
                        quest = quest,
                        onComplete = { onIntent(DashboardIntent.CompleteQuest(quest.id)) },
                        onSkip = { onIntent(DashboardIntent.SkipQuest(quest.id)) }
                    )
                }
            }

            // ── Daily Habits ────────────────────────────────────────
            if (todayHabits.isNotEmpty()) {
                item {
                    SectionHeader("◈ DAILY MISSIONS", CyanAccent)
                }
                items(todayHabits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        onComplete = { onIntent(DashboardIntent.CompleteHabit(habit.id)) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DashboardHeader(username: String, level: Int, hp: Int, maxHp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = username.uppercase(Locale.ROOT),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
            ) {
                // Assuming RankBadge is implemented in your components
                RankBadge(level = level)
                Text(
                    text = "LVL $level",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }

        // HP display
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "HP",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = DangerRed
            )
            Text(
                text = "$hp / $maxHp",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            val hpFraction = (hp.toFloat() / maxHp).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PanelMid)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(hpFraction)
                        .background(
                            Brush.horizontalGradient(listOf(DangerRed, Color(0xFFFF6B6B)))
                        )
                )
            }
        }
    }
}

@Composable
fun QuestCard(quest: Quest, onComplete: () -> Unit, onSkip: () -> Unit) {
    SystemPanel(glowColor = PurplePrimary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assuming QuestDifficultyBadge is implemented in your components
                    QuestDifficultyBadge(difficulty = quest.difficulty)
                    Text(
                        text = quest.title,
                        fontSize = 9.sp,
                        color = CyanAccent,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = quest.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = quest.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(GoldAccent.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "+${quest.xpReward} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = quest.skillArea.uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = BorderGlow, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlow)
            ) {
                Text("SKIP", fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .weight(2f)
                    .shadow(12.dp, RoundedCornerShape(8.dp),
                        ambientColor = PurplePrimary, spotColor = PurpleLight),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "COMPLETE QUEST",
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onComplete: () -> Unit) {
    val glowColor = if (habit.completedToday) SuccessGreen else CyanAccent
    SystemPanel(glowColor = glowColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY MISSION",
                    fontSize = 9.sp,
                    color = CyanAccent,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = habit.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (habit.completedToday) TextMuted else TextPrimary
                )
                if (habit.currentStreak > 0) {
                    Text(
                        text = "${habit.currentStreak}-DAY STREAK",
                        fontSize = 11.sp,
                        color = GoldAccent,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (habit.completedToday) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(12.dp, CircleShape, ambientColor = SuccessGreen)
                        .background(SuccessGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = SystemBlack, modifier = Modifier.size(20.dp))
                }
            } else {
                OutlinedButton(
                    onClick = onComplete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("DONE", fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun GenerateQuestsButton(isLoading: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "btn_glow"
    )

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(10.dp),
                ambientColor = PurplePrimary.copy(alpha = glowAlpha),
                spotColor = CyanAccent.copy(alpha = glowAlpha)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "⚔  GENERATE NEW QUESTS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = accent,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "1. Dashboard Screen (Populated)")
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
                )
            ),
            isGeneratingQuest = false,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
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