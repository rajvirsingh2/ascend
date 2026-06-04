package com.ascend.app.ui.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.app.domain.model.UserInterest
// import com.ascend.app.domain.repository.InterestsRepository  // your repo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterestsViewModel @Inject constructor(
    // private val repo: InterestsRepository  // inject your data layer
) : ViewModel() {

    private val _state = MutableStateFlow(InterestsState())
    val state: StateFlow<InterestsState> = _state.asStateFlow()

    private val _effects = Channel<InterestsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // val cats = repo.getCategories()
                val cats = emptyList<com.ascend.app.domain.model.InterestCategory>() // replace
                _state.update { it.copy(categories = cats, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onIntent(intent: InterestsIntent) {
        when (intent) {
            is InterestsIntent.TogglePickedCategory -> togglePicked(intent.id)
            is InterestsIntent.ToggleArea           -> toggleArea(intent.catId, intent.subId)
            is InterestsIntent.SetAreaPriority      -> setAreaPriority(intent.catId, intent.subId, intent.priority)
            is InterestsIntent.SetCategoryProficiency -> setProficiency(intent.catId, intent.level)
            is InterestsIntent.SetGlobalGoal        -> _state.update { it.copy(globalGoal = intent.goal) }
            is InterestsIntent.RemoveInterest       -> removeInterest(intent.index)
            InterestsIntent.Continue                -> goNext()
            InterestsIntent.GoBack                  -> goBack()
            InterestsIntent.Save                    -> save()
            InterestsIntent.DismissError            -> _state.update { it.copy(error = null) }
        }
    }

    private fun togglePicked(id: String) {
        _state.update {
            val newSet = if (id in it.pickedCategoryIds) it.pickedCategoryIds - id
            else it.pickedCategoryIds + id
            // If category un-picked, remove its areas
            val cleanedInterests = if (id !in newSet)
                it.selectedInterests.filter { ui -> ui.category != id }
            else it.selectedInterests
            it.copy(pickedCategoryIds = newSet, selectedInterests = cleanedInterests)
        }
    }

    private fun toggleArea(catId: String, subId: String) {
        _state.update { s ->
            val exists = s.selectedInterests.any { it.category == catId && it.subcategory == subId }
            val newList = if (exists) {
                s.selectedInterests.filterNot { it.category == catId && it.subcategory == subId }
            } else {
                s.selectedInterests + UserInterest(
                    category = catId, subcategory = subId,
                    priority = 2, customGoal = ""
                )
            }
            s.copy(selectedInterests = newList)
        }
    }

    private fun setAreaPriority(catId: String, subId: String, priority: Int) {
        _state.update { s ->
            s.copy(selectedInterests = s.selectedInterests.map {
                if (it.category == catId && it.subcategory == subId) it.copy(priority = priority)
                else it
            })
        }
    }

    private fun setProficiency(catId: String, level: String) {
        _state.update { it.copy(proficiencyByCategory = it.proficiencyByCategory + (catId to level)) }
    }

    private fun removeInterest(idx: Int) {
        _state.update { s ->
            s.copy(selectedInterests = s.selectedInterests.toMutableList().apply {
                if (idx in indices) removeAt(idx)
            })
        }
    }

    private fun goNext() {
        _state.update {
            val cur = it.step.ordinal
            val total = InterestsStep.entries.size
            if (cur < total - 1) it.copy(step = InterestsStep.entries[cur + 1])
            else it
        }
    }

    private fun goBack() {
        _state.update {
            val cur = it.step.ordinal
            if (cur > 0) it.copy(step = InterestsStep.entries[cur - 1])
            else it
        }
    }

    private fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                // repo.saveOnboarding(
                //     interests = _state.value.selectedInterests,
                //     proficiency = _state.value.proficiencyByCategory,
                //     goal = _state.value.globalGoal
                // )
                _state.update { it.copy(isSaving = false) }
                _effects.send(InterestsEffect.NavigateToDashboard)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}