package com.ascend.app.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.auth.jetBrainsMono

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onItemClick: (NotifItem) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationsScreenContent(
        items = state.items,
        unreadCount = state.items.count { !it.isRead },
        selectedFilter = state.filter,
        onBack = onBack,
        onFilterChange = viewModel::setFilter,
        onMarkAllRead = viewModel::markAllRead,
        onClearAll = viewModel::clearAll,
        onItemClick = { item ->
            viewModel.markRead(item.id)
            onItemClick(item)
        },
        onSwipeDelete = viewModel::deleteItem
    )
}

@Composable
fun NotificationsScreenContent(
    items: List<NotifItem>,
    unreadCount: Int,
    selectedFilter: NotifType?,
    onBack: () -> Unit,
    onFilterChange: (NotifType?) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    onItemClick: (NotifItem) -> Unit,
    onSwipeDelete: (String) -> Unit
) {
    val filtered = remember(items, selectedFilter) {
        if (selectedFilter == null) items else items.filter { it.type == selectedFilter }
    }
    val grouped = remember(filtered) { groupByDay(filtered) }

    Scaffold(containerColor = SystemBlack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PurplePrimary.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                NotificationsTopBar(
                    unreadCount = unreadCount,
                    onBack = onBack,
                    onMarkAllRead = onMarkAllRead,
                    onClearAll = onClearAll
                )
                FilterChipRow(selected = selectedFilter, onChange = onFilterChange)
                Spacer(Modifier.height(6.dp))

                if (filtered.isEmpty()) {
                    EmptyNotifications(filter = selectedFilter)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        grouped.forEach { (dayLabel, dayItems) ->
                            item(key = "h_$dayLabel") { DayHeader(label = dayLabel) }
                            items(dayItems, key = { it.id }) { item ->
                                NotificationCard(item = item, onClick = { onItemClick(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "◈ NOTIFICATIONS",
                fontFamily = orbitron, fontSize = 18.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                color = PurpleLight,
                style = TextStyle(shadow = Shadow(PurpleLight.copy(alpha = 0.4f), blurRadius = 8f))
            )
            if (unreadCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape).background(DangerRed)
                        .shadow(8.dp, CircleShape, ambientColor = DangerRed, spotColor = DangerRed)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        unreadCount.toString(),
                        fontFamily = jetBrainsMono, fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black, color = Color.White
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onMarkAllRead, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.DoneAll, null, tint = CyanAccent, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onClearAll, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.DeleteSweep, null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FilterChipRow(selected: NotifType?, onChange: (NotifType?) -> Unit) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        FilterPill("ALL", TextPrimary, selected == null) { onChange(null) }
        NotifType.entries.forEach { type ->
            FilterPill(type.displayLabel, type.accentColor, selected == type) { onChange(type) }
        }
    }
}

@Composable
private fun FilterPill(label: String, color: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (active) color else BorderGlow, RoundedCornerShape(8.dp))
            .then(
                if (active) Modifier.shadow(8.dp, RoundedCornerShape(8.dp),
                    ambientColor = color, spotColor = color) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = jetBrainsMono, fontSize = 10.sp,
            letterSpacing = 1.2.sp, fontWeight = FontWeight.Black,
            color = if (active) color else TextMuted
        )
    }
}

@Composable
private fun DayHeader(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(6.dp).background(CyanAccent))
        Text(label.uppercase(Locale.ROOT),
            fontFamily = jetBrainsMono, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Box(modifier = Modifier.weight(1f).height(1.dp).background(BorderGlow.copy(alpha = 0.3f)))
    }
}

@Composable
fun NotificationCard(item: NotifItem, onClick: () -> Unit) {
    val accent = item.type.accentColor
    val bg = if (!item.isRead) accent.copy(alpha = 0.06f) else PanelMid.copy(alpha = 0.5f)
    val border = if (!item.isRead) accent.copy(alpha = 0.5f) else BorderGlow.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (!item.isRead) 1.5.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                    .then(if (!item.isRead) Modifier.shadow(8.dp, RoundedCornerShape(9.dp),
                        ambientColor = accent, spotColor = accent) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.type.icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.type.displayLabel,
                        fontFamily = jetBrainsMono, fontSize = 9.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.Black, color = accent)
                    if (!item.isRead) {
                        Box(modifier = Modifier
                            .size(6.dp).clip(CircleShape).background(accent)
                            .shadow(6.dp, CircleShape, ambientColor = accent, spotColor = accent))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(formatRelative(item.timestamp),
                        fontFamily = jetBrainsMono, fontSize = 9.sp,
                        color = TextMuted.copy(alpha = 0.6f))
                }
                Spacer(Modifier.height(4.dp))
                Text(item.title,
                    fontFamily = orbitron, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp,
                    color = if (item.isRead) TextSecondary else TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(item.body,
                    fontFamily = jetBrainsMono, fontSize = 11.5.sp,
                    color = TextSecondary.copy(alpha = if (item.isRead) 0.6f else 0.85f),
                    lineHeight = 16.sp)
                item.xpDelta?.let { xp ->
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GoldAccent.copy(alpha = 0.12f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("+${xp} XP",
                            fontFamily = jetBrainsMono, fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                            color = GoldAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotifications(filter: NotifType?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("📡", fontSize = 44.sp, modifier = Modifier.alpha(0.4f))
            Text(
                if (filter == null) "NO TRANSMISSIONS" else "NO ${filter.displayLabel} TRANSMISSIONS",
                fontFamily = orbitron, fontSize = 14.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = TextMuted
            )
            Text("The System is quiet for now.",
                fontFamily = jetBrainsMono, fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.6f))
        }
    }
}

private fun groupByDay(items: List<NotifItem>): List<Pair<String, List<NotifItem>>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val week = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
    val df = SimpleDateFormat("MMM d", Locale.getDefault())
    val groups = linkedMapOf<String, MutableList<NotifItem>>()
    items.sortedByDescending { it.timestamp }.forEach { item ->
        val label = when {
            item.timestamp >= today.timeInMillis     -> "TODAY"
            item.timestamp >= yesterday.timeInMillis -> "YESTERDAY"
            item.timestamp >= week.timeInMillis      -> "THIS WEEK"
            else -> df.format(Date(item.timestamp))
        }
        groups.getOrPut(label) { mutableListOf() }.add(item)
    }
    return groups.toList()
}

private fun formatRelative(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000          -> "now"
        diff < 3_600_000       -> "${diff / 60_000}m"
        diff < 86_400_000      -> "${diff / 3_600_000}h"
        diff < 7 * 86_400_000  -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Populated")
@Composable
fun NotificationsPreview_Populated() {
    val now = System.currentTimeMillis()
    val mock = listOf(
        NotifItem("n1", NotifType.LEVEL_UP, "Level 24 reached!", "You've ascended to the next tier. New quests unlocked.",
            now - 5 * 60_000, false, null, xpDelta = 500),
        NotifItem("n2", NotifType.QUEST_COMPLETE, "Quest Cleared", "Refactor auth service — well done.",
            now - 2 * 3600_000, false, null, xpDelta = 150),
        NotifItem("n3", NotifType.STREAK_REMINDER, "12-Day Streak Active", "Complete 1 daily mission to keep it going.",
            now - 8 * 3600_000, true),
        NotifItem("n4", NotifType.GOAL_MILESTONE, "Goal at 50%", "Climb Mt. Fuji — halfway there!",
            now - 26 * 3600_000, true),
        NotifItem("n5", NotifType.FRIEND_ACTIVITY, "Sung Jinwoo passed you", "They reached Level 25 in Tech.",
            now - 3 * 86_400_000, true)
    )
    MaterialTheme {
        NotificationsScreenContent(
            items = mock, unreadCount = 2, selectedFilter = null,
            onBack = {}, onFilterChange = {},
            onMarkAllRead = {}, onClearAll = {},
            onItemClick = {}, onSwipeDelete = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Empty")
@Composable
fun NotificationsPreview_Empty() {
    MaterialTheme {
        NotificationsScreenContent(
            items = emptyList(), unreadCount = 0, selectedFilter = null,
            onBack = {}, onFilterChange = {},
            onMarkAllRead = {}, onClearAll = {},
            onItemClick = {}, onSwipeDelete = {}
        )
    }
}
