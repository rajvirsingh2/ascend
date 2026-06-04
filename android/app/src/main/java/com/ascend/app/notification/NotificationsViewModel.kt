package com.ascend.app.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsState(
    val items: List<NotifItem> = emptyList(),
    val filter: NotifType? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repo.getNotifications().collect { items ->
                    _state.update { it.copy(items = items, isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setFilter(type: NotifType?) {
        _state.update { it.copy(filter = type) }
    }

    fun markRead(id: String) {
        // optimistically update UI
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.id == id) it.copy(isRead = true) else it
            })
        }
        viewModelScope.launch { repo.markRead(id) }
    }

    fun markAllRead() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { repo.markAllRead() }
    }

    fun clearAll() {
        _state.update { it.copy(items = emptyList()) }
        viewModelScope.launch { repo.clearAll() }
    }

    fun deleteItem(id: String) {
        _state.update { s -> s.copy(items = s.items.filterNot { it.id == id }) }
        viewModelScope.launch { repo.delete(id) }
    }

    /** Call when new notification arrives (from FCM or in-app trigger) */
    fun addItem(item: NotifItem) {
        _state.update { it.copy(items = listOf(item) + it.items) }
    }
}
