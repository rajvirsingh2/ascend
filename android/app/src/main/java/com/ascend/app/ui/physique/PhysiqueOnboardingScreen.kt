package com.ascend.app.ui.physique

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.remote.api.PhysiqueApiService
import com.ascend.app.data.remote.dto.SavePhysiqueRequest
import com.ascend.app.domain.model.PhysiqueProfile
import com.ascend.app.domain.model.activityOptions
import com.ascend.app.domain.model.bodyGoalOptions
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class PhysiqueViewModel @Inject constructor(
    private val api: PhysiqueApiService
) : ViewModel() {

    private val _profile = MutableStateFlow(PhysiqueProfile())
    val profile = _profile.asStateFlow()

    private val _step      = MutableStateFlow(0)
    val step = _step.asStateFlow()

    private val _isSaving  = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _done      = MutableStateFlow(false)
    val done = _done.asStateFlow()

    private val _error     = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun update(block: PhysiqueProfile.() -> PhysiqueProfile) =
        _profile.update { it.block() }

    fun next() { if (_step.value < 4) _step.update { it + 1 } else save() }
    fun back() { if (_step.value > 0) _step.update { it - 1 } }

    private fun save() {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value    = null
            try {
                val p = _profile.value
                api.savePhysique(
                    SavePhysiqueRequest(
                        age = p.age,
                        sex = p.sex,
                        heightCm = p.heightCm,
                        weightKg = p.weightKg,
                        targetWeightKg = p.targetWeightKg,
                        bodyGoal = p.bodyGoal,
                        activityLevel = p.activityLevel,
                        fitnessLevel = p.fitnessLevel,
                    )
                )
                // generate first set of exercise quests
                api.generateExerciseQuests()
                _done.value = true
            } catch (e: Exception) {
                _error.value = "Failed to save profile: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

// ─── Stateful Wrapper ────────────────────────────────────────────────────────

@Composable
fun PhysiqueOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: PhysiqueViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val step    by viewModel.step.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val done    by viewModel.done.collectAsStateWithLifecycle()
    val error   by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(done) { if (done) onComplete() }

    PhysiqueOnboardingScreenContent(
        profile = profile,
        step = step,
        isSaving = isSaving,
        error = error,
        onUpdateProfile = viewModel::update,
        onNext = viewModel::next,
        onBack = viewModel::back
    )
}

// ─── Stateless UI Composable safe for Previews ───────────────────────────────

@Composable
fun PhysiqueOnboardingScreenContent(
    profile: PhysiqueProfile,
    step: Int,
    isSaving: Boolean,
    error: String?,
    onUpdateProfile: ((PhysiqueProfile.() -> PhysiqueProfile)) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val stepTitles = listOf(
        "Basic info", "Body metrics", "Target & goal",
        "Activity level", "Fitness level"
    )

    Scaffold(containerColor = DarkColors.Void) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // progress indicator
            Text(
                text = "PHYSIQUE PROFILE · STEP ${step + 1} OF 5",
                fontSize = 10.sp, color = DarkColors.Arcane,
                fontWeight = FontWeight.Medium, letterSpacing = 0.12.sp
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / 5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color     = DarkColors.Arcane,
                trackColor = DarkColors.Dusk
            )
            Spacer(Modifier.height(6.dp))
            Text(stepTitles[step], fontSize = 20.sp,
                fontWeight = FontWeight.Medium, color = DarkColors.TextPrimary)

            Spacer(Modifier.height(20.dp))

            // animated step content
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "physique_step",
                modifier = Modifier.weight(1f)
            ) { currentStep ->
                when (currentStep) {
                    0 -> StepBasicInfo(profile, onUpdateProfile)
                    1 -> StepBodyMetrics(profile, onUpdateProfile)
                    2 -> StepTargetAndGoal(profile, onUpdateProfile)
                    3 -> StepActivityLevel(profile, onUpdateProfile)
                    4 -> StepFitnessLevel(profile, onUpdateProfile)
                }
            }

            error?.let {
                Text(it, fontSize = 12.sp, color = DarkColors.Ember,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            // navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    AscendButton(
                        text    = "BACK",
                        onClick = onBack,
                        gradient = listOf(DarkColors.Dusk, DarkColors.Deep),
                        modifier = Modifier.width(100.dp)
                    )
                }
                AscendButton(
                    text    = when {
                        isSaving  -> "SAVING..."
                        step == 4 -> "SAVE PROFILE"
                        else      -> "NEXT"
                    },
                    onClick  = onNext,
                    enabled  = !isSaving,
                    gradient = Gradients.ArcaneFlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Step composables ────────────────────────────────────────────────────────

@Composable
private fun StepBasicInfo(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // age
        LabeledInput("Age (years)") {
            OutlinedTextField(
                value = if (profile.age == 0) "" else profile.age.toString(),
                onValueChange = { update { copy(age = it.toIntOrNull() ?: 0) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = ascendFieldColors()
            )
        }

        // sex
        LabeledInput("Biological sex") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male", "female", "other").forEach { sex ->
                    val selected = profile.sex == sex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected)
                                    Brush.horizontalGradient(Gradients.ArcaneFlow)
                                else
                                    Brush.horizontalGradient(listOf(DarkColors.Abyss, DarkColors.Deep))
                            )
                            .clickable { update { copy(sex = sex) } }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(sex.replaceFirstChar { it.uppercase() },
                            fontSize = 13.sp, color = Color.White,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBodyMetrics(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        LabeledInput("Height (cm)") {
            OutlinedTextField(
                value = if (profile.heightCm == 0f) "" else profile.heightCm.toString(),
                onValueChange = { update { copy(heightCm = it.toFloatOrNull() ?: 0f) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. 175", color = DarkColors.TextHint) },
                colors = ascendFieldColors()
            )
        }

        LabeledInput("Current weight (kg)") {
            OutlinedTextField(
                value = if (profile.weightKg == 0f) "" else profile.weightKg.toString(),
                onValueChange = { update { copy(weightKg = it.toFloatOrNull() ?: 0f) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. 75", color = DarkColors.TextHint) },
                colors = ascendFieldColors()
            )
        }

        // live BMI preview
        if (profile.heightCm > 0 && profile.weightKg > 0) {
            val heightM = profile.heightCm / 100f
            val bmi = profile.weightKg / (heightM * heightM)
            val bmiCat = when {
                bmi < 18.5f -> "Underweight" to DarkColors.Cyan
                bmi < 25f   -> "Normal" to Color(0xFF39FF14)
                bmi < 30f   -> "Overweight" to Color(0xFFFFD700)
                else        -> "Obese" to DarkColors.Ember
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkColors.Abyss)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Your BMI", fontSize = 13.sp, color = DarkColors.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("%.1f".format(bmi), fontSize = 20.sp,
                        fontWeight = FontWeight.Medium, color = DarkColors.TextPrimary)
                    Text(bmiCat.first, fontSize = 12.sp,
                        color = bmiCat.second, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StepTargetAndGoal(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        LabeledInput("Target weight (kg)") {
            OutlinedTextField(
                value = if (profile.targetWeightKg == 0f) "" else profile.targetWeightKg.toString(),
                onValueChange = { update { copy(targetWeightKg = it.toFloatOrNull() ?: weightKg) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Leave same if maintaining", color = DarkColors.TextHint) },
                colors = ascendFieldColors()
            )
        }

        LabeledInput("Choose your body goal") {}
        Spacer(Modifier.height(4.dp))

        // body goal cards with illustrations
        bodyGoalOptions.forEach { option ->
            val selected = profile.bodyGoal == option.key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected)
                            Brush.horizontalGradient(
                                listOf(DarkColors.Arcane.copy(.25f), DarkColors.Cyan.copy(.15f))
                            )
                        else Brush.horizontalGradient(listOf(DarkColors.Abyss, DarkColors.Deep))
                    )
                    .border(
                        width = if (selected) 1.5.dp else 0.5.dp,
                        color = if (selected) DarkColors.Arcane else DarkColors.Dusk,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { update { copy(bodyGoal = option.key) } }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // silhouette illustration
                BodyGoalIllustration(goal = option.key)

                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) DarkColors.TextPrimary else DarkColors.TextPrimary)
                    Text(option.description, fontSize = 11.sp,
                        color = DarkColors.TextMuted, lineHeight = 15.sp)
                    Text(option.comparison, fontSize = 10.sp,
                        color = if (selected) DarkColors.Arcane else DarkColors.TextHint,
                        fontWeight = FontWeight.Medium)
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(Gradients.ArcaneFlow)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 11.sp, color = Color.White,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepActivityLevel(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text("How active are you currently?",
            fontSize = 14.sp, color = DarkColors.TextMuted)
        Spacer(Modifier.height(4.dp))

        activityOptions.forEach { (key, label) ->
            val selected = profile.activityLevel == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected)
                            Brush.horizontalGradient(
                                listOf(DarkColors.Arcane.copy(.2f), DarkColors.Cyan.copy(.1f))
                            )
                        else Brush.horizontalGradient(listOf(DarkColors.Abyss, DarkColors.Abyss))
                    )
                    .border(
                        width = if (selected) 1.5.dp else 0.5.dp,
                        color = if (selected) DarkColors.Arcane else DarkColors.Dusk,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { update { copy(activityLevel = key) } }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(label.split(" (")[0], fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = DarkColors.TextPrimary)
                    if (label.contains("(")) {
                        Text(label.substringAfter("(").dropLast(1),
                            fontSize = 11.sp, color = DarkColors.TextMuted)
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(Gradients.ArcaneFlow)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepFitnessLevel(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text("What is your current fitness experience?",
            fontSize = 14.sp, color = DarkColors.TextMuted)
        Spacer(Modifier.height(4.dp))

        val levels = listOf(
            Triple("beginner",     "Beginner",
                "Little to no exercise experience. Just starting out."),
            Triple("intermediate", "Intermediate",
                "6+ months of consistent training. Familiar with main exercises."),
            Triple("advanced",     "Advanced",
                "2+ years of serious training. Comfortable with progressive overload.")
        )

        val icons = mapOf(
            "beginner"     to "🌱",
            "intermediate" to "⚡",
            "advanced"     to "🔥"
        )

        levels.forEach { (key, title, desc) ->
            val selected = profile.fitnessLevel == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected)
                            Brush.horizontalGradient(
                                listOf(DarkColors.Arcane.copy(.25f), DarkColors.Cyan.copy(.15f))
                            )
                        else Brush.horizontalGradient(listOf(DarkColors.Abyss, DarkColors.Deep))
                    )
                    .border(
                        width = if (selected) 1.5.dp else 0.5.dp,
                        color = if (selected) DarkColors.Arcane else DarkColors.Dusk,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { update { copy(fitnessLevel = key) } }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(icons[key] ?: "", fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = DarkColors.TextPrimary)
                    Text(desc, fontSize = 11.sp,
                        color = DarkColors.TextMuted, lineHeight = 16.sp)
                }
            }
        }

        // estimated quest intensity preview
        val intensityText = when (profile.fitnessLevel) {
            "beginner"     -> "Your quests: 3 sets of 10 reps, 60s rest, focus on form"
            "intermediate" -> "Your quests: 4 sets of 8-12 reps, 45s rest, progressive load"
            "advanced"     -> "Your quests: 5 sets of 4-6 reps, 30s rest, near-maximal effort"
            else           -> ""
        }

        if (intensityText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkColors.Arcane.copy(.1f))
                    .padding(12.dp)
            ) {
                Text(intensityText, fontSize = 12.sp,
                    color = DarkColors.Arcane, lineHeight = 18.sp)
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun LabeledInput(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = DarkColors.TextMuted,
            fontWeight = FontWeight.Medium, letterSpacing = 0.04.sp)
        content()
    }
}

@Composable
private fun ascendFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = DarkColors.Arcane,
    unfocusedBorderColor = DarkColors.Dusk,
    focusedTextColor     = DarkColors.TextPrimary,
    unfocusedTextColor   = DarkColors.TextPrimary,
    cursorColor          = DarkColors.Arcane,
    focusedLabelColor    = DarkColors.Arcane,
    unfocusedLabelColor  = DarkColors.TextMuted,
)

@Preview(showBackground = true, name = "Step 1: Basic Info")
@Composable
fun PhysiquePreview_Step0() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(),
            step = 0,
            isSaving = false,
            error = null,
            onUpdateProfile = {},
            onNext = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Step 2: Body Metrics")
@Composable
fun PhysiquePreview_Step1() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            // Pre-filled to show BMI calculation
            profile = PhysiqueProfile(heightCm = 180f, weightKg = 75f),
            step = 1,
            isSaving = false,
            error = null,
            onUpdateProfile = {},
            onNext = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Step 3: Target & Goal")
@Composable
fun PhysiquePreview_Step2() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(bodyGoal = "lean_athletic"),
            step = 2,
            isSaving = false,
            error = null,
            onUpdateProfile = {},
            onNext = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Step 4: Activity Level")
@Composable
fun PhysiquePreview_Step3() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(activityLevel = "moderate"), // Assuming moderate is a key
            step = 3,
            isSaving = false,
            error = null,
            onUpdateProfile = {},
            onNext = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Step 5: Fitness Level")
@Composable
fun PhysiquePreview_Step4() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(fitnessLevel = "intermediate"),
            step = 4,
            isSaving = false,
            error = null,
            onUpdateProfile = {},
            onNext = {},
            onBack = {}
        )
    }
}