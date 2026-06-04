package com.ascend.app.ui.interests

import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.UserInterest

enum class InterestsStep { CATEGORY_PICK, FOCUS_AREAS, PROFICIENCY_PICK, GLOBAL_GOAL, REVIEW }

data class InterestsState(
    val step: InterestsStep = InterestsStep.CATEGORY_PICK,
    val categories: List<InterestCategory> = emptyList(),
    val pickedCategoryIds: Set<String> = emptySet(),
    val selectedInterests: List<UserInterest> = emptyList(),
    val proficiencyByCategory: Map<String, String> = emptyMap(),
    val globalGoal: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed class InterestsIntent {
    data class TogglePickedCategory(val id: String) : InterestsIntent()
    data class ToggleArea(val catId: String, val subId: String) : InterestsIntent()
    data class SetAreaPriority(val catId: String, val subId: String, val priority: Int) : InterestsIntent()
    data class SetCategoryProficiency(val catId: String, val level: String) : InterestsIntent()
    data class SetGlobalGoal(val goal: String) : InterestsIntent()
    data class RemoveInterest(val index: Int) : InterestsIntent()
    object Continue : InterestsIntent()
    object GoBack : InterestsIntent()
    object Save : InterestsIntent()
    object DismissError : InterestsIntent()
}

sealed class InterestsEffect {
    object NavigateToDashboard : InterestsEffect()
    data class ShowToast(val message: String) : InterestsEffect()
}
