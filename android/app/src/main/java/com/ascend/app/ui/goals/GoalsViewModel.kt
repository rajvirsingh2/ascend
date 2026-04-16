package com.ascend.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.remote.api.GoalApiService
import com.ascend.app.data.remote.dto.CreateGoalRequest
import com.ascend.app.data.remote.dto.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val api: GoalApiService
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<GoalsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { onIntent(GoalsIntent.Load) }

    fun onIntent(intent: GoalsIntent) {
        when (intent) {
            is GoalsIntent.Load -> load()
            is GoalsIntent.ShowCreateDialog ->
                _state.update { it.copy(showCreateDialog = true) }
            is GoalsIntent.DismissDialog ->
                _state.update { it.copy(showCreateDialog = false) }
            is GoalsIntent.CreateGoal -> createGoal(intent)
            is GoalsIntent.DeleteGoal -> deleteGoal(intent.id)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = api.getGoals()
                _state.update {
                    it.copy(
                        isLoading = false,
                        goals = response.data?.map { g -> g.toDomain() } ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to load goals") }
            }
        }
    }

    private fun createGoal(intent: GoalsIntent.CreateGoal) {
        viewModelScope.launch {
            try {
                val response = api.createGoal(
                    CreateGoalRequest(
                        title = intent.title,
                        description = intent.description,
                        skillArea = intent.skillArea,
                        priority = intent.priority
                    )
                )
                response.data?.let { goal ->
                    _state.update {
                        it.copy(
                            goals = it.goals + goal.toDomain(),
                            showCreateDialog = false
                        )
                    }
                    _effects.send(GoalsEffect.ShowSnackbar("Goal created"))
                }
            } catch (e: Exception) {
                _effects.send(GoalsEffect.ShowSnackbar("Failed to create goal"))
            }
        }
    }

    private fun deleteGoal(id: String) {
        viewModelScope.launch {
            try {
                api.deleteGoal(id)
                _state.update { it.copy(goals = it.goals.filter { g -> g.id != id }) }
                _effects.send(GoalsEffect.ShowSnackbar("Goal removed"))
            } catch (e: Exception) {
                _effects.send(GoalsEffect.ShowSnackbar("Failed to remove goal"))
            }
        }
    }
}