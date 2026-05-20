package com.ascend.app.ui.interests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.app.data.repository.InterestsRepository
import com.ascend.app.domain.model.Result
import com.ascend.app.domain.model.UserInterest
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
    private val repository: InterestsRepository
): ViewModel(){
    private val _state= MutableStateFlow(InterestsState())
    val state: StateFlow<InterestsState> = _state.asStateFlow()

    private val _effects= Channel<InterestsEffect>(Channel.BUFFERED)
    val effects=_effects.receiveAsFlow()

    init {
        loadCategories()
    }

    private fun loadCategories(){
        viewModelScope.launch {
            when(val res=repository.getCategories()){
                is Result.Success -> _state.update { it.copy(
                    categories = res.data, isLoading = false
                ) }
                is Result.Error -> _state.update { it.copy(
                    error = res.message, isLoading = false
                ) }
                is Result.Loading -> _state.update { it.copy(isLoading = true) }
            }
        }
    }

    fun onIntent(intent: InterestsIntent){
        when(intent){
            is InterestsIntent.SelectCategory ->{
                _state.update { it.copy(
                    draftCategory = intent.categoryId,
                    draftSubcategory = null,
                    draftCustomGoal = "",
                    draftPriority = 1,
                    draftProficiency = "Beginner",
                    step = InterestsStep.SUBCATEGORY_PICK
                ) }
            }
            is InterestsIntent.SelectSubcategory ->{
                _state.update { it.copy(
                    draftSubcategory = intent.subcategoryId,
                    step = InterestsStep.PROFICIENCY_PICK   // advance to proficiency selection
                ) }
            }
            is InterestsIntent.SetPriority ->{
                _state.update { it.copy(
                    draftPriority = intent.priority
                ) }
            }
            is InterestsIntent.SetProficiency ->{
                _state.update { it.copy(
                    draftProficiency = intent.proficiency
                ) }
            }
            is InterestsIntent.ConfirmProficiencyAndContinue -> {
                _state.update { it.copy(step = InterestsStep.CUSTOM_GOAL) }
            }
            is InterestsIntent.SetCustomGoal ->{
                _state.update { it.copy(
                    draftCustomGoal = intent.text
                ) }
            }
            is InterestsIntent.ConfirmDraftAndAddMore ->{
                commitDraft()
                _state.update { it.copy(
                    step = InterestsStep.CATEGORY_PICK,
                    draftCategory = null,
                    draftSubcategory = null,
                    draftPriority = 1,
                    draftProficiency = "Beginner",
                    draftCustomGoal = ""
                ) }
            }
            is InterestsIntent.ConfirmDraftAndReview -> {
                commitDraft()
                _state.update { it.copy(step = InterestsStep.REVIEW) }
            }

            is InterestsIntent.RemoveInterest -> {
                _state.update { s ->
                    val updated = s.selectedInterests.toMutableList()
                    if (intent.index in updated.indices) updated.removeAt(intent.index)
                    s.copy(selectedInterests = updated)
                }
            }
            is InterestsIntent.ChangePriority -> {
                _state.update { s ->
                    val updated = s.selectedInterests.toMutableList()
                    if (intent.index in updated.indices) {
                        updated[intent.index] = updated[intent.index].copy(priority = intent.priority)
                    }
                    s.copy(selectedInterests = updated.sortedBy { it.priority })
                }
            }

            is InterestsIntent.GoBack -> {
                val current = _state.value.step
                val previous = when (current) {
                    InterestsStep.SUBCATEGORY_PICK -> InterestsStep.CATEGORY_PICK
                    InterestsStep.PROFICIENCY_PICK -> InterestsStep.SUBCATEGORY_PICK
                    InterestsStep.CUSTOM_GOAL      -> InterestsStep.PROFICIENCY_PICK
                    InterestsStep.REVIEW           -> InterestsStep.CATEGORY_PICK
                    else                           -> null
                }
                if (previous != null) {
                    _state.update { it.copy(step = previous) }
                }
            }
            is InterestsIntent.Save -> {
                saveInterests()
            }

            is InterestsIntent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    fun proceedToCustomGoal(){
        _state.update { it.copy(step = InterestsStep.CUSTOM_GOAL) }
    }
    private fun saveInterests() {
        val interests = _state.value.selectedInterests
        if (interests.isEmpty()) return

        viewModelScope.launch {
            when (val result = repository.saveInterests(interests)) {
                is Result.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    _effects.send(InterestsEffect.NavigateToDashboard)
                }
                is Result.Error -> {
                    _state.update { it.copy(isSaving = false, error = result.message) }
                }
                is Result.Loading -> {
                    _state.update { it.copy(isSaving = true) }
                }
            }
        }
    }

    private fun commitDraft() {
        val s = _state.value
        val categoryId = s.draftCategory ?: return
        val interest = UserInterest(
            category = categoryId,
            subcategory = s.draftSubcategory ?: "",
            customGoal = s.draftCustomGoal.trim(),
            priority = s.draftPriority,
            proficiency = s.draftProficiency
        )
        val existing = s.selectedInterests.toMutableList()
        val idx = existing.indexOfFirst {
            it.category == interest.category && it.subcategory == interest.subcategory
        }
        if (idx >= 0) existing[idx] = interest else existing.add(interest)
        _state.update { it.copy(selectedInterests = existing.sortedBy { i -> i.priority }) }
    }
}