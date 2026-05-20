package com.ascend.app.ui.profile

import com.ascend.app.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val achievements: List<AchievementItem> = emptyList(),
    val completedQuestCount: Int = 0
)

sealed interface ProfileIntent {
    object Load : ProfileIntent
    object Logout : ProfileIntent
    data class UploadAvatar(val base64Image: String) : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
}