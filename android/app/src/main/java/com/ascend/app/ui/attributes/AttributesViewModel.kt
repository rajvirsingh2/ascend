package com.ascend.app.ui.attributes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.repository.UserRepository
import com.ascend.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AttributesUiState(
    val user: User? = null
)

@HiltViewModel
class AttributesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    val state: StateFlow<AttributesUiState> = userRepository.observeUser()
        .map { AttributesUiState(user = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AttributesUiState()
        )
}
