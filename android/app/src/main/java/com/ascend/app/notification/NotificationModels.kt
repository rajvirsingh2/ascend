package com.ascend.app.notification

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.ascend.app.ui.theme.*

/* ============================================================
 *  NOTIFICATION EVENT TYPES
 * ============================================================ */
enum class NotifType(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    val displayLabel: String,
    val accentColor: Color,
    val icon: ImageVector
) {
    QUEST_COMPLETE(
        channelId = "ascend_quests",
        channelName = "Quest Updates",
        channelDescription = "Notifications for completed quests and XP gained",
        displayLabel = "QUEST",
        accentColor = CyanAccent,
        icon = Icons.Filled.Check
    ),
    LEVEL_UP(
        channelId = "ascend_level",
        channelName = "Level Up",
        channelDescription = "Notifications when you reach a new level",
        displayLabel = "LEVEL UP",
        accentColor = GoldAccent,
        icon = Icons.Filled.ArrowUpward
    ),
    STREAK_REMINDER(
        channelId = "ascend_streaks",
        channelName = "Streak Reminders",
        channelDescription = "Daily reminders to maintain your streak",
        displayLabel = "STREAK",
        accentColor = GoldAccent,
        icon = Icons.Filled.LocalFireDepartment
    ),
    STREAK_BROKEN(
        channelId = "ascend_streaks",
        channelName = "Streak Reminders",
        channelDescription = "Daily reminders to maintain your streak",
        displayLabel = "STREAK LOST",
        accentColor = DangerRed,
        icon = Icons.Filled.LocalFireDepartment // using standard instead of Outlined because Outlined isn't always available without import
    ),
    GOAL_MILESTONE(
        channelId = "ascend_goals",
        channelName = "Goal Milestones",
        channelDescription = "Progress alerts on your long-term goals",
        displayLabel = "GOAL",
        accentColor = PurpleLight,
        icon = Icons.Filled.Flag
    ),
    DAILY_QUEST(
        channelId = "ascend_dailies",
        channelName = "Daily Quests",
        channelDescription = "New daily quests available",
        displayLabel = "DAILY",
        accentColor = PurplePrimary,
        icon = Icons.Filled.Bolt
    ),
    FRIEND_ACTIVITY(
        channelId = "ascend_social",
        channelName = "Friend Activity",
        channelDescription = "Friend rank ups, challenges, and adds",
        displayLabel = "SOCIAL",
        accentColor = CyanAccent,
        icon = Icons.Filled.People
    ),
    SYSTEM(
        channelId = "ascend_system",
        channelName = "System",
        channelDescription = "General app notifications",
        displayLabel = "SYSTEM",
        accentColor = TextSecondary,
        icon = Icons.Filled.Info
    )
}

/* ============================================================
 *  NOTIFICATION DATA
 * ============================================================ */
data class NotifItem(
    val id: String,
    val type: NotifType,
    val title: String,
    val body: String,
    val timestamp: Long,           // epoch millis
    val isRead: Boolean = false,
    val actionRoute: String? = null,  // optional deep link
    val xpDelta: Int? = null,         // for quest/level events
    val extraData: Map<String, String> = emptyMap()
)

/* ============================================================
 *  TOAST EVENT (in-app overlay)
 * ============================================================ */
data class ToastEvent(
    val id: Long = System.currentTimeMillis(),
    val type: NotifType,
    val title: String,
    val message: String,
    val xpDelta: Int? = null,
    val durationMs: Long = 3500L
)
