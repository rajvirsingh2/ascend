package com.ascend.app.ui.dashboard

import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.User

data class DashboardUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val activeQuests: List<Quest> = emptyList(),
    val todayHabits: List<Habit> = emptyList(),
    val isGeneratingQuest: Boolean = false,
    val error: String? = null
)

sealed interface DashboardIntent {
    data object LoadDashboard : DashboardIntent
    data object RequestNewQuests : DashboardIntent
    data class CompleteQuest(val questId: String) : DashboardIntent
    data class SkipQuest(val questId: String) : DashboardIntent
    data class CompleteHabit(val habitId: String) : DashboardIntent
}

sealed interface DashboardEffect {
    data class LevelUp(val newLevel: Int) : DashboardEffect
    data class ShowSnackbar(val message: String) : DashboardEffect
    data class NavigateTo(val route: String) : DashboardEffect
}