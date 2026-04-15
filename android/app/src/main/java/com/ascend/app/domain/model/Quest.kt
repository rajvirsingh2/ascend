package com.ascend.app.domain.model

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val type: QuestType,
    val difficulty: Int,
    val xpReward: Int,
    val status: QuestStatus,
    val skillArea: String,
    val isAiGenerated: Boolean
)

enum class QuestType { DAILY, WEEKLY }

enum class QuestStatus { ACTIVE, COMPLETED, SKIPPED, EXPIRED }