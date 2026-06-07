package com.ascend.app.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.ascend.app.domain.model.Goal
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.components.*
import com.ascend.app.ui.theme.*
import java.util.Locale



/* ============================================================
 *  ROOT (stateful)
 * ============================================================ */
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GoalsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    GoalsScreenContent(
        isLoading = state.isLoading,
        goals = state.goals,
        showCreateDialog = state.showCreateDialog,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent
    )
}

/* ============================================================
 *  CONTENT (stateless)
 * ============================================================ */
@Composable
fun GoalsScreenContent(
    isLoading: Boolean,
    goals: List<Goal>,
    showCreateDialog: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (GoalsIntent) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF07070B),
        floatingActionButton = {
            NewGoalFab(onClick = { onIntent(GoalsIntent.ShowCreateDialog) })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .padding(padding)
        ) {
            // Top atmospheric halo
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ReactPurple.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ReactPurple, strokeWidth = 2.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "LOADING GOALS...",
                            fontFamily = jetBrainsMono,
                            fontSize = 11.sp, letterSpacing = 3.sp,
                            color = ReactInkDim, fontWeight = FontWeight.Black
                        )
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 14.dp, bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row with counter
                item {
                    val completed = goals.count { it.progress >= 100 }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "◈ ",
                                fontFamily = orbitron,
                                fontSize = 22.sp,
                                color = ReactCyan
                            )
                            Text(
                                "LONG-TERM GOALS",
                                fontFamily = orbitron,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = ReactPurple,
                                style = TextStyle(
                                    shadow = Shadow(ReactPurple.copy(alpha = 0.4f), blurRadius = 10f)
                                )
                            )
                        }
                        if (goals.isNotEmpty()) {
                            Text(
                                "$completed/${goals.size}",
                                fontFamily = jetBrainsMono,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = ReactInkDim
                            )
                        }
                    }
                    // Border under header
                    HorizontalDivider(color = ReactPanelLine)
                }

                if (goals.isEmpty()) {
                    item { EmptyGoalsState() }
                } else {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onToggleDone = { onIntent(GoalsIntent.ToggleGoalDone(goal.id)) }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet for create goal
    if (showCreateDialog) {
        CreateGoalBottomSheet(
            onDismiss = { onIntent(GoalsIntent.DismissDialog) },
            onCreate = { title, desc, skillArea, priority ->
                onIntent(GoalsIntent.CreateGoal(title, desc, skillArea, priority))
            }
        )
    }
}

/* ============================================================
 *  GOAL CARD
 * ============================================================ */
@Composable
fun GoalCard(goal: Goal, onToggleDone: () -> Unit) {
    val isDone = goal.progress >= 100
    val skillCol = skillAreaColor(goal.skillArea)
    val rank = rankForPriority(goal.priority)
    val rankCol = rankColor(rank)

    val borderColor = if (isDone) ReactGreen else ReactPanelLine
    val glowColor = if (isDone) ReactGreen else skillCol.copy(alpha = 0.5f)

    val animProgress by animateFloatAsState(
        (goal.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700, easing = EaseOutCubic),
        label = "p"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDone) Modifier.shadow(
                    14.dp, RoundedCornerShape(10.dp),
                    ambientColor = ReactGreen, spotColor = ReactGreen
                ) else Modifier.shadow(
                    8.dp, RoundedCornerShape(10.dp),
                    ambientColor = glowColor, spotColor = glowColor
                )
            )
            .clip(RoundedCornerShape(10.dp))
            .background(ReactPanel)
            .border(if (isDone) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .scanlineHorizontal()
            .padding(14.dp)
    ) {
        // Corner blob top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            skillCol.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(64f, 0f),
                        radius = 80f
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDone) ReactGreen.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .border(
                        1.5.dp,
                        if (isDone) ReactGreen else ReactPanelLine,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Filled.Check, null,
                        tint = ReactGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Chip row + percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Chip(
                            text = goal.skillArea.uppercase(Locale.ROOT),
                            color = skillCol,
                            filled = true
                        )
                        Chip(
                            text = "$rank-RANK",
                            color = rankCol,
                            filled = false
                        )
                    }
                    Text(
                        text = "${goal.progress}%",
                        fontFamily = jetBrainsMono,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) ReactGreen else ReactCyan
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Title
                Text(
                    text = goal.title,
                    fontFamily = orbitron,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = if (isDone) ReactInkDim else ReactInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Desc
                if (goal.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = goal.description,
                        fontFamily = jetBrainsMono,
                        fontSize = 12.sp,
                        color = ReactInkDim.copy(alpha = 0.8f),
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(11.dp))

                // Progress bar
                ProgressGlowBar(
                    fraction = animProgress,
                    gradient = if (isDone)
                        listOf(ReactGreen, ReactGreen)
                    else
                        listOf(ReactPurple, ReactCyan)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color, filled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (filled) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = if (filled) 0.5f else 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = jetBrainsMono,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ProgressGlowBar(fraction: Float, gradient: List<Color>) {
    val infiniteTransition = rememberInfiniteTransition(label = "barEdge")
    val edgeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "edge"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(ReactPanelLine)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(Brush.horizontalGradient(gradient), RoundedCornerShape(3.dp))
                .shadow(8.dp, RoundedCornerShape(3.dp),
                    ambientColor = gradient.last(), spotColor = gradient.last())
        ) {
            if (fraction > 0.02f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(16.dp)
                        .blur(2.dp)
                        .background(Color.White.copy(alpha = edgeAlpha))
                )
            }
        }
    }
}

/* ============================================================
 *  EMPTY STATE
 * ============================================================ */
@Composable
private fun EmptyGoalsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚔️", fontSize = 44.sp, modifier = Modifier.alpha(0.4f))
        Spacer(Modifier.height(14.dp))
        Text(
            "NO GOALS SET",
            fontFamily = orbitron,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = ReactInkDim
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Declare a long-term objective and the System will forge quests toward it.",
            fontFamily = jetBrainsMono,
            fontSize = 12.sp,
            color = ReactInkFaint,
            lineHeight = 18.sp,
            modifier = Modifier.widthIn(max = 240.dp),
            textAlign = TextAlign.Center
        )
    }
}

/* ============================================================
 *  FAB
 * ============================================================ */
@Composable
private fun NewGoalFab(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .height(48.dp)
            .shadow(
                (glow * 20).dp,
                RoundedCornerShape(24.dp),
                ambientColor = ReactPurple, spotColor = ReactCyan
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(ReactPurple, ReactCyan)))
            .clickable { onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Add, null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "NEW GOAL",
                fontFamily = orbitron,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = Color.Black
            )
        }
    }
}

/* ============================================================
 *  CREATE GOAL BOTTOM SHEET
 * ============================================================ */
@Composable
fun CreateGoalBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int) -> Unit
) {
    if (LocalInspectionMode.current) {
        CreateGoalSheetContent(onDismiss, onCreate)
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            CreateGoalSheetContent(onDismiss, onCreate)
        }
    }
}

@Composable
private fun CreateGoalSheetContent(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var skillArea by remember { mutableStateOf("fitness") }
    val priority by remember { mutableIntStateOf(2) }

    val skillAreas = listOf("fitness", "learning", "mindfulness", "productivity", "social", "creativity")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A).copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        24.dp,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        ambientColor = ReactPurple, spotColor = ReactCyan
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(ReactPanel)
                    .border(
                        1.dp,
                        ReactPanelLine,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* eat clicks */ }
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "▸ NEW GOAL",
                            fontFamily = jetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = ReactCyan
                        )
                        Icon(
                            Icons.Filled.Close, null,
                            tint = ReactInkDim,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    // Title field
                    SheetField(
                        label = "TITLE",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g. Reach S-Rank fitness",
                        singleLine = true
                    )

                    // Description field
                    SheetField(
                        label = "DESCRIPTION",
                        value = description,
                        onValueChange = { description = it },
                        placeholder = "What does success look like?",
                        singleLine = false,
                        minHeight = 80.dp
                    )

                    // Category chips
                    Column {
                        Text(
                            "CATEGORY",
                            fontFamily = jetBrainsMono,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReactInkDim
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRowCategoryChips(
                            options = skillAreas,
                            selected = skillArea,
                            onSelect = { skillArea = it }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // CREATE button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(
                                14.dp, RoundedCornerShape(10.dp),
                                ambientColor = ReactPurple, spotColor = ReactCyan
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(ReactPurple, ReactCyan)))
                            .alpha(if (title.isNotBlank()) 1f else 0.45f)
                            .clickable(enabled = title.isNotBlank()) {
                                onCreate(
                                    title.ifBlank { "Untitled Goal" },
                                    description.ifBlank { "No description." },
                                    skillArea,
                                    priority
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Add, null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CREATE GOAL",
                                fontFamily = orbitron,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp = 0.dp
) {
    Column {
        Text(
            label,
            fontFamily = jetBrainsMono,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            color = ReactInkDim
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
            placeholder = {
                Text(
                    placeholder,
                    fontFamily = jetBrainsMono,
                    color = ReactInkFaint,
                    fontSize = 14.sp
                )
            },
            textStyle = TextStyle(
                fontFamily = jetBrainsMono,
                fontSize = 14.sp,
                color = ReactInk
            ),
            singleLine = singleLine,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ReactCyan,
                unfocusedBorderColor = ReactPanelLine,
                focusedContainerColor = Color(0xFF0C0C16),
                unfocusedContainerColor = Color(0xFF0C0C16),
                cursorColor = ReactCyan
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Done
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCategoryChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        options.forEach { opt ->
            val active = selected == opt
            val color = skillAreaColor(opt)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active) color.copy(alpha = 0.15f) else ReactPanelLine.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        if (active) color else ReactPanelLine,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Text(
                    opt.uppercase(Locale.ROOT),
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) color else ReactInkDim
                )
            }
        }
    }
}

/* ============================================================
 *  HELPERS
 * ============================================================ */


/* ============================================================
 *  PREVIEWS
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Goals (Empty)")
@Composable
fun GoalsScreenPreview_Empty() {
    MaterialTheme {
        GoalsScreenContent(
            isLoading = false,
            goals = emptyList(),
            showCreateDialog = false,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Goals (Populated)")
@Composable
fun GoalsScreenPreview_Populated() {
    MaterialTheme {
        GoalsScreenContent(
            isLoading = false,
            goals = listOf(
                Goal(
                    id = "g1",
                    title = "Climb Mt. Fuji",
                    description = "Complete the Yoshida trail ascent before the season ends. Requires endurance training.",
                    skillArea = "fitness",
                    priority = 2,
                    status = "IN_PROGRESS",
                    progress = 45
                ),
                Goal(
                    id = "g2",
                    title = "Master React Advanced Patterns",
                    description = "Complete the final 3 modules of the advanced architecture course.",
                    skillArea = "learning",
                    priority = 3,
                    status = "IN_PROGRESS",
                    progress = 80
                ),
                Goal(
                    id = "g3",
                    title = "Acquire Rare Skill: Polyglot",
                    description = "Reach B2 fluency level in Japanese. Daily practice required.",
                    skillArea = "mindfulness",
                    priority = 1,
                    status = "IN_PROGRESS",
                    progress = 15
                )
            ),
            showCreateDialog = false,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Create Goal Sheet")
@Composable
fun CreateGoalSheetPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(Color(0xFF07070B))) {
            CreateGoalBottomSheet(
                onDismiss = {},
                onCreate = { _, _, _, _ -> }
            )
        }
    }
}