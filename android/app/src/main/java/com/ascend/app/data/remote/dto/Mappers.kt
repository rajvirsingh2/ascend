package com.ascend.app.data.remote.dto

import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.QuestStatus
import com.ascend.app.domain.model.QuestType
import com.ascend.app.domain.model.User

fun UserResponse.toDomain() = User(
    id = id,
    email = email,
    username = username,
    level = level,
    currentXp = currentXp,
    xpToNext = xpToNext,
    avatarUrl = avatarUrl
)

fun QuestResponse.toDomain() = Quest(
    id = id,
    title = title,
    description = description,
    type = if (type == "weekly") QuestType.WEEKLY else QuestType.DAILY,
    difficulty = difficulty,
    xpReward = xpReward,
    status = when (status) {
        "completed" -> QuestStatus.COMPLETED
        "skipped"   -> QuestStatus.SKIPPED
        "expired"   -> QuestStatus.EXPIRED
        else        -> QuestStatus.ACTIVE
    },
    skillArea = skillArea,
    isAiGenerated = isAiGenerated
)

fun HabitResponse.toDomain() = Habit(
    id = id,
    title = title,
    frequency = frequency,
    xpReward = xpReward,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    completedToday = false  // resolved by last_completed_at in M6
)