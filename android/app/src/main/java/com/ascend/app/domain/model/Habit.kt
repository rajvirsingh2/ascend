package com.ascend.app.domain.model

data class Habit(
    val id: String,
    val title: String,
    val frequency: String,
    val xpReward: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedToday: Boolean
)