package com.ascend.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark theme ────────────────────────────────────────────────
object DarkColors {
    val Void        = Color(0xFF0D0D1A)   // app background
    val Abyss       = Color(0xFF13132B)   // card surface
    val Deep        = Color(0xFF1C1C3A)   // elevated surface
    val Dusk        = Color(0xFF252550)   // borders / dividers
    val Arcane      = Color(0xFF7B61FF)   // primary accent
    val Cyan        = Color(0xFF00D4FF)   // XP / energy
    val Ember       = Color(0xFFFF6B35)   // HP / danger
    val Gold        = Color(0xFFFFD700)   // legendary / XP gain
    val Neon        = Color(0xFF39FF14)   // success / complete
    val Crimson     = Color(0xFFFF2D78)   // rare quest
    val TextPrimary = Color(0xFFE8E8FF)
    val TextMuted   = Color(0xFF7B7BAA)
}

// ── Light theme ───────────────────────────────────────────────
object LightColors {
    val Frost       = Color(0xFFF0F0FF)
    val Surface     = Color(0xFFFFFFFF)
    val Mist        = Color(0xFFE8E8FF)
    val Royal       = Color(0xFF5B3FFF)
    val Azure       = Color(0xFF0099CC)
    val Flame       = Color(0xFFE85A2A)
    val Amber       = Color(0xFFCC9900)
    val TextPrimary = Color(0xFF1A1A60)
    val TextMuted   = Color(0xFF5050A0)
}

// ── Gradient definitions ──────────────────────────────────────
object Gradients {
    val ArcaneFlow     = listOf(Color(0xFF7B61FF), Color(0xFF00D4FF))
    val EmberPulse     = listOf(Color(0xFFFF6B35), Color(0xFFFF2D78))
    val EnergyStream   = listOf(Color(0xFF00D4FF), Color(0xFF39FF14))
    val LegendaryFlame = listOf(Color(0xFFFFD700), Color(0xFFFF6B35), Color(0xFFFF2D78))
    val VoidRift       = listOf(Color(0xFF7B61FF), Color(0xFFFF2D78))
    val ShadowCard     = listOf(Color(0xFF1C1C3A), Color(0xFF252550))
}

// ── Rarity system ─────────────────────────────────────────────
enum class QuestRarity(
    val label: String,
    val gradient: List<Color>,
    val borderColor: Color
) {
    D_RANK("D-Rank · Common",
        listOf(Color(0xFF555555), Color(0xFF888888)),
        Color(0xFF888888)),
    C_RANK("C-Rank · Uncommon",
        listOf(Color(0xFF1A6B1A), Color(0xFF39FF14)),
        Color(0xFF39FF14)),
    B_RANK("B-Rank · Rare",
        listOf(Color(0xFF00A3CC), Color(0xFF00D4FF)),
        Color(0xFF00D4FF)),
    A_RANK("A-Rank · Epic",
        listOf(Color(0xFF5B3FFF), Color(0xFFFF2D78)),
        Color(0xFFFF2D78)),
    S_RANK("S-Rank · Legendary",
        listOf(Color(0xFFFFD700), Color(0xFFFF6B35), Color(0xFFFF2D78)),
        Color(0xFFFFD700))
}

fun Int.toRarity(): QuestRarity = when (this) {
    1    -> QuestRarity.D_RANK
    2    -> QuestRarity.C_RANK
    3    -> QuestRarity.B_RANK
    4    -> QuestRarity.A_RANK
    else -> QuestRarity.S_RANK
}