package com.ascend.app.domain.model

data class StatDelta(
    val statName: String,
    val before: String,
    val after: String,
    val delta: String,
    val deltaPositive: Boolean = true
)

fun buildLevelUpDeltas(newLevel: Int): List<StatDelta> = listOf(
    StatDelta("Max HP",
        before = "${(newLevel - 1) * 10 + 100}",
        after  = "${newLevel * 10 + 100}",
        delta  = "+10"),
    StatDelta("XP Multiplier",
        before = "${"%.2f".format(1.0 + (newLevel - 2) * 0.05)}×",
        after  = "${"%.2f".format(1.0 + (newLevel - 1) * 0.05)}×",
        delta  = "+5%"),
    StatDelta("Quest Slots",
        before = "${2 + (newLevel - 1) / 3}",
        after  = "${2 + newLevel / 3}",
        delta  = if (newLevel % 3 == 0) "+1" else "—"),
    StatDelta("Skill Points",
        before = "—", after = "+2", delta = "+2 available",
        deltaPositive = true)
)