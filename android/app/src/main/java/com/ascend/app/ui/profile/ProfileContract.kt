package com.ascend.app.ui.profile

import com.ascend.app.domain.model.Achievement
import com.ascend.app.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val achievements: List<Achievement> = emptyList(),
    val completedQuestCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
)

sealed interface ProfileIntent {
    object Load : ProfileIntent
    object Logout : ProfileIntent
    data class UploadAvatar(val base64Image: String) : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
    data class ShowError(val message: String) : ProfileEffect
}