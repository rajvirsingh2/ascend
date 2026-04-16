package com.ascend.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val userRepo: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepo.observeUser().collect { user ->
                _state.update { it.copy(isLoading = false, user = user) }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.Load -> Unit
            is ProfileIntent.Logout -> logout()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            userRepo.logout()
            _effects.send(ProfileEffect.NavigateToLogin)
        }
    }
}