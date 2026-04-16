package com.ascend.app.ui.goals

import com.ascend.app.domain.model.Goal

data class GoalsUiState(
    val isLoading: Boolean = true,
    val goals: List<Goal> = emptyList(),
    val showCreateDialog: Boolean = false,
    val error: String? = null
)

sealed interface GoalsIntent {
    data object Load : GoalsIntent
    data object ShowCreateDialog : GoalsIntent
    data object DismissDialog : GoalsIntent
    data class CreateGoal(
        val title: String,
        val description: String,
        val skillArea: String,
        val priority: Int
    ) : GoalsIntent
    data class DeleteGoal(val id: String) : GoalsIntent
}

sealed interface GoalsEffect {
    data class ShowSnackbar(val message: String) : GoalsEffect
}