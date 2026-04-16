package com.ascend.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val frequency: String,
    val xpReward: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedToday: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)