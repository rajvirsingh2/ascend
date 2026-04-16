package com.ascend.app.ui.profile

import com.ascend.app.domain.model.User

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null
)

sealed interface ProfileIntent {
    data object Load : ProfileIntent
    data object Logout : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToLogin : ProfileEffect
}