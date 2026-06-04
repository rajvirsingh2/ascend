package com.ascend.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.domain.model.Achievement
import com.ascend.app.data.remote.api.UserApiService
import com.ascend.app.data.remote.dto.AvatarUploadRequest
import com.ascend.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val userApi: UserApiService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar = _isUploadingAvatar.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                // Fetch fresh data from network
                userRepo.refresh()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Launch database observation in a separate job so it doesn't block remaining work
            viewModelScope.launch {
                userRepo.observeUser().collect { user ->
                    _state.update { it.copy(isLoading = false, user = user) }
                }
            }

            try {
                val achResp = userApi.getAchievements().data ?: emptyList()
                val statsResp = userApi.getStats().data

                _state.update { currentState ->
                    currentState.copy(
                        achievements = achResp.map {
                            Achievement(it.key, it.title, it.tag, it.icon, it.earned, it.earnedAt)
                        },
                        bestStreak = statsResp?.best_streak ?: 0
                    )
                }

                // OPTIONAL: Fetch completed quest statistics if managed by separate endpoint
                // val questStats = userApi.getQuestStats().data
                // _state.update { it.copy(completedQuestCount = questStats.completedTotal) }

            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // Unifies UI events through the standardized pathway
    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Load -> loadProfile()
            is ProfileIntent.Logout -> logout()
            is ProfileIntent.UploadAvatar -> uploadAvatar(intent.base64Image)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            userRepo.logout()
            _effects.send(ProfileEffect.NavigateToLogin)
        }
    }

    private fun uploadAvatar(base64Image: String) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            try {
                val response = userApi.uploadAvatar(AvatarUploadRequest(base64Image))
                if (response.data != null) {
                    userRepo.refresh()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }
}

