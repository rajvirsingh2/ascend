package com.ascend.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.remote.api.HistoryApiService
import com.ascend.app.data.remote.api.ProgressLogEntry
import com.ascend.app.data.remote.dto.QuestResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class HistoryUiState(
    val isLoading: Boolean = true,
    val completedQuests: List<QuestResponse> = emptyList(),
    val progressLog: List<ProgressLogEntry> = emptyList(),
    val selectedTab: Int = 0
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: HistoryApiService
) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun selectTab(index: Int) = _state.update { it.copy(selectedTab = index) }

    private fun load() {
        viewModelScope.launch {
            try {
                val quests = api.getQuestHistory().data ?: emptyList()
                val logs   = api.getProgressLog().data ?: emptyList()
                _state.update { it.copy(isLoading = false,
                    completedQuests = quests, progressLog = logs) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}