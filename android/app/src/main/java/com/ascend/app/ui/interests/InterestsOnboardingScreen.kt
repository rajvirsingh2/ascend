package com.ascend.app.ui.interests

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.InterestSubcategory
import com.ascend.app.domain.model.UserInterest
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.GoldAccent
import com.ascend.app.ui.theme.PanelDark
import com.ascend.app.ui.theme.PanelMid
import com.ascend.app.ui.theme.PurpleLight
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextMuted
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun InterestsOnboardingScreen(
    viewModel: InterestsViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is InterestsEffect.NavigateToDashboard -> onComplete()
                is InterestsEffect.ShowToast -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    InterestsOnboardingScreenContent(
        step = state.step,
        selectedInterests = state.selectedInterests,
        categories = state.categories,
        isLoading = state.isLoading,
        isSaving = state.isSaving,
        error = state.error,
        draftCategoryObj = state.draftCategoryObj,
        draftSubcategory = state.draftSubcategory,
        draftPriority = state.draftPriority,
        draftProficiency = state.draftProficiency,
        draftCustomGoal = state.draftCustomGoal,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        onProceedToCustomGoal = viewModel::proceedToCustomGoal
    )
}

@Composable
fun InterestsOnboardingScreenContent(
    step: InterestsStep,
    selectedInterests: List<UserInterest>,
    categories: List<InterestCategory>,
    isLoading: Boolean,
    isSaving: Boolean,
    error: String?,
    draftCategoryObj: InterestCategory?,
    draftSubcategory: String?,
    draftPriority: Int,
    draftProficiency: String,
    draftCustomGoal: String,
    snackbarHostState: SnackbarHostState,
    onIntent: (InterestsIntent) -> Unit,
    onProceedToCustomGoal: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SystemBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
                .padding(padding)
        ) {
            OnboardingTopBar(
                step = step,
                selectedCount = selectedInterests.size,
                onBack = { onIntent(InterestsIntent.GoBack) }
            )

            StepProgressBar(current = step)
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurplePrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "LOADING SYSTEM...", fontSize = 11.sp,
                            letterSpacing = 3.sp, color = TextMuted, fontWeight = FontWeight.Black
                        )
                    }
                }
                return@Scaffold
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally { if (isForward) it else -it } + fadeIn()) togetherWith (slideOutHorizontally { if (isForward) -it else it } + fadeOut())
                },
                label = "step_transition"
            ) { currentStep ->
                when (currentStep) {
                    InterestsStep.CATEGORY_PICK -> CategoryPickStep(
                        categories = categories,
                        selectedInterests = selectedInterests,
                        onSelectCategory = { onIntent(InterestsIntent.SelectCategory(it)) },
                        onGoToReview = {
                            if (selectedInterests.isNotEmpty()) {
                                onIntent(InterestsIntent.ConfirmDraftAndReview)
                            }
                        }
                    )

                    InterestsStep.SUBCATEGORY_PICK -> SubcategoryPickStep(
                        category = draftCategoryObj,
                        selectedSubcategory = draftSubcategory,
                        selectedPriority = draftPriority,
                        onSelectSubcategory = { onIntent(InterestsIntent.SelectSubcategory(it)) },
                        onSetPriority = { onIntent(InterestsIntent.SetPriority(it)) },
                        onNext = { onIntent(InterestsIntent.SelectSubcategory(draftSubcategory)) }
                    )

                    InterestsStep.PROFICIENCY_PICK -> ProficiencyPickStep(
                        categoryName = draftCategoryObj?.name ?: "",
                        subcategoryName = draftCategoryObj?.subcategories
                            ?.find { it.id == draftSubcategory }?.name ?: "",
                        selectedProficiency = draftProficiency,
                        onSetProficiency = { onIntent(InterestsIntent.SetProficiency(it)) },
                        onNext = { onIntent(InterestsIntent.ConfirmProficiencyAndContinue) }
                    )

                    InterestsStep.CUSTOM_GOAL -> CustomGoalStep(
                        categoryName = draftCategoryObj?.name ?: "",
                        subcategoryName = draftCategoryObj?.subcategories
                            ?.find { it.id == draftSubcategory }?.name ?: "",
                        customGoal = draftCustomGoal,
                        onGoalChanged = { onIntent(InterestsIntent.SetCustomGoal(it)) },
                        onAddMore = { onIntent(InterestsIntent.ConfirmDraftAndAddMore) },
                        onDone = { onIntent(InterestsIntent.ConfirmDraftAndReview) }
                    )

                    InterestsStep.REVIEW -> ReviewStep(
                        interests = selectedInterests,
                        categories = categories,
                        isSaving = isSaving,
                        onRemove = { onIntent(InterestsIntent.RemoveInterest(it)) },
                        onChangePriority = { idx, p -> onIntent(InterestsIntent.ChangePriority(idx, p)) },
                        onAddMore = { onIntent(InterestsIntent.GoBack) },
                        onSave = { onIntent(InterestsIntent.Save) }
                    )
                }
            }
        }
    }
    if (error != null) {
        AlertDialog(
            onDismissRequest = { onIntent(InterestsIntent.DismissError) },
            confirmButton = {
                TextButton(onClick = { onIntent(InterestsIntent.DismissError) }) {
                    Text("OK", color = PurpleLight)
                }
            },
            title = { Text("Error", color = TextPrimary) },
            text = { Text(error, color = TextSecondary) },
            containerColor = PanelDark
        )
    }
}

@Composable
private fun ReviewStep(
    interests: List<UserInterest>,
    categories: List<InterestCategory>,
    isSaving: Boolean,
    onRemove: (Int) -> Unit,
    onChangePriority: (Int, Int) -> Unit,
    onAddMore: () -> Unit,
    onSave: () -> Unit
) {
    val catMap = categories.associateBy { it.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("Your quest profile", fontSize = 18.sp,
                fontWeight = FontWeight.Black, color = TextPrimary)
            Text("The System will generate quests based on these areas.",
                fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
        }

        itemsIndexed(interests, key = { _, item -> "${item.category}_${item.subcategory}" }) { idx, interest ->
            val cat = catMap[interest.category]
            val sub = cat?.subcategories?.find { it.id == interest.subcategory }
            val catColor = try {
                Color((cat?.color ?: "#7C3AED").toColorInt())
            } catch (_: Exception) { PurplePrimary }

            ReviewInterestCard(
                interest = interest,
                categoryName = cat?.name ?: interest.category,
                subcategoryName = sub?.name ?: "",
                catColor = catColor,
                emoji = categoryEmoji(interest.category),
                onRemove = { onRemove(idx) },
                onChangePriority = { onChangePriority(idx, it) }
            )
        }

        item {
            // Add more button
            OutlinedButton(
                onClick = onAddMore,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, BorderGlow)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ADD ANOTHER AREA", fontSize = 11.sp,
                    letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            // Summary panel
            SystemPanel(glowColor = PurplePrimary) {
                Text("◈ QUEST SYSTEM PREVIEW", fontSize = 10.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 3.sp, color = PurpleLight)
                Spacer(Modifier.height(10.dp))
                val primaries = interests.count { it.priority == 1 }
                val secondaries = interests.count { it.priority == 2 }
                Text(
                    "Your daily quests will be generated from $primaries primary area${if (primaries != 1) "s" else ""}" +
                            (if (secondaries > 0) " and $secondaries secondary area${if (secondaries != 1) "s" else ""}" else "") +
                            ". The AI will personalise them to your stated goals.",
                    fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp
                )
            }
        }

        item {
            // Save button
            Button(
                onClick = onSave,
                enabled = interests.isNotEmpty() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(22.dp),
                            color = TextPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("⚔  ENTER THE SYSTEM", fontSize = 14.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewInterestCard(
    interest: UserInterest,
    categoryName: String,
    subcategoryName: String,
    catColor: Color,
    emoji: String,
    onRemove: () -> Unit,
    onChangePriority: (Int) -> Unit
) {
    val priorityColor = when (interest.priority) {
        1 -> GoldAccent; 2 -> CyanAccent; else -> TextMuted
    }
    val priorityLabel = when (interest.priority) {
        1 -> "PRIMARY"; 2 -> "SECONDARY"; else -> "OPTIONAL"
    }

    SystemPanel(glowColor = catColor.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)) {
                Text(emoji, fontSize = 24.sp)
                Column {
                    Text(categoryName.uppercase(), fontSize = 12.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = catColor)
                    if (subcategoryName.isNotBlank()) {
                        Text(subcategoryName, fontSize = 13.sp,
                            color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    if (interest.customGoal.isNotBlank()) {
                        Text("\"${interest.customGoal}\"", fontSize = 12.sp,
                            color = TextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = BorderGlow)
        Spacer(Modifier.height(10.dp))

        // Priority switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Black,
                letterSpacing = 2.sp, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to "P", 2 to "S", 3 to "O").forEach { (p, label) ->
                    val selected = interest.priority == p
                    val color = when (p) { 1 -> GoldAccent; 2 -> CyanAccent; else -> TextMuted }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (selected) color.copy(alpha = 0.15f) else Color.Transparent,
                                CircleShape
                            )
                            .border(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) color else BorderGlow,
                                CircleShape
                            )
                            .clickable { onChangePriority(p) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black,
                            color = if (selected) color else TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomGoalStep(
    categoryName: String,
    subcategoryName: String,
    customGoal: String,
    onGoalChanged: (String) -> Unit,
    onAddMore: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text("Tell the System your goal", fontSize = 18.sp,
            fontWeight = FontWeight.Black, color = TextPrimary)

        val areaLabel = subcategoryName.ifBlank { categoryName }
        Text(
            "For $areaLabel — what do you specifically want to achieve? " +
                    "This helps the AI generate quests tailored exactly to you.",
            fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp
        )

        // Examples
        SystemPanel(glowColor = BorderGlow) {
            Text("◈ EXAMPLES", fontSize = 10.sp, fontWeight = FontWeight.Black,
                letterSpacing = 3.sp, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            listOf(
                "Build a full-stack app in 3 months",
                "Run a 5K without stopping by June",
                "Meditate every morning for 21 days",
                "Read 12 books this year"
            ).forEach { example ->
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 3.dp)) {
                    Text("▸", fontSize = 12.sp, color = CyanAccent)
                    Text(example, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        // Text input
        OutlinedTextField(
            value = customGoal,
            onValueChange = { if (it.length <= 300) onGoalChanged(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            placeholder = { Text("Describe what you want to achieve...",
                color = TextMuted, fontSize = 14.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = BorderGlow,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
                cursorColor = CyanAccent
            ),
            shape = RoundedCornerShape(10.dp),
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            supportingText = {
                Text("${customGoal.length}/300", fontSize = 11.sp, color = TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        )

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onAddMore,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, BorderGlow)
            ) {
                Text("+ ADD MORE", fontSize = 11.sp, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black)
            }

            Button(
                onClick = onDone,
                modifier = Modifier.weight(2f).height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("REVIEW ALL", fontSize = 13.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp, color = TextPrimary)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SubcategoryPickStep(
    category: InterestCategory?,
    selectedSubcategory: String?,
    selectedPriority: Int,
    onSelectSubcategory: (String?) -> Unit,
    onSetPriority: (Int) -> Unit,
    onNext: () -> Unit
) {
    if (category == null) return

    val catColor = try {
        Color(category.color.toColorInt())
    } catch (_: Exception) { PurplePrimary }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, bottom = 100.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(categoryEmoji(category.id), fontSize = 28.sp)
                Column {
                    Text(category.name.uppercase(), fontSize = 16.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = catColor)
                    Text("Pick your specific focus area", fontSize = 13.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Subcategory options
        items(category.subcategories, key = { it.id }) { sub ->
            SubcategoryCard(
                sub = sub,
                accentColor = catColor,
                isSelected = selectedSubcategory == sub.id,
                onClick = {
                    onSelectSubcategory(if (selectedSubcategory == sub.id) null else sub.id)
                }
            )
        }

        // "General" option — no subcategory
        item {
            SubcategoryCard(
                sub = InterestSubcategory(
                    id = "", name = "General ${category.name}",
                    description = "No specific focus — mix of everything"
                ),
                accentColor = TextMuted,
                isSelected = selectedSubcategory == null,
                onClick = { onSelectSubcategory(null) }
            )
        }

        // Priority picker
        item {
            Spacer(Modifier.height(8.dp))
            SystemPanel(glowColor = BorderGlow) {
                Text("◈ PRIORITY LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp, color = TextMuted)
                Spacer(Modifier.height(12.dp))
                Text("How much focus should this area get?",
                    fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "PRIMARY", 2 to "SECONDARY", 3 to "OPTIONAL").forEach { (p, label) ->
                        val selected = selectedPriority == p
                        val color = when (p) {
                            1 -> GoldAccent; 2 -> CyanAccent; else -> TextMuted
                        }
                        OutlinedButton(
                            onClick = { onSetPriority(p) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) color.copy(alpha = 0.1f) else Color.Transparent,
                                contentColor = if (selected) color else TextMuted
                            ),
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) color else BorderGlow
                            )
                        ) {
                            Text(label, fontSize = 9.sp, letterSpacing = 1.sp,
                                fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        item {

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = catColor)
            ) {
                Text("NEXT: PROFICIENCY", fontSize = 13.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun ProficiencyPickStep(
    categoryName: String,
    subcategoryName: String,
    selectedProficiency: String,
    onSetProficiency: (String) -> Unit,
    onNext: () -> Unit
) {
    val areaLabel = subcategoryName.ifBlank { categoryName }
    val levels = listOf(
        Triple("Beginner",     "🌱", "Just starting out — quests will teach fundamentals"),
        Triple("Intermediate", "⚡", "Have some experience — quests will challenge and grow you"),
        Triple("Expert",       "🔥", "Highly skilled — quests will push your limits")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Your level in $areaLabel",
            fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary
        )
        Text(
            "The System calibrates quest difficulty to your skill level.",
            fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp
        )
        Spacer(Modifier.height(4.dp))

        levels.forEach { (level, emoji, desc) ->
            val isSelected = selectedProficiency == level
            val accentColor = when (level) {
                "Beginner"     -> CyanAccent
                "Intermediate" -> GoldAccent
                else           -> Color(0xFFEF4444)
            }
            Surface(
                onClick = { onSetProficiency(level) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accentColor else BorderGlow,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.08f) else PanelMid
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(emoji, fontSize = 32.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            level.uppercase(),
                            fontSize = 14.sp, fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = if (isSelected) accentColor else TextPrimary
                        )
                        Text(
                            desc, fontSize = 12.sp, color = TextSecondary,
                            lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            tint = accentColor, modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "NEXT: SET YOUR GOAL", fontSize = 13.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = TextPrimary
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SubcategoryCard(
    sub: InterestSubcategory,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else BorderGlow,
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.08f) else PanelMid
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        1.5.dp,
                        if (isSelected) accentColor else TextMuted,
                        CircleShape
                    )
                    .background(
                        if (isSelected) accentColor else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Icon(Icons.Default.Check, null,
                    tint = SystemBlack, modifier = Modifier.size(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(sub.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (isSelected) TextPrimary else TextSecondary)
                if (sub.description.isNotBlank()) {
                    Text(sub.description, fontSize = 12.sp, color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun CategoryPickStep(
    categories: List<InterestCategory>,
    selectedInterests: List<UserInterest>,
    onSelectCategory: (String) -> Unit,
    onGoToReview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, bottom=100.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "What do you want to level up?",
                fontSize = 18.sp, fontWeight = FontWeight.Black,
                color = TextPrimary, letterSpacing = 0.5.sp
            )
            Text(
                "Select all areas that matter to you. You can add multiple.",
                fontSize = 13.sp, color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
        }
        items(categories, key = { it.id }) { category ->
            val alreadySelected = selectedInterests.any { it.category == category.id }
            CategoryCard(
                category = category,
                isSelected = alreadySelected,
                onClick = { onSelectCategory(category.id) }
            )
        }
        if (selectedInterests.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onGoToReview,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "REVIEW SELECTIONS (${selectedInterests.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp, color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: InterestCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val parsedColor = try {
        Color(category.color.toColorInt())
    } catch (_: Exception) { PurplePrimary }

    val borderColor by animateColorAsState(
        if (isSelected) parsedColor else BorderGlow,
        tween(250), label = "border"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) parsedColor.copy(alpha = 0.08f) else PanelDark
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color dot / icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(parsedColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, parsedColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    categoryEmoji(category.id),
                    fontSize = 22.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    category.name.uppercase(),
                    fontSize = 13.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp, color = TextPrimary
                )
                Text(
                    category.description,
                    fontSize = 12.sp, color = TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "${category.subcategories.size} focus areas",
                    fontSize = 10.sp, color = parsedColor,
                    letterSpacing = 1.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null,
                    tint = parsedColor, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Default.ChevronRight, null,
                    tint = TextMuted, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun StepProgressBar(current: InterestsStep) {
    val step = InterestsStep.entries.toTypedArray()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        step.forEach {
            val active = it.ordinal <= current.ordinal
            val animColor by animateColorAsState(
                if (active) PurplePrimary else BorderGlow,
                animationSpec = tween(3000), label = "step_color"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(animColor)
            )
        }
    }
}

@Composable
fun OnboardingTopBar(onBack: () -> Unit, selectedCount: Int, step: InterestsStep) {
    val title = when (step) {
        InterestsStep.CATEGORY_PICK    -> "CHOOSE YOUR PATH"
        InterestsStep.SUBCATEGORY_PICK -> "SELECT FOCUS"
        InterestsStep.PROFICIENCY_PICK -> "YOUR SKILL LEVEL"
        InterestsStep.CUSTOM_GOAL      -> "YOUR GOAL"
        InterestsStep.REVIEW           -> "REVIEW & CONFIRM"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (step != InterestsStep.CATEGORY_PICK) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextSecondary)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = TextPrimary)
            if (selectedCount > 0) {
                Text("$selectedCount area${if (selectedCount > 1) "s" else ""} selected",
                    fontSize = 10.sp, color = CyanAccent, letterSpacing = 1.sp
                )
            }
        }
        if (selectedCount > 0 && step == InterestsStep.CATEGORY_PICK) {
            TextButton(onClick = { /*TODO*/ }) {
                Text("REVIEW", fontSize = 10.sp, color = PurpleLight, letterSpacing = 1.sp, fontWeight = FontWeight.Black)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

private fun categoryEmoji(id: String): String = when (id) {
    "technology" -> "💻"
    "physical"   -> "⚔"
    "mental"     -> "🧠"
    "social"     -> "🗣"
    "finance"    -> "📈"
    else         -> "◈"
}


private val mockCategories = listOf(
    InterestCategory(
        id = "physical",
        name = "Physical",
        description = "Level up your strength, endurance, and overall health.",
        color = "#E53935", // Red
        subcategories = listOf(
            InterestSubcategory(
                id = "c_calis",
                name = "Calisthenics",
                description = "Bodyweight mastery",
                questHints = "Focus on pull-ups, push-ups, and core."
            ),
            InterestSubcategory(
                id = "c_run",
                name = "Running",
                description = "Cardio and endurance",
                questHints = "Focus on 5k, 10k, and sprints."
            )
        )
    ),
    InterestCategory(
        id = "technology",
        name = "Technology",
        description = "Master programming, systems, and engineering.",
        color = "#00ACC1", // Cyan
        subcategories = listOf(
            InterestSubcategory(
                id = "t_and",
                name = "Android Development",
                description = "Mobile apps",
                questHints = "Jetpack Compose, Kotlin Coroutines, Architecture."
            ),
            InterestSubcategory(
                id = "t_back",
                name = "Backend",
                description = "Servers and APIs",
                questHints = "Go, Gin framework, PostgreSQL, Docker."
            )
        )
    )
)

private val mockSelectedInterests = listOf(
    UserInterest(
        category = "physical",
        subcategory = "c_run",
        priority = 1,
        customGoal = "Run a 5K under 25 minutes"
    ),
    UserInterest(
        category = "technology",
        subcategory = "t_and",
        priority = 2,
        customGoal = "Build a habit tracking app"
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 1: Category Pick")
@Composable
fun InterestsPreview_CategoryPick() {
    InterestsOnboardingScreenContent(
        step = InterestsStep.CATEGORY_PICK,
        selectedInterests = emptyList(),
        categories = mockCategories,
        isLoading = false,
        isSaving = false,
        error = null,
        draftCategoryObj = null,
        draftSubcategory = null,
        draftPriority = 1,
        draftProficiency = "Beginner",
        draftCustomGoal = "",
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        onProceedToCustomGoal = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 2: Subcategory Pick")
@Composable
fun InterestsPreview_SubcategoryPick() {
    InterestsOnboardingScreenContent(
        step = InterestsStep.SUBCATEGORY_PICK,
        selectedInterests = emptyList(),
        categories = mockCategories,
        isLoading = false,
        isSaving = false,
        error = null,
        draftCategoryObj = mockCategories[0], // Physical
        draftSubcategory = "c_calis",
        draftPriority = 1,
        draftProficiency = "Intermediate",
        draftCustomGoal = "",
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        onProceedToCustomGoal = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 3: Custom Goal")
@Composable
fun InterestsPreview_CustomGoal() {
    InterestsOnboardingScreenContent(
        step = InterestsStep.CUSTOM_GOAL,
        selectedInterests = emptyList(),
        categories = mockCategories,
        isLoading = false,
        isSaving = false,
        error = null,
        draftCategoryObj = mockCategories[0],
        draftSubcategory = "c_run",
        draftPriority = 1,
        draftProficiency = "Beginner",
        draftCustomGoal = "I want to",
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        onProceedToCustomGoal = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 4: Review")
@Composable
fun InterestsPreview_Review() {
    InterestsOnboardingScreenContent(
        step = InterestsStep.REVIEW,
        selectedInterests = mockSelectedInterests,
        categories = mockCategories,
        isLoading = false,
        isSaving = false,
        error = null,
        draftCategoryObj = null,
        draftSubcategory = null,
        draftPriority = 1,
        draftProficiency = "Beginner",
        draftCustomGoal = "",
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {},
        onProceedToCustomGoal = {}
    )
}