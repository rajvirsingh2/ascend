package com.ascend.app.ui.interests

import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.UserInterest

data class InterestsState(
    val step: InterestsStep = InterestsStep.CATEGORY_PICK,
    val categories: List<InterestCategory> = emptyList(),
    val selectedInterests: List<UserInterest> = emptyList(),
    val draftCategory: String? = null,
    val draftSubcategory: String? = null,
    val draftCustomGoal: String = "",
    val draftPriority: Int = 1,
    val draftProficiency: String = "Beginner",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val canProceedFromCategory: Boolean get() = draftCategory != null
    val canProceedFromSubcategory: Boolean get() = true
    val canSave: Boolean get() = selectedInterests.isNotEmpty() && !isSaving
    val draftCategoryObj get() = categories.find { it.id == draftCategory }
}

enum class InterestsStep {
    CATEGORY_PICK,
    SUBCATEGORY_PICK,
    PROFICIENCY_PICK,   // ask skill level before custom goal
    CUSTOM_GOAL,
    REVIEW
}

sealed class InterestsIntent{
    data class SelectCategory(val categoryId:String): InterestsIntent()
    data class SelectSubcategory(val subcategoryId:String?): InterestsIntent()
    data class SetPriority(val priority: Int) : InterestsIntent()
    data class SetProficiency(val proficiency: String) : InterestsIntent()
    object ConfirmProficiencyAndContinue : InterestsIntent()   // advance from PROFICIENCY_PICK → CUSTOM_GOAL
    data class SetCustomGoal(val text: String) : InterestsIntent()
    object ConfirmDraftAndAddMore: InterestsIntent()
    object ConfirmDraftAndReview: InterestsIntent()
    data class RemoveInterest(val index: Int): InterestsIntent()
    data class ChangePriority(val index: Int, val priority:Int): InterestsIntent()
    object GoBack: InterestsIntent()
    object Save: InterestsIntent()
    object DismissError: InterestsIntent()
}

sealed class InterestsEffect{
    object NavigateToDashboard: InterestsEffect()
    data class ShowToast(val message: String): InterestsEffect()
}
