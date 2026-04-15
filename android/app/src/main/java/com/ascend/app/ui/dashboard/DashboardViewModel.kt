package com.ascend.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.QuestStatus
import com.ascend.app.domain.model.QuestType
import com.ascend.app.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        onIntent(DashboardIntent.LoadDashboard)
    }

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadDashboard -> loadFakeData()
            is DashboardIntent.CompleteQuest -> completeQuest(intent.questId)
            is DashboardIntent.SkipQuest -> skipQuest(intent.questId)
            is DashboardIntent.CompleteHabit -> completeHabit(intent.habitId)
            is DashboardIntent.RequestNewQuests -> generateQuests()
        }
    }

    private fun loadFakeData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            delay(600) // simulate network
            _state.update {
                it.copy(
                    isLoading = false,
                    user = fakeUser(),
                    activeQuests = fakeQuests(),
                    todayHabits = fakeHabits()
                )
            }
        }
    }

    private fun completeQuest(questId: String) {
        viewModelScope.launch {
            val xpGained = 40
            _state.update { state ->
                val newXp = (state.user?.currentXp ?: 0) + xpGained
                state.copy(
                    activeQuests = state.activeQuests.filter { it.id != questId },
                    user = state.user?.copy(currentXp = newXp)
                )
            }
            _effects.send(DashboardEffect.ShowSnackbar("+$xpGained XP"))
        }
    }

    private fun skipQuest(questId: String) {
        _state.update { it.copy(activeQuests = it.activeQuests.filter { q -> q.id != questId }) }
    }

    private fun completeHabit(habitId: String) {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    todayHabits = state.todayHabits.map { h ->
                        if (h.id == habitId) h.copy(completedToday = true, currentStreak = h.currentStreak + 1)
                        else h
                    }
                )
            }
            _effects.send(DashboardEffect.ShowSnackbar("+10 XP"))
        }
    }

    private fun generateQuests() {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingQuest = true) }
            delay(1500) // simulate AI call
            _state.update { it.copy(isGeneratingQuest = false) }
            _effects.send(DashboardEffect.ShowSnackbar("New quests generated!"))
        }
    }

    // --- Fake data ---

    private fun fakeUser() = User(
        id = "1",
        email = "test@ascend.app",
        username = "Hero",
        level = 3,
        currentXp = 50,
        xpToNext = 316,
        avatarUrl = null
    )

    private fun fakeQuests() = listOf(
        Quest("q1", "Complete a 2km run",
            "Head outside and run 2km at any pace.",
            QuestType.DAILY, 2, 40, QuestStatus.ACTIVE, "fitness", false),
        Quest("q2", "Read for 30 minutes",
            "Find a quiet spot and read uninterrupted.",
            QuestType.DAILY, 1, 25, QuestStatus.ACTIVE, "learning", false),
        Quest("q3", "Meditate for 10 minutes",
            "Use any app or sit in silence.",
            QuestType.DAILY, 1, 20, QuestStatus.ACTIVE, "mindfulness", true)
    )

    private fun fakeHabits() = listOf(
        Habit("h1", "Morning run", "daily", 15, 5, 12, false),
        Habit("h2", "Read 20 pages", "daily", 10, 3, 8, false),
        Habit("h3", "Drink 2L water", "daily", 5, 14, 14, true)
    )
}