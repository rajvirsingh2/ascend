package com.ascend.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.data.remote.api.ProgressLogEntry
import com.ascend.app.data.remote.dto.QuestResponse
import com.ascend.app.ui.auth.jetBrainsMono // Assumed standard project font imports
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.theme.*
import java.util.Locale

// 1. Stateful Wrapper handling ViewModel (Kept from Snippet 1)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryScreenContent(
        selectedTab = state.selectedTab,
        isLoading = state.isLoading,
        completedQuests = state.completedQuests,
        progressLog = state.progressLog,
        onTabSelected = { viewModel.selectTab(it) }
    )
}

// 2. Stateless UI Composable (Skeleton from 1, UI styling from 2)
@Composable
fun HistoryScreenContent(
    selectedTab: Int,
    isLoading: Boolean,
    completedQuests: List<QuestResponse>,
    progressLog: List<ProgressLogEntry>,
    onTabSelected: (Int) -> Unit
) {
    Scaffold(containerColor = SystemBlack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
                .padding(padding)
        ) {
            // Background blur halo from Snippet 2
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PurplePrimary.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Header (Replaced standard app bar with custom header styled like 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "HISTORY",
                        fontFamily = orbitron,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    val count = if (selectedTab == 0) completedQuests.size else progressLog.size
                    Text(
                        "$count ENTRIES",
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Custom Tabs from Snippet 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FilterTab(
                        label = "QUESTS",
                        active = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterTab(
                        label = "XP LOG",
                        active = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))

                // Dot color legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Text(
                            if (selectedTab == 0) "COMPLETED" else "XP GAINED",
                            fontFamily = jetBrainsMono,
                            fontSize = 9.sp,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                        )
                        Text(
                            if (selectedTab == 0) "SKIPPED" else "XP LOST",
                            fontFamily = jetBrainsMono,
                            fontSize = 9.sp,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Content
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanAccent)
                    }
                } else {
                    when (selectedTab) {
                        0 -> QuestHistoryTab(completedQuests)
                        1 -> XpLogTab(progressLog)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (active) Modifier
                    .background(Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)))
                    .shadow(
                        10.dp, RoundedCornerShape(8.dp),
                        ambientColor = PurplePrimary, spotColor = CyanAccent
                    )
                else Modifier
                    .background(Color.Transparent)
                    .border(1.dp, BorderGlow, RoundedCornerShape(8.dp))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = orbitron,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Black,
            color = if (active) Color.White else TextMuted
        )
    }
}

@Composable
private fun QuestHistoryTab(quests: List<QuestResponse>) {
    if (quests.isEmpty()) {
        EmptyStateMessage("No completed quests yet")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(quests) { quest ->
            val isComplete = quest.status == "completed"
            val rarity = quest.difficulty.toRarity()

            CyberLogRow(
                title = quest.title,
                subtitle = "${rarity.rank}-Rank · ${quest.skillArea.uppercase(Locale.ROOT)}",
                xpLabel = "+${quest.xpReward}",
                typeLabel = "QUEST",
                typeColor = CyanAccent,
                dotColor = if (isComplete) SuccessGreen else DangerRed,
                isPositive = isComplete
            )
        }
    }
}

@Composable
private fun XpLogTab(logs: List<ProgressLogEntry>) {
    if (logs.isEmpty()) {
        EmptyStateMessage("No XP activity yet")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(logs) { entry ->
            val isPositive = entry.xpDelta > 0
            val typeLabel = entry.eventType.replace("_", " ").uppercase(Locale.ROOT)

            // Format title intelligently depending on event type
            val title = if (entry.levelBefore != entry.levelAfter) {
                "Level Up: ${entry.levelBefore} → ${entry.levelAfter}"
            } else {
                typeLabel
            }

            CyberLogRow(
                title = title,
                subtitle = entry.createdAt.take(10), // Example truncation for ISO dates
                xpLabel = "${if (isPositive) "+" else ""}${entry.xpDelta}",
                typeLabel = "XP",
                typeColor = PurpleLight,
                dotColor = if (isPositive) SuccessGreen else DangerRed,
                isPositive = isPositive
            )
        }
    }
}

// Reusable Cyberpunk Row Component abstracted from Snippet 2
@Composable
private fun CyberLogRow(
    title: String,
    subtitle: String,
    xpLabel: String,
    typeLabel: String,
    typeColor: Color,
    dotColor: Color,
    isPositive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelMid.copy(alpha = 0.85f))
            .border(1.dp, BorderGlow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .shadow(8.dp, CircleShape, ambientColor = dotColor, spotColor = dotColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Type chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(typeColor.copy(alpha = 0.12f))
                            .border(1.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            typeLabel,
                            fontFamily = jetBrainsMono,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Black,
                            color = typeColor
                        )
                    }
                    Text(
                        title,
                        fontFamily = orbitron,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = if (isPositive) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }

            // XP chip
            val xpColor = if (isPositive) GoldAccent else TextMuted
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(xpColor.copy(alpha = 0.12f))
                    .border(1.dp, xpColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    xpLabel,
                    fontFamily = jetBrainsMono,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = xpColor
                )
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = TextMuted,
            fontFamily = jetBrainsMono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// --- PREVIEWS ---

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "History: Quests Tab")
@Composable
fun HistoryScreenPreview_Quests() {
    MaterialTheme {
        HistoryScreenContent(
            selectedTab = 0,
            isLoading = false,
            completedQuests = listOf(
                QuestResponse(
                    id = "1",
                    title = "Refactor Auth Service",
                    description = "Migrate to Ktor client",
                    type = "TASK",
                    difficulty = 3,
                    xpReward = 150,
                    status = "completed",
                    skillArea = "tech",
                    isAiGenerated = false
                ),
                QuestResponse(
                    id = "2",
                    title = "Morning Workout",
                    description = "30 mins cardio",
                    type = "HABIT",
                    difficulty = 2,
                    xpReward = 80,
                    status = "failed",
                    skillArea = "physical",
                    isAiGenerated = false
                )
            ),
            progressLog = emptyList(),
            onTabSelected = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "History: XP Log Tab")
@Composable
fun HistoryScreenPreview_XpLog() {
    MaterialTheme {
        HistoryScreenContent(
            selectedTab = 1,
            isLoading = false,
            completedQuests = emptyList(),
            progressLog = listOf(
                ProgressLogEntry(
                    eventType = "QUEST_COMPLETED",
                    xpDelta = 150,
                    levelBefore = 12,
                    levelAfter = 12,
                    createdAt = "2026-05-19T14:30:00Z"
                ),
                ProgressLogEntry(
                    eventType = "LEVEL_UP",
                    xpDelta = 500,
                    levelBefore = 12,
                    levelAfter = 13,
                    createdAt = "2026-05-18T09:15:00Z"
                )
            ),
            onTabSelected = {}
        )
    }
}
