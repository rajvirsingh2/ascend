package com.ascend.app.ui.dashboard

import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.User

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val user: User? = null,
    val activeQuests: List<Quest> = emptyList(),
    val todayHabits: List<Habit> = emptyList(),
    val error: String? = null,
)

sealed interface DashboardIntent {
    data object LoadDashboard : DashboardIntent
    data object GenerateQuests : DashboardIntent
    data class CompleteQuest(val questId: String) : DashboardIntent
    data class SkipQuest(val questId: String) : DashboardIntent
    data class CompleteHabit(val habitId: String) : DashboardIntent
}

sealed interface DashboardEffect {
    data class LevelUp(val newLevel: Int, val statDeltas: List<com.ascend.app.domain.model.StatDelta>) : DashboardEffect
    data class ShowSnackbar(val message: String) : DashboardEffect
    data class NavigateTo(val route: String) : DashboardEffect
}