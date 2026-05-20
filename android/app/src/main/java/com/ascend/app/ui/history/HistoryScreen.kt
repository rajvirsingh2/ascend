package com.ascend.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

import com.ascend.app.data.remote.api.ProgressLogEntry
import com.ascend.app.data.remote.dto.QuestResponse
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.toRarity

// 1. Stateful Wrapper handling ViewModel
@OptIn(ExperimentalMaterial3Api::class)
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

// 2. Stateless UI Composable safe for Previews
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    selectedTab: Int,
    isLoading: Boolean,
    completedQuests: List<QuestResponse>,
    progressLog: List<ProgressLogEntry>,
    onTabSelected: (Int) -> Unit
) {
    Scaffold(
        containerColor = DarkColors.Void,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = DarkColors.Abyss,
                contentColor     = DarkColors.Arcane
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { onTabSelected(0) },
                    text = { Text("QUESTS", fontSize = 11.sp, letterSpacing = 0.08.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { onTabSelected(1) },
                    text = { Text("XP LOG", fontSize = 11.sp, letterSpacing = 0.08.sp) }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkColors.Arcane)
                }
                return@Scaffold
            }

            when (selectedTab) {
                0 -> QuestHistoryTab(completedQuests)
                1 -> XpLogTab(progressLog)
            }
        }
    }
}

@Composable
private fun QuestHistoryTab(quests: List<QuestResponse>) {
    if (quests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No completed quests yet", color = DarkColors.TextMuted, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quests) { quest ->
            val rarity     = quest.difficulty.toRarity()
            val isComplete = quest.status == "completed"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkColors.Abyss)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp).height(48.dp)
                        .background(
                            if (isComplete) rarity.borderColor
                            else DarkColors.TextHint
                        )
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(quest.title, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, color = DarkColors.TextPrimary)
                    Text("${rarity.rank}-Rank · ${quest.skillArea}",
                        fontSize = 10.sp, color = rarity.borderColor,
                        letterSpacing = 0.06.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isComplete) "DONE" else quest.status.uppercase(),
                        fontSize = 10.sp,
                        color = if (isComplete) Color(0xFF39FF14)
                        else DarkColors.TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    if (isComplete) {
                        Text("+${quest.xpReward} XP", fontSize = 11.sp,
                            color = DarkColors.Gold, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun XpLogTab(logs: List<ProgressLogEntry>) {
    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No XP activity yet", color = DarkColors.TextMuted, fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(logs) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkColors.Abyss)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.eventType.replace("_", " ")
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = DarkColors.TextPrimary
                    )
                    if (entry.levelBefore != entry.levelAfter) {
                        Text("Level ${entry.levelBefore} → ${entry.levelAfter}",
                            fontSize = 10.sp, color = DarkColors.Gold)
                    }
                }
                Text(
                    text = "+${entry.xpDelta} XP",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFF39FF14)
                )
            }
        }
    }
}
// --- UPDATED PREVIEW ---

@Preview(showBackground = true, name = "2. History: XP Log Tab (Populated)")
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
                    xpDelta = 50,
                    levelBefore = 12,
                    levelAfter = 12,
                    createdAt = "2026-05-19T14:30:00Z"
                ),
                ProgressLogEntry(
                    eventType = "LEVEL_UP",
                    xpDelta = 150,
                    levelBefore = 12,
                    levelAfter = 13,
                    createdAt = "2026-05-18T09:15:00Z"
                ),
                ProgressLogEntry(
                    eventType = "DAILY_HABIT",
                    xpDelta = 15,
                    levelBefore = 11,
                    levelAfter = 12,
                    createdAt = "2026-05-17T08:00:00Z"
                )
            ),
            onTabSelected = {}
        )
    }
}