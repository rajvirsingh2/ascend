package com.ascend.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.realtime.WebSocketManager
import com.ascend.app.data.realtime.WsEvent
import com.ascend.app.data.repository.HabitRepository
import com.ascend.app.data.repository.QuestRepository
import com.ascend.app.data.repository.UserRepository
import com.ascend.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val questRepo: QuestRepository,
    private val habitRepo: HabitRepository,
    private val userRepo: UserRepository,
    private val wsManager: WebSocketManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeLocalData()
        observeWebSocketEvents()
        onIntent(DashboardIntent.LoadDashboard)
    }

    // observe Room flows — UI updates automatically on any cache change
    private fun observeLocalData() {
        viewModelScope.launch {
            combine(
                userRepo.observeUser(),
                questRepo.observeActiveQuests(),
                habitRepo.observeHabits()
            ) { user, quests, habits ->
                Triple(user, quests, habits)
            }.collect { (user, quests, habits) ->
                _state.update {
                    it.copy(user = user, activeQuests = quests, todayHabits = habits)
                }
            }
        }
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadDashboard -> refresh()
            is DashboardIntent.CompleteQuest -> completeQuest(intent.questId)
            is DashboardIntent.SkipQuest -> skipQuest(intent.questId)
            is DashboardIntent.CompleteHabit -> completeHabit(intent.habitId)
            is DashboardIntent.RequestNewQuests -> generateQuests()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userRepo.refresh()
            questRepo.refresh()
            habitRepo.refresh()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun completeQuest(questId: String) {
        viewModelScope.launch {
            when (val result = questRepo.completeQuest(questId)) {
                is Result.Success -> {
                    val awarded = result.data.xpAwarded ?: 0
                    val leveledUp = result.data.leveledUp == true
                    val newLevel = result.data.levelAfter ?: 0
                    userRepo.refresh() // re-sync XP
                    _effects.send(DashboardEffect.ShowSnackbar("+$awarded XP"))
                    if (leveledUp) _effects.send(DashboardEffect.LevelUp(newLevel))
                }
                is Result.Error ->
                    _effects.send(DashboardEffect.ShowSnackbar("Failed to complete quest"))
                else -> Unit
            }
        }
    }

    private fun skipQuest(questId: String) {
        viewModelScope.launch { questRepo.skipQuest(questId) }
    }

    private fun completeHabit(habitId: String) {
        viewModelScope.launch {
            when (val result = habitRepo.completeHabit(habitId)) {
                is Result.Success -> {
                    val awarded = result.data.xpAwarded ?: 0
                    if (awarded > 0) {
                        userRepo.refresh()
                        _effects.send(DashboardEffect.ShowSnackbar("+$awarded XP"))
                    } else {
                        _effects.send(DashboardEffect.ShowSnackbar("Already completed today"))
                    }
                }
                is Result.Error ->
                    _effects.send(DashboardEffect.ShowSnackbar("Failed to check in"))
                else -> Unit
            }
        }
    }

    private fun generateQuests() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingQuest = true) }
            when (val result = questRepo.generateQuests()) {
                is Result.Success ->
                    _effects.send(DashboardEffect.ShowSnackbar("New quests ready!"))
                is Result.Error ->
                    _effects.send(DashboardEffect.ShowSnackbar("Quest generation failed"))
                else -> Unit
            }
            _state.update { it.copy(isGeneratingQuest = false) }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            wsManager.events.collect { event ->
                when (event) {
                    is WsEvent.LevelUp -> {
                        userRepo.refresh() // sync new level from API
                        _effects.send(DashboardEffect.LevelUp(event.newLevel))
                    }
                    is WsEvent.XpAwarded -> {
                        _effects.send(DashboardEffect.ShowSnackbar("+${event.amount} XP"))
                        userRepo.refresh()
                    }
                    is WsEvent.GuildQuestCompleted -> {
                        _effects.send(DashboardEffect.ShowSnackbar(
                            "${event.memberName} completed ${event.questTitle}!"
                        ))
                    }
                    is WsEvent.Disconnected -> Unit
                }
            }
        }
    }
}