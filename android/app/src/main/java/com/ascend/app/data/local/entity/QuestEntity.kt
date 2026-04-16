package com.ascend.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: Int,
    val xpReward: Int,
    val status: String,
    val skillArea: String,
    val isAiGenerated: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)