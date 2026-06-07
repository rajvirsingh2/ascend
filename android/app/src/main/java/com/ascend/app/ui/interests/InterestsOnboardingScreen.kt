package com.ascend.app.ui.interests

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.ascend.app.domain.model.InterestCategory
import com.ascend.app.domain.model.InterestSubcategory
import com.ascend.app.domain.model.UserInterest
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.components.SystemPanel
import com.ascend.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

import com.ascend.app.ui.components.*

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
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun InterestsOnboardingScreenContent(
    state: InterestsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (InterestsIntent) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF07070B),
        bottomBar = {
            OnboardingBottomBar(
                isFinalStep = state.step == InterestsStep.REVIEW,
                isLoading = state.isSaving,
                proceedEnabled = canProceed(state),
                onBack = { onIntent(InterestsIntent.GoBack) },
                onProceed = {
                    if (state.step == InterestsStep.REVIEW) onIntent(InterestsIntent.Save)
                    else onIntent(InterestsIntent.Continue)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .scanlineOverlay(isLight = true)
                .padding(padding)
        ) {
            // Top atmospheric halo
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(384.dp)
                    .blur(60.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ReactPurple.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                OnboardingHeader(
                    step = state.step,
                    onBack = { onIntent(InterestsIntent.GoBack) }
                )

                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ReactPurple, strokeWidth = 2.dp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "LOADING SYSTEM...",
                                fontFamily = jetBrainsMono,
                                fontSize = 11.sp, letterSpacing = 3.sp,
                                color = ReactInkDim, fontWeight = FontWeight.Black
                            )
                        }
                    }
                    return@Column
                }

                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally { if (isForward) it else -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (isForward) -it else it } + fadeOut())
                    },
                    label = "step_transition"
                ) { currentStep ->
                    val pickedCats = remember(state.categories, state.pickedCategoryIds) {
                        state.categories.filter { it.id in state.pickedCategoryIds }
                    }
                    when (currentStep) {
                        InterestsStep.CATEGORY_PICK -> CategoryPickStep(
                            categories = state.categories,
                            pickedCategoryIds = state.pickedCategoryIds,
                            onTogglePicked = { onIntent(InterestsIntent.TogglePickedCategory(it)) }
                        )
                        InterestsStep.FOCUS_AREAS -> FocusAreasStep(
                            pickedCategories = pickedCats,
                            selectedInterests = state.selectedInterests,
                            onToggleArea = { c, s -> onIntent(InterestsIntent.ToggleArea(c, s)) },
                            onSetPriority = { c, s, p -> onIntent(InterestsIntent.SetAreaPriority(c, s, p)) }
                        )
                        InterestsStep.PROFICIENCY_PICK -> ProficiencyPickStep(
                            pickedCategories = pickedCats,
                            proficiencyMap = state.proficiencyByCategory,
                            onSetProficiency = { c, lv -> onIntent(InterestsIntent.SetCategoryProficiency(c, lv)) }
                        )
                        InterestsStep.GLOBAL_GOAL -> GlobalGoalStep(
                            goal = state.globalGoal,
                            onGoalChanged = { onIntent(InterestsIntent.SetGlobalGoal(it)) }
                        )
                        InterestsStep.REVIEW -> ConfirmAwakeningStep(
                            selectedInterests = state.selectedInterests,
                            categories = state.categories,
                            globalGoal = state.globalGoal,
                            onRemove = { onIntent(InterestsIntent.RemoveInterest(it)) }
                        )
                    }
                }
            }
        }
    }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { onIntent(InterestsIntent.DismissError) },
            confirmButton = {
                TextButton(onClick = { onIntent(InterestsIntent.DismissError) }) {
                    Text("OK", color = ReactPurple)
                }
            },
            title = { Text("Error", color = ReactInk) },
            text = { Text(state.error, color = ReactInkDim) },
            containerColor = ReactPanel
        )
    }
}

private fun canProceed(state: InterestsState): Boolean = when (state.step) {
    InterestsStep.CATEGORY_PICK    -> state.pickedCategoryIds.isNotEmpty()
    InterestsStep.FOCUS_AREAS      -> state.selectedInterests.isNotEmpty()
    InterestsStep.PROFICIENCY_PICK -> state.pickedCategoryIds.all { state.proficiencyByCategory[it]?.isNotBlank() == true }
    InterestsStep.GLOBAL_GOAL      -> state.globalGoal.isNotBlank()
    InterestsStep.REVIEW           -> state.selectedInterests.isNotEmpty() && !state.isSaving
}

/* ============================================================
 * HEADER
 * ============================================================ */
@Composable
fun OnboardingHeader(
    step: InterestsStep,
    onBack: () -> Unit
) {
    val total = InterestsStep.entries.size
    val current = step.ordinal + 1
    val title = when (step) {
        InterestsStep.CATEGORY_PICK    -> "◈ CHOOSE YOUR PATHS"
        InterestsStep.FOCUS_AREAS      -> "◈ SET FOCUS AREAS"
        InterestsStep.PROFICIENCY_PICK -> "◈ SKILL CALIBRATION"
        InterestsStep.GLOBAL_GOAL      -> "◈ DECLARE YOUR GOAL"
        InterestsStep.REVIEW           -> "◈ CONFIRM AWAKENING"
    }
    val sub = when (step) {
        InterestsStep.CATEGORY_PICK    -> "Select core domains. Each unlocks focus areas."
        InterestsStep.FOCUS_AREAS      -> "Toggle areas. Set priority per area."
        InterestsStep.PROFICIENCY_PICK -> "Calibrate System difficulty to your skill."
        InterestsStep.GLOBAL_GOAL      -> "One directive. The AI will personalize quests."
        InterestsStep.REVIEW           -> "Confirm your quest configuration."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (step != InterestsStep.CATEGORY_PICK) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ReactInkDim)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    fontFamily = orbitron,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = ReactPurple,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1500,
                        velocity = 30.dp
                    ),
                    style = TextStyle(shadow = Shadow(ReactPurple.copy(alpha = 0.5f), blurRadius = 10f))
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SYSTEM CALIBRATION",
                    fontFamily = jetBrainsMono,
                    fontSize = 9.5.sp, letterSpacing = 2.sp, color = ReactCyan
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, ReactPanelLine, RoundedCornerShape(4.dp))
                        .background(ReactPanel.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "STEP $current/$total",
                        fontFamily = jetBrainsMono,
                        fontSize = 9.5.sp, letterSpacing = 2.sp, color = ReactInkDim,
                        maxLines = 1, softWrap = false
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        SystemProgressBar(progress = current.toFloat() / total)
        Spacer(Modifier.height(8.dp))
        Text(
            sub,
            fontFamily = jetBrainsMono,
            fontSize = 12.sp, letterSpacing = 0.25.sp,
            color = ReactInkDim
        )
    }
}

@Composable
fun SystemProgressBar(progress: Float) {
    val animProgress by animateFloatAsState(progress, tween(500), label = "p")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.DarkGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animProgress)
                .background(ReactCyan)
        )
    }
}

/* ============================================================
 * STEP 1 — CATEGORY PICK
 * ============================================================ */
@Composable
fun CategoryPickStep(
    categories: List<InterestCategory>,
    pickedCategoryIds: Set<String>,
    onTogglePicked: (String) -> Unit
) {
    val spanMap = mapOf(
        "technology" to 2, "tech" to 2,
        "physical" to 2,
        "mental" to 1,
        "social" to 1,
        "finance" to 2
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = categories.size,
            span = { idx ->
                val s = spanMap[categories[idx].id] ?: 1
                GridItemSpan(if (s == 2) 2 else 1)
            },
            key = { idx -> categories[idx].id }
        ) { idx ->
            val category = categories[idx]
            val span = spanMap[category.id] ?: 1
            BentoCategoryCard(
                category = category,
                isSelected = category.id in pickedCategoryIds,
                isWide = span == 2,
                onClick = { onTogglePicked(category.id) }
            )
        }
    }
}

@Composable
private fun BentoCategoryCard(
    category: InterestCategory,
    isSelected: Boolean,
    isWide: Boolean,
    onClick: () -> Unit
) {
    val accent = try { Color(category.color.toColorInt()) } catch (_: Exception) { categoryColor(category.id) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .reactStyleCard(selected = isSelected, glowColor = accent, cornerRadius = 12.dp)
            .clickable { onClick() }
    ) {
        CornerTick(Modifier.align(Alignment.TopEnd), accent.copy(alpha = if (isSelected) 0.7f else 0.4f), Corner.TopRight)
        CornerTick(Modifier.align(Alignment.BottomStart), accent.copy(alpha = 0.25f), Corner.BottomLeft)

        if (isWide && category.id == "finance") {
            Row(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accent.copy(alpha = 0.10f), CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        categoryIcon(category.id), null, tint = accent,
                        modifier = Modifier
                            .size(28.dp)
                            .shadow(8.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category.name.uppercase(),
                        fontFamily = orbitron, fontSize = 18.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                        color = if (isSelected) accent else ReactInk
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▸ ", fontSize = 11.sp, color = accent)
                        Text(
                            "${category.subcategories.size} FOCUS AREAS",
                            fontFamily = jetBrainsMono, fontSize = 10.sp,
                            letterSpacing = 1.sp, color = ReactInkDim
                        )
                    }
                }
                SelectedBadge(isSelected = isSelected, accent = accent)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        categoryIcon(category.id), null, tint = accent,
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(8.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    )
                    SelectedBadge(isSelected = isSelected, accent = accent)
                }
                Column {
                    Text(
                        category.name.uppercase(),
                        fontFamily = orbitron,
                        fontSize = if (isWide) 20.sp else 18.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                        color = if (isSelected) accent else ReactInk
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▸ ", fontSize = 10.sp, color = accent)
                        Text(
                            "${category.subcategories.size} FOCUS AREAS",
                            fontFamily = jetBrainsMono, fontSize = 10.sp,
                            letterSpacing = 1.sp, color = ReactInkDim
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedBadge(isSelected: Boolean, accent: Color) {
    val alpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(200), label = "badgeA")
    Box(
        modifier = Modifier
            .size(24.dp)
            .alpha(alpha)
            .background(accent.copy(alpha = 0.15f), CircleShape)
            .border(1.dp, accent.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Check, null, tint = accent, modifier = Modifier.size(14.dp))
    }
}

/* ============================================================
 * STEP 2 — FOCUS AREAS
 * ============================================================ */
@Composable
fun FocusAreasStep(
    pickedCategories: List<InterestCategory>,
    selectedInterests: List<UserInterest>,
    onToggleArea: (String, String) -> Unit,
    onSetPriority: (String, String, Int) -> Unit
) {
    val areaMap = remember(selectedInterests) {
        selectedInterests.associate { "${it.category}_${it.subcategory}" to it.priority }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Toggle areas inside each domain. Set priority per area.",
                fontFamily = jetBrainsMono, fontSize = 12.sp,
                color = ReactInkDim
            )
        }
        pickedCategories.forEach { cat ->
            val accent = try { Color(cat.color.toColorInt()) } catch (_: Exception) { categoryColor(cat.id) }
            item(key = "header_${cat.id}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon(cat.id), null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        cat.name.uppercase(),
                        fontFamily = orbitron, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = accent,
                        style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.5f), blurRadius = 8f))
                    )
                }
            }
            items(cat.subcategories, key = { "${cat.id}_${it.id}" }) { sub ->
                val key = "${cat.id}_${sub.id}"
                val priority = areaMap[key]
                FocusAreaRow(
                    name = sub.name,
                    accent = accent,
                    isOn = priority != null,
                    priority = priority ?: 2,
                    onToggle = { onToggleArea(cat.id, sub.id) },
                    onSetPriority = { p -> onSetPriority(cat.id, sub.id, p) }
                )
            }
        }
    }
}

@Composable
private fun FocusAreaRow(
    name: String, accent: Color, isOn: Boolean, priority: Int,
    onToggle: () -> Unit, onSetPriority: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reactStyleCard(selected = isOn, glowColor = accent, cornerRadius = 10.dp)
            .clickable { onToggle() }
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isOn) accent else ReactPanelLine, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isOn) {
                    Box(
                        modifier = Modifier
                            .size(8.dp).clip(CircleShape).background(accent)
                            .shadow(6.dp, CircleShape, ambientColor = accent, spotColor = accent)
                    )
                }
            }
            Text(
                name,
                fontFamily = jetBrainsMono, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (isOn) ReactInk else ReactInkDim,
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedVisibility(visible = isOn) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple(1, "PRIMARY", ReactGold),
                    Triple(2, "SECONDARY", ReactPurple),
                    Triple(3, "OPTIONAL", ReactInkDim)
                ).forEach { (p, label, color) ->
                    PrioPill(label, color, priority == p, { onSetPriority(p) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PrioPill(
    label: String, color: Color, active: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (active) color else ReactPanelLine, RoundedCornerShape(6.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = jetBrainsMono, fontSize = 9.5.sp, letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Black,
            color = if (active) color else ReactInkDim
        )
    }
}

/* ============================================================
 * STEP 3 — PROFICIENCY
 * ============================================================ */
@Composable
fun ProficiencyPickStep(
    pickedCategories: List<InterestCategory>,
    proficiencyMap: Map<String, String>,
    onSetProficiency: (String, String) -> Unit
) {
    val levels = listOf(
        Triple("Beginner", "🌱", "Just starting"),
        Triple("Intermediate", "⚡", "Some experience"),
        Triple("Expert", "🔥", "Highly skilled")
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                "Calibrate System difficulty per domain.",
                fontFamily = jetBrainsMono, fontSize = 12.sp,
                color = ReactInkDim
            )
        }
        items(pickedCategories, key = { it.id }) { cat ->
            val accent = try { Color(cat.color.toColorInt()) } catch (_: Exception) { categoryColor(cat.id) }
            val selected = proficiencyMap[cat.id] ?: ""

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon(cat.id), null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        cat.name.uppercase(),
                        fontFamily = orbitron, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = accent
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    levels.forEach { (level, emoji, desc) ->
                        val active = selected == level
                        val color = when (level) {
                            "Beginner" -> ReactCyan
                            "Intermediate" -> ReactGold
                            else -> ReactRed
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .reactStyleCard(selected = active, glowColor = color, cornerRadius = 10.dp)
                                .clickable { onSetProficiency(cat.id, level) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(emoji, fontSize = 20.sp)
                            Text(
                                level.uppercase(),
                                fontFamily = orbitron, fontSize = 10.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                                color = if (active) color else ReactInkDim
                            )
                            Text(
                                desc,
                                fontFamily = jetBrainsMono, fontSize = 9.sp,
                                color = ReactInkDim.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================
 * STEP 4 — GLOBAL GOAL
 * ============================================================ */
@Composable
fun GlobalGoalStep(
    goal: String,
    onGoalChanged: (String) -> Unit
) {
    val charLimit = 240
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Declare what you want the System to drive you toward. Be specific.",
            fontFamily = jetBrainsMono, fontSize = 12.sp,
            color = ReactInkDim,
            lineHeight = 18.sp
        )
        SystemPanel(glowColor = ReactPanelLine) {
            Text(
                "▸ EXAMPLE DIRECTIVES",
                fontFamily = jetBrainsMono, fontSize = 10.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
                color = ReactCyan
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "Become fluent in Rust and ship an open-source tool.",
                "Deadlift 2x bodyweight and sleep 8h nightly.",
                "Build a \$2k/mo side income stream."
            ).forEach { example ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("▸", fontFamily = jetBrainsMono, fontSize = 12.sp, color = ReactCyan)
                    Text(
                        example,
                        fontFamily = jetBrainsMono, fontSize = 12.sp,
                        color = ReactInkDim,
                        lineHeight = 17.sp
                    )
                }
            }
        }
        Column {
            Text(
                "TELL THE SYSTEM YOUR GOAL",
                fontFamily = jetBrainsMono, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = ReactInkDim
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = goal,
                onValueChange = { if (it.length <= charLimit) onGoalChanged(it) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                placeholder = {
                    Text(
                        "Your directive here...",
                        fontFamily = jetBrainsMono, color = ReactInkDim.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                textStyle = TextStyle(
                    fontFamily = jetBrainsMono, fontSize = 14.sp,
                    color = ReactInk, lineHeight = 21.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ReactCyan,
                    unfocusedBorderColor = ReactPanelLine,
                    focusedContainerColor = ReactPanel,
                    unfocusedContainerColor = ReactPanel,
                    cursorColor = ReactCyan
                ),
                shape = RoundedCornerShape(10.dp),
                maxLines = 8,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${goal.length} / $charLimit",
                fontFamily = jetBrainsMono, fontSize = 10.sp,
                color = ReactInkDim.copy(alpha = 0.6f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}

/* ============================================================
 * STEP 5 — CONFIRM AWAKENING
 * ============================================================ */
@Composable
fun ConfirmAwakeningStep(
    selectedInterests: List<UserInterest>,
    categories: List<InterestCategory>,
    globalGoal: String,
    onRemove: (Int) -> Unit
) {
    val catMap = categories.associateBy { it.id }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SystemPanel(glowColor = ReactPurple) {
                Text(
                    "◈ FOCUS AREAS · ${selectedInterests.size}",
                    fontFamily = jetBrainsMono, fontSize = 11.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                    color = ReactPurple
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedInterests.forEachIndexed { idx, interest ->
                        val cat = catMap[interest.category]
                        val sub = cat?.subcategories?.find { it.id == interest.subcategory }
                        val catColor = try {
                            Color((cat?.color ?: "#7C3AED").toColorInt())
                        } catch (_: Exception) { categoryColor(interest.category) }
                        val (prioLabel, prioColor) = when (interest.priority) {
                            1 -> "PRIMARY" to ReactGold
                            2 -> "SECONDARY" to ReactPurple
                            else -> "OPTIONAL" to ReactInkDim
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(ReactPanel)
                                .border(1.dp, ReactPanelLine, RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                categoryIcon(interest.category), null,
                                tint = catColor, modifier = Modifier.size(18.dp)
                            )
                            Text(
                                sub?.name ?: interest.subcategory,
                                fontFamily = jetBrainsMono, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, color = ReactInk,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(prioColor.copy(alpha = 0.12f))
                                    .border(1.dp, prioColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    prioLabel,
                                    fontFamily = jetBrainsMono, fontSize = 8.5.sp,
                                    letterSpacing = 1.sp, fontWeight = FontWeight.Black,
                                    color = prioColor
                                )
                            }
                            Icon(
                                Icons.Filled.Close, null, tint = ReactInkDim,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { onRemove(idx) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SystemPanel(glowColor = ReactPanelLine) {
                Text(
                    "▸ DIRECTIVE",
                    fontFamily = jetBrainsMono, fontSize = 10.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.5.sp,
                    color = ReactCyan
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    globalGoal.ifBlank { "(no directive set)" },
                    fontFamily = jetBrainsMono, fontSize = 13.sp,
                    color = ReactInkDim,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/* ============================================================
 * BOTTOM BAR
 * ============================================================ */
@Composable
fun OnboardingBottomBar(
    isFinalStep: Boolean,
    isLoading: Boolean = false,
    proceedEnabled: Boolean = true,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "p"
    )
    val proceedElev = if (isFinalStep) (pulse * 22).dp else 14.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF07070B).copy(alpha = 0.92f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, ReactPanelLine, RoundedCornerShape(10.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "← BACK",
                fontFamily = jetBrainsMono, fontSize = 11.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Black,
                color = ReactInkDim
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(
                    proceedElev, RoundedCornerShape(10.dp),
                    ambientColor = ReactPurple, spotColor = ReactCyan
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(ReactPurple, ReactCyan)))
                .alpha(if (proceedEnabled) 1f else 0.45f)
                .clickable(enabled = proceedEnabled && !isLoading) { onProceed() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isFinalStep) Icons.Filled.Bolt else Icons.AutoMirrored.Filled.ArrowForward,
                        null, tint = Color.Black, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFinalStep) "ENTER THE SYSTEM" else "CONTINUE",
                        fontFamily = orbitron, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 2.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/* ============================================================
 * HELPERS — reactCard modifier, corner ticks, scanlines
 * ============================================================ */



private enum class Corner { TopRight, BottomLeft, TopLeft, BottomRight }

@Composable
private fun CornerTick(modifier: Modifier, color: Color, corner: Corner) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(if (corner == Corner.TopRight) 36.dp else 24.dp)
    ) {
        val w = size.width; val h = size.height
        val sw = 1.5f
        when (corner) {
            Corner.TopRight -> {
                drawLine(color, Offset(0f, 0f), Offset(w, 0f), strokeWidth = sw)
                drawLine(color, Offset(w, 0f), Offset(w, h), strokeWidth = sw)
            }
            Corner.BottomLeft -> {
                drawLine(color, Offset(0f, h), Offset(w, h), strokeWidth = sw)
                drawLine(color, Offset(0f, 0f), Offset(0f, h), strokeWidth = sw)
            }
            Corner.TopLeft -> {
                drawLine(color, Offset(0f, 0f), Offset(w, 0f), strokeWidth = sw)
                drawLine(color, Offset(0f, 0f), Offset(0f, h), strokeWidth = sw)
            }
            Corner.BottomRight -> {
                drawLine(color, Offset(0f, h), Offset(w, h), strokeWidth = sw)
                drawLine(color, Offset(w, 0f), Offset(w, h), strokeWidth = sw)
            }
        }
    }
}

/* ============================================================
 * PREVIEW
 * ============================================================ */
private val mockCategories = listOf(
    InterestCategory(
        id = "technology", name = "Technology",
        description = "Master programming.",
        color = "#00E5FF",
        subcategories = listOf(
            InterestSubcategory("t_and", "Android Development", "Mobile apps", ""),
            InterestSubcategory("t_back", "Backend", "Servers and APIs", "")
        )
    ),
    InterestCategory(
        id = "physical", name = "Physical",
        description = "Strength, endurance, health.",
        color = "#FFB4AB",
        subcategories = listOf(
            InterestSubcategory("c_calis", "Calisthenics", "Bodyweight", ""),
            InterestSubcategory("c_run", "Running", "Cardio", "")
        )
    ),
    InterestCategory(
        id = "mental", name = "Mental",
        description = "Sharpen mind.",
        color = "#B388FF",
        subcategories = listOf(InterestSubcategory("m_med", "Meditation", "", ""))
    )
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 1: Category Pick")
@Composable
fun InterestsPreview_CategoryPick() {
    InterestsOnboardingScreenContent(
        state = InterestsState(
            step = InterestsStep.CATEGORY_PICK,
            categories = mockCategories,
            pickedCategoryIds = setOf("technology")
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 2: Focus Areas")
@Composable
fun InterestsPreview_FocusAreas() {
    InterestsOnboardingScreenContent(
        state = InterestsState(
            step = InterestsStep.FOCUS_AREAS,
            categories = mockCategories,
            pickedCategoryIds = setOf("technology", "physical"),
            selectedInterests = listOf(
                UserInterest("technology", "t_and", "", 1),
                UserInterest("physical", "c_run", "", 2)
            )
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 3: Proficiency")
@Composable
fun InterestsPreview_Proficiency() {
    InterestsOnboardingScreenContent(
        state = InterestsState(
            step = InterestsStep.PROFICIENCY_PICK,
            categories = mockCategories,
            pickedCategoryIds = setOf("technology", "physical"),
            proficiencyByCategory = mapOf("technology" to "Intermediate")
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 4: Global Goal")
@Composable
fun InterestsPreview_GlobalGoal() {
    InterestsOnboardingScreenContent(
        state = InterestsState(
            step = InterestsStep.GLOBAL_GOAL,
            categories = mockCategories,
            pickedCategoryIds = setOf("technology", "physical"),
            globalGoal = "Land a senior engineer role and run a half-marathon by Q4."
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Step 5: Review")
@Composable
fun InterestsPreview_Review() {
    InterestsOnboardingScreenContent(
        state = InterestsState(
            step = InterestsStep.REVIEW,
            categories = mockCategories,
            pickedCategoryIds = setOf("technology", "physical"),
            selectedInterests = listOf(
                UserInterest("technology", "t_and", "", 2),
                UserInterest("physical", "c_run", "", 1)
            ),
            globalGoal = "Land a senior engineer role and run a half-marathon by Q4."
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onIntent = {}
    )
}