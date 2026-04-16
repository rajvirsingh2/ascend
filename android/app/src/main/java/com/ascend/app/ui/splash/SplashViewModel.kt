package com.ascend.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Login : SplashDestination
    data object Dashboard : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    userRepo: UserRepository
) : ViewModel() {

    val destination = userRepo.hasToken()
        .map { hasToken ->
            if (hasToken) SplashDestination.Dashboard
            else SplashDestination.Login
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SplashDestination.Loading
        )
}