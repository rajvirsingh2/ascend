package com.ascend.app.ui.physique

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ascend.app.data.remote.api.PhysiqueApiService
import com.ascend.app.data.remote.dto.SavePhysiqueRequest
import com.ascend.app.domain.model.PhysiqueProfile
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DangerRed
import com.ascend.app.ui.theme.GoldAccent
import com.ascend.app.ui.theme.PanelMid
import com.ascend.app.ui.theme.PurpleLight
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.SuccessGreen
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextMuted
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

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

/* ============================================================
 *  STEPS
 * ============================================================ */
enum class PhysiqueStep {
    BIOMETRIC_SCAN,
    AGE,
    BODY_METRICS,
    ACTIVITY_LEVEL,
    OBJECTIVE,
    SUMMARY
}

/* ============================================================
 *  ROOT
 * ============================================================ */
@Composable
fun PhysiqueOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: PhysiqueViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val stepInt by viewModel.step.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(done) { if (done) onComplete() }

    PhysiqueOnboardingScreenContent(
        profile = profile,
        step = PhysiqueStep.entries[stepInt.coerceIn(0, PhysiqueStep.entries.lastIndex)],
        isSaving = isSaving,
        error = error,
        onUpdateProfile = viewModel::update,
        onNext = viewModel::next,
        onBack = viewModel::back
    )
}

@Composable
fun PhysiqueOnboardingScreenContent(
    profile: PhysiqueProfile,
    step: PhysiqueStep,
    isSaving: Boolean,
    error: String?,
    onUpdateProfile: ((PhysiqueProfile.() -> PhysiqueProfile)) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(containerColor = SystemBlack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemBlack)
                .scanlineHorizontal()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(280.dp)
                    .blur(50.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(PurplePrimary.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                PhysiqueHeader(step = step)
                Spacer(Modifier.height(20.dp))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally { if (isForward) it else -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (isForward) -it else it } + fadeOut())
                    },
                    label = "physique_step",
                    modifier = Modifier.weight(1f)
                ) { current ->
                    when (current) {
                        PhysiqueStep.BIOMETRIC_SCAN -> StepBiometric(profile, onUpdateProfile)
                        PhysiqueStep.AGE            -> StepAge(profile, onUpdateProfile)
                        PhysiqueStep.BODY_METRICS   -> StepBodyMetrics(profile, onUpdateProfile)
                        PhysiqueStep.ACTIVITY_LEVEL -> StepActivity(profile, onUpdateProfile)
                        PhysiqueStep.OBJECTIVE      -> StepObjective(profile, onUpdateProfile)
                        PhysiqueStep.SUMMARY        -> StepSummary(profile)
                    }
                }

                error?.let {
                    Text(
                        it,
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        color = DangerRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                PhysiqueBottomBar(
                    isFirstStep = step == PhysiqueStep.BIOMETRIC_SCAN,
                    isFinalStep = step == PhysiqueStep.SUMMARY,
                    isSaving = isSaving,
                    proceedEnabled = canProceed(step, profile),
                    onBack = onBack,
                    onNext = onNext
                )
            }
        }
    }
}

private fun canProceed(step: PhysiqueStep, p: PhysiqueProfile): Boolean = when (step) {
    PhysiqueStep.BIOMETRIC_SCAN -> p.sex.isNotBlank()
    PhysiqueStep.AGE            -> p.age in 14..80
    PhysiqueStep.BODY_METRICS   -> p.heightCm > 0 && p.weightKg > 0
    PhysiqueStep.ACTIVITY_LEVEL -> p.activityLevel.isNotBlank()
    PhysiqueStep.OBJECTIVE      -> p.bodyGoal.isNotBlank()
    PhysiqueStep.SUMMARY        -> true
}

/* ============================================================
 *  HEADER
 * ============================================================ */
@Composable
private fun PhysiqueHeader(step: PhysiqueStep) {
    val total = PhysiqueStep.entries.size
    val current = step.ordinal + 1
    val title = when (step) {
        PhysiqueStep.BIOMETRIC_SCAN -> "BIOMETRIC SCAN"
        PhysiqueStep.AGE            -> "AGE"
        PhysiqueStep.BODY_METRICS   -> "BODY METRICS"
        PhysiqueStep.ACTIVITY_LEVEL -> "ACTIVITY LEVEL"
        PhysiqueStep.OBJECTIVE      -> "OBJECTIVE"
        PhysiqueStep.SUMMARY        -> "CALIBRATION COMPLETE"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier=Modifier.weight(1f)
            ) {
                Text(
                    "◈ ",
                    fontFamily = orbitron,
                    fontSize = 18.sp,
                    color = CyanAccent
                )
                Text(
                    "PHYSIQUE · $title",
                    fontFamily = orbitron,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = PurpleLight,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1500,
                        initialDelayMillis = 1500,
                        velocity = 30.dp
                    ),
                    style = TextStyle(shadow = Shadow(PurpleLight.copy(alpha = 0.4f), blurRadius = 10f))
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, BorderGlow.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .background(PanelMid.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    "STEP $current/$total",
                    fontFamily = jetBrainsMono,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.sp,
                    color = TextMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ProgressBarWithEdge(progress = current.toFloat() / total)
    }
}

@Composable
private fun ProgressBarWithEdge(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "progEdge")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "p"
    )
    val anim by animateFloatAsState(progress, tween(500), label = "prog")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(PanelMid)
            .border(1.dp, BorderGlow.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(anim)
                .background(
                    Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                    RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(20.dp)
                    .blur(4.dp)
                    .background(Color.White.copy(alpha = 0.5f * pulse))
            )
        }
    }
}

/* ============================================================
 *  STEP 1 — BIOMETRIC SCAN (sex)
 * ============================================================ */
@Composable
private fun StepBiometric(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SexCard(
            symbol = "♂",
            label = "MALE",
            selected = profile.sex == "male",
            modifier = Modifier.weight(1f),
            onClick = { update { copy(sex = "male") } }
        )
        SexCard(
            symbol = "♀",
            label = "FEMALE",
            selected = profile.sex == "female",
            modifier = Modifier.weight(1f),
            onClick = { update { copy(sex = "female") } }
        )
    }
}

@Composable
private fun SexCard(
    symbol: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val color = CyanAccent
    Box(
        modifier = modifier
            .reactStyleCard(selected, color)
            .clickable { onClick() }
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                symbol,
                fontSize = 56.sp,
                color = if (selected) color else TextSecondary,
                style = TextStyle(
                    shadow = Shadow(
                        if (selected) color.copy(alpha = 0.6f) else Color.Transparent,
                        blurRadius = 20f
                    )
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                fontFamily = orbitron,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp,
                color = if (selected) color else TextPrimary
            )
        }
    }
}

/* ============================================================
 *  STEP 2 — AGE (slider + huge value)
 * ============================================================ */
@Composable
private fun StepAge(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    val ageValue = if (profile.age == 0) 25 else profile.age

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            ageValue.toString(),
            fontFamily = orbitron,
            fontSize = 88.sp,
            fontWeight = FontWeight.Black,
            color = CyanAccent,
            style = TextStyle(shadow = Shadow(CyanAccent.copy(alpha = 0.5f), blurRadius = 28f))
        )
        Text(
            "YEARS",
            fontFamily = jetBrainsMono,
            fontSize = 11.sp,
            letterSpacing = 4.sp,
            color = TextMuted
        )

        Spacer(Modifier.height(32.dp))

        AscendSlider(
            value = ageValue.toFloat(),
            range = 14f..80f,
            steps = 0,
            accentColor = CyanAccent,
            onValueChange = { update { copy(age = it.roundToInt()) } }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SliderLegend("14")
            SliderLegend("47")
            SliderLegend("80")
        }
    }
}

/* ============================================================
 *  STEP 3 — BODY METRICS (height + weight)
 * ============================================================ */
@Composable
private fun StepBodyMetrics(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        MetricSlider(
            label = "HEIGHT",
            unit = "cm",
            value = if (profile.heightCm > 0) profile.heightCm else 178f,
            range = 120f..220f,
            accent = CyanAccent,
            onChange = { update { copy(heightCm = it) } }
        )

        HorizontalDivider(color = BorderGlow.copy(alpha = 0.2f))

        MetricSlider(
            label = "WEIGHT",
            unit = "kg",
            value = if (profile.weightKg > 0) profile.weightKg else 74f,
            range = 40f..160f,
            accent = CyanAccent,
            onChange = { update { copy(weightKg = it) } }
        )
    }
}

@Composable
private fun MetricSlider(
    label: String,
    unit: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                label,
                fontFamily = jetBrainsMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.0f".format(value),
                    fontFamily = orbitron,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = accent,
                    style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.5f), blurRadius = 16f))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        AscendSlider(
            value = value,
            range = range,
            steps = 0,
            accentColor = accent,
            onValueChange = onChange
        )
    }
}

/* ============================================================
 *  STEP 4 — ACTIVITY LEVEL
 * ============================================================ */
@Composable
private fun StepActivity(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    val options = listOf(
        Triple("sedentary", "SEDENTARY", "Desk-bound · little exercise"),
        Triple("light",     "LIGHT",     "1–3 days / week"),
        Triple("moderate",  "MODERATE",  "3–5 days / week"),
        Triple("intense",   "INTENSE",   "6–7 days / week")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (key, title, desc) ->
            val selected = profile.activityLevel == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .reactStyleCard(selected, PurpleLight)
                    .clickable { update { copy(activityLevel = key) } }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.FitnessCenter,
                    null,
                    tint = if (selected) PurpleLight else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontFamily = orbitron,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = if (selected) PurpleLight else TextPrimary
                    )
                    Text(
                        desc,
                        fontFamily = jetBrainsMono,
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.75f),
                        lineHeight = 15.sp
                    )
                }
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = PurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  STEP 5 — OBJECTIVE (Cut / Maintain / Bulk)
 * ============================================================ */
@Composable
private fun StepObjective(
    profile: PhysiqueProfile,
    update: (PhysiqueProfile.() -> PhysiqueProfile) -> Unit
) {
    val options = listOf(
        ObjectiveOpt("cut",      "CUT",      "Lose fat",     CyanAccent),
        ObjectiveOpt("maintain", "MAINTAIN", "Recomp",       GoldAccent),
        ObjectiveOpt("bulk",     "BULK",     "Gain muscle",  SuccessGreen)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { opt ->
            ObjectiveCard(
                opt = opt,
                selected = profile.bodyGoal == opt.key,
                modifier = Modifier.weight(1f),
                onClick = { update { copy(bodyGoal = opt.key) } }
            )
        }
    }
}

private data class ObjectiveOpt(
    val key: String,
    val label: String,
    val desc: String,
    val color: Color
)

@Composable
private fun ObjectiveCard(
    opt: ObjectiveOpt,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .reactStyleCard(selected, opt.color)
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                opt.label,
                fontFamily = orbitron,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = if (selected) opt.color else TextPrimary
            )
            Text(
                opt.desc,
                fontFamily = jetBrainsMono,
                fontSize = 10.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ============================================================
 *  STEP 6 — SUMMARY (BMI + BMR + TDEE + Target)
 * ============================================================ */
@Composable
private fun StepSummary(profile: PhysiqueProfile) {
    val bmi = if (profile.heightCm > 0)
        profile.weightKg / (profile.heightCm / 100f).pow(2) else 0f
    val bmiCat = when {
        bmi < 18.5f -> "UNDERWEIGHT"
        bmi < 25f   -> "OPTIMAL"
        bmi < 30f   -> "ELEVATED"
        else        -> "HIGH"
    }

    val bmrRaw = 10f * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age
    val bmr = (bmrRaw + (if (profile.sex == "male") 5f else -161f)).roundToInt()

    val activityMult = when (profile.activityLevel) {
        "sedentary" -> 1.2f
        "light"     -> 1.375f
        "moderate"  -> 1.55f
        "intense"   -> 1.725f
        else        -> 1.2f
    }
    val tdee = (bmr * activityMult).roundToInt()

    val goalAdj = when (profile.bodyGoal) {
        "cut"      -> -0.18f
        "bulk"     ->  0.15f
        else       ->  0f
    }
    val target = (tdee * (1f + goalAdj)).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // BMI panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .reactStyleCard(selected = true, glowColor = GoldAccent)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "BODY MASS INDEX",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "%.1f".format(bmi),
                    fontFamily = orbitron,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldAccent,
                    style = TextStyle(shadow = Shadow(GoldAccent.copy(alpha = 0.5f), blurRadius = 24f))
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        bmiCat,
                        fontFamily = jetBrainsMono,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                }
            }
        }

        // BMR / TDEE / TARGET row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryTile(label = "BMR",    value = bmr,    color = PurpleLight,   modifier = Modifier.weight(1f))
            SummaryTile(label = "TDEE",   value = tdee,   color = CyanAccent,    modifier = Modifier.weight(1f))
            SummaryTile(label = "TARGET", value = target, color = SuccessGreen,  modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val anim by animateIntAsState(value, tween(900, easing = EaseOutCubic), label = "v")
    Box(
        modifier = modifier
            .reactStyleCard(selected = false, glowColor = Color.Transparent)
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                fontFamily = jetBrainsMono,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                color = TextMuted
            )
            Text(
                anim.toString(),
                fontFamily = orbitron,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = color,
                style = TextStyle(shadow = Shadow(color.copy(alpha = 0.4f), blurRadius = 12f))
            )
            Text(
                "KCAL",
                fontFamily = jetBrainsMono,
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                color = TextMuted.copy(alpha = 0.6f)
            )
        }
    }
}

/* ============================================================
 *  ASCEND SLIDER (custom cyber slider)
 * ============================================================ */
@Composable
private fun AscendSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value,
        valueRange = range,
        steps = steps,
        onValueChange = onValueChange,
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = PanelMid,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                10.dp, RoundedCornerShape(20.dp),
                ambientColor = accentColor, spotColor = accentColor
            )
    )
}

@Composable
private fun SliderLegend(text: String) {
    Text(
        text,
        fontFamily = jetBrainsMono,
        fontSize = 9.5.sp,
        letterSpacing = 1.sp,
        color = TextMuted.copy(alpha = 0.6f)
    )
}

/* ============================================================
 *  BOTTOM BAR
 * ============================================================ */
@Composable
private fun PhysiqueBottomBar(
    isFirstStep: Boolean,
    isFinalStep: Boolean,
    isSaving: Boolean,
    proceedEnabled: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "p"
    )
    val elev = if (isFinalStep) (pulse * 22).dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFirstStep) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, BorderGlow, RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "← BACK",
                    fontFamily = jetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(
                    elev, RoundedCornerShape(10.dp),
                    ambientColor = PurplePrimary, spotColor = CyanAccent
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)))
                .alpha(if (proceedEnabled) 1f else 0.45f)
                .clickable(enabled = proceedEnabled && !isSaving) { onNext() },
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isFinalStep) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isFinalStep) "SAVE PHYSIQUE" else "CONTINUE",
                        fontFamily = orbitron,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/* ============================================================
 *  HELPERS
 * ============================================================ */

fun Modifier.reactStyleCard(selected: Boolean, glowColor: Color, cornerRadius: Dp = 12.dp): Modifier {
    return this
        .alpha(if (selected) 1f else 0.7f)
        .then(
            if (selected) Modifier.shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = glowColor,
                spotColor = glowColor
            ) else Modifier
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(PanelMid)
        .border(
            width = 1.dp,
            color = if (selected) glowColor else BorderGlow.copy(alpha = 0.5f),
            shape = RoundedCornerShape(cornerRadius)
        )
}

fun Modifier.scanlineHorizontal(): Modifier = drawWithCache {
    val spacing = 4f
    onDrawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(
                Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, y + 1.5f),
                end = Offset(size.width, y + 1.5f),
                strokeWidth = 1.5f
            )
            y += spacing
        }
    }
}

/* ============================================================
 *  PREVIEWS
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "1 - Biometric")
@Composable
fun PhysiquePreview_Step0() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male"),
            step = PhysiqueStep.BIOMETRIC_SCAN,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "2 - Age")
@Composable
fun PhysiquePreview_Step1() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male", age = 27),
            step = PhysiqueStep.AGE,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "3 - Body Metrics")
@Composable
fun PhysiquePreview_Step2() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male", age = 27, heightCm = 178f, weightKg = 74f),
            step = PhysiqueStep.BODY_METRICS,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "4 - Activity")
@Composable
fun PhysiquePreview_Step3() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male", age = 27, heightCm = 178f, weightKg = 74f, activityLevel = "moderate"),
            step = PhysiqueStep.ACTIVITY_LEVEL,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "5 - Objective")
@Composable
fun PhysiquePreview_Step4() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male", age = 27, heightCm = 178f, weightKg = 74f, activityLevel = "moderate", bodyGoal = "cut"),
            step = PhysiqueStep.OBJECTIVE,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "6 - Summary")
@Composable
fun PhysiquePreview_Step5() {
    MaterialTheme {
        PhysiqueOnboardingScreenContent(
            profile = PhysiqueProfile(sex = "male", age = 27, heightCm = 178f, weightKg = 74f, activityLevel = "moderate", bodyGoal = "cut"),
            step = PhysiqueStep.SUMMARY,
            isSaving = false, error = null,
            onUpdateProfile = {}, onNext = {}, onBack = {}
        )
    }
}