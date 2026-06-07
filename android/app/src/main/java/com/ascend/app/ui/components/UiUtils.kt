package com.ascend.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ascend.app.ui.theme.*
import java.util.Locale

fun formatNum(n: Int): String =
    if (n >= 1000) "%,d".format(n) else n.toString()

fun formatBigNum(n: Int): String = formatNum(n)

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

fun rankForPriority(priority: Int): String = when (priority) {
    1 -> "S"
    2 -> "A"
    3 -> "B"
    4 -> "C"
    else -> "D"
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
    "fitness", "physical", "strength", "endurance", "mobility", "running", "calisthenics" -> Color(0xFFFFB4AB)
    "learning", "mental", "focus", "meditation", "reading" -> PurpleLight
    "mindfulness"                                   -> Color(0xFFD2BBFF)
    "productivity", "coding", "tech", "technology", "ai", "ml", "devops"  -> CyanAccent
    "social", "networking", "speaking", "leadership" -> GoldAccent
    "creativity"                                    -> Color(0xFFFF6B9D)
    "finance", "budgeting", "investing", "saving"   -> Color(0xFF732EE4)
    else                                            -> CyanAccent
}

fun categoryEmoji(name: String): String = when (name.lowercase(Locale.ROOT)) {
    "tech"     -> "💻"
    "physical" -> "⚔"
    "mental"   -> "🧠"
    "social"   -> "🗣"
    "finance"  -> "📈"
    else       -> "◈"
}

fun categoryIcon(id: String): ImageVector = when (id) {
    "technology", "tech" -> Icons.Filled.Memory
    "physical"           -> Icons.Filled.FitnessCenter
    "mental"             -> Icons.Filled.Psychology
    "social"             -> Icons.Filled.Groups
    "finance"            -> Icons.Filled.AccountBalance
    else                 -> Icons.Filled.Star
}

fun categoryColor(id: String): Color = when (id) {
    "technology", "tech" -> ReactCyan
    "physical"           -> Color(0xFFFFB4AB)
    "mental"             -> ReactPurple
    "social"             -> ReactGold
    "finance"            -> ReactGreen
    else                 -> ReactPurple
}

fun blendColors(c1: Color, c2: Color, weight: Float): Color {
    val w = weight.coerceIn(0f, 1f)
    return Color(
        red   = c1.red * w + c2.red * (1 - w),
        green = c1.green * w + c2.green * (1 - w),
        blue  = c1.blue * w + c2.blue * (1 - w),
        alpha = c1.alpha * w + c2.alpha * (1 - w)
    )
}
