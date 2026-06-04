package com.ascend.app.ui.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// --- React Mapped Colors ---
val ReactCyan = Color(0xFF00E5FF)
val ReactGold = Color(0xFFFFD700)
val ReactGreen = Color(0xFF00E676)
val ReactRed = Color(0xFFFF3B30)
val ReactPurple = Color(0xFFB388FF)
val ReactInk = Color.White
val ReactInkDim = Color.Gray
val ReactInkFaint = Color(0xFF555555)
val ReactPanel = Color(0xFF0C0C16)
val ReactPanelLine = Color(0xFF2A2A35)

/* ============================================================
 * ROOT
 * ============================================================ */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    StatsScreenContent(
        isLoading = state.isLoading,
        error = state.error,
        level = state.level,
        totalXp = state.totalXp,
        totalQuests = state.totalQuests,
        bestStreak = state.bestStreak,
        heatmapFloats = state.heatmapFloats,
        xpHistory = state.xpHistory,
        totalXpLast30Days = state.totalXpLast30Days,
        questDistribution = state.questDistribution,
        questsThisWeek = state.questsThisWeek,
        questsSkipped = state.questsSkipped,
        onTimePercentage = state.onTimePercentage
    )
}

@Composable
fun StatsScreenContent(
    isLoading: Boolean,
    error: String?,
    level: Int,
    totalXp: Int,
    totalQuests: Int,
    bestStreak: Int,
    heatmapFloats: List<Float>,
    xpHistory: List<Float>,
    totalXpLast30Days: Int,
    questDistribution: List<Triple<String, Int, Color>>,
    questsThisWeek: Int,
    questsSkipped: Int,
    onTimePercentage: Float
) {
    Scaffold(containerColor = Color(0xFF07070B)) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .padding(padding)
        ) {
            when {
                isLoading -> CenteredLoading()
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error, fontFamily = jetBrainsMono, fontSize = 12.sp, color = ReactRed)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp)
                ) {
                    item { SectionHead("HUNTER ANALYTICS") }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 18.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile(
                                    label = "TOTAL XP", value = totalXp, color = ReactGold,
                                    icon = Icons.Filled.Bolt, modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "LEVEL", value = level, color = ReactPurple,
                                    icon = Icons.Filled.ArrowUpward, modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile(
                                    label = "BEST STREAK", value = bestStreak, color = ReactCyan,
                                    icon = Icons.Filled.LocalFireDepartment, modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "QUESTS DONE", value = totalQuests, color = ReactGreen,
                                    icon = Icons.Filled.Check, modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item { SectionHead("XP GAINED · LAST 30 DAYS") }
                    item { XpLineChartPanel(xpHistory = xpHistory, totalXp30d = totalXpLast30Days) }

                    item { SectionHead("QUEST DISTRIBUTION") }
                    item { QuestDistributionPanel(dist = questDistribution) }

                    item { SectionHead("ACTIVITY CALENDAR") }
                    item { ActivityHeatmapPanel(heat = heatmapFloats) }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(bottom = 18.dp)
                        ) {
                            CompletionRingPanel(pct = onTimePercentage.roundToInt(), modifier = Modifier.weight(1f))
                            KpiListPanel(
                                questsThisWeek = questsThisWeek,
                                questsSkipped = questsSkipped,
                                onTimePercentage = onTimePercentage.roundToInt(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item { SectionHead("RANK JOURNEY") }
                    val rIndex = when {
                        level >= 50 -> 5
                        level >= 40 -> 4
                        level >= 30 -> 3
                        level >= 20 -> 2
                        level >= 10 -> 1
                        else -> 0
                    }
                    item { RankJourneyPanel(achievedRankIndex = rIndex) }
                }
            }
        }
    }
}

/* ============================================================
 * ROLLING DIGIT ODOMETER (Restricted to MetricTiles)
 * ============================================================ */
@Composable
fun RollingDigitCounter(
    count: Int,
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    var animatedTarget by remember { mutableIntStateOf(if (isPreview) count else 0) }
    LaunchedEffect(count) { animatedTarget = count }

    val countString = if (animatedTarget >= 1000) "%,d".format(animatedTarget) else animatedTarget.toString()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        countString.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 80f)) { it } + fadeIn(tween(300))) togetherWith
                            (slideOutVertically(spring(dampingRatio = 0.7f, stiffness = 80f)) { -it } + fadeOut(tween(300)))
                },
                label = "RollingDigit_$index"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = textStyle,
                    color = color
                )
            }
        }
    }
}

/* ============================================================
 * UI COMPONENTS
 * ============================================================ */
@Composable
fun SectionHead(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(ReactCyan))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontFamily = orbitron,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.5.sp,
            color = ReactInk
        )
    }
}

@Composable
fun ReactPanel(
    glowColor: Color? = null,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (glowColor != null) Modifier.shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(8.dp),
                    ambientColor = glowColor, spotColor = glowColor
                ) else Modifier
            )
            .clip(RoundedCornerShape(8.dp))
            .background(ReactPanel)
            .border(
                width = 1.dp,
                color = glowColor ?: ReactPanelLine,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(padding)
    ) {
        Column(content = content)
    }
}

@Composable
fun MetricTile(
    label: String,
    value: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ReactPanel(glowColor = color, modifier = modifier, padding = PaddingValues(15.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                label,
                fontFamily = jetBrainsMono,
                fontSize = 9.5.sp,
                letterSpacing = 1.5.sp,
                color = ReactInkDim
            )
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))

        RollingDigitCounter(
            count = value,
            color = color,
            textStyle = TextStyle(
                fontFamily = orbitron,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFeatureSettings = "tnum",
                shadow = Shadow(color.copy(alpha = 0.4f), blurRadius = 16f)
            )
        )
    }
}

@Composable
fun XpLineChartPanel(xpHistory: List<Float>, totalXp30d: Int) {
    val vals = if (xpHistory.size >= 2) xpHistory else listOf(0f, 0f)

    ReactPanel(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        padding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            XpLineCanvas(vals = vals)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("30d AGO", fontFamily = jetBrainsMono, fontSize = 9.sp, color = ReactInkFaint)
            Text("+$totalXp30d XP", fontFamily = jetBrainsMono, fontSize = 9.sp, color = ReactCyan)
            Text("TODAY", fontFamily = jetBrainsMono, fontSize = 9.sp, color = ReactInkFaint)
        }
    }
}

@Composable
private fun XpLineCanvas(vals: List<Float>) {
    val isPreview = LocalInspectionMode.current
    var trigger by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) { trigger = true }

    val drawProgress by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f),
        label = "chartAnim"
    )

    val maxV = (vals.maxOrNull() ?: 1f) * 1.1f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val n = vals.size
        if (n < 2) return@Canvas

        val pts = vals.mapIndexed { i, v ->
            Offset((i / (n - 1).toFloat()) * w, h - ((v * drawProgress) / maxV) * h)
        }

        val areaPath = Path().apply {
            moveTo(0f, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(w, h)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(ReactPurple.copy(alpha = 0.45f), ReactCyan.copy(alpha = 0f))
            )
        )

        val linePath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(ReactPurple, ReactCyan)),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val last = pts.last()
        drawCircle(color = ReactCyan.copy(alpha = 0.4f * drawProgress), radius = 6f, center = last)
        drawCircle(color = ReactCyan.copy(alpha = drawProgress), radius = 3.5f, center = last)
    }
}

@Composable
fun QuestDistributionPanel(dist: List<Triple<String, Int, Color>>) {
    val maxV = dist.maxOfOrNull { it.second }?.toFloat()?.coerceAtLeast(1f) ?: 1f

    ReactPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            dist.forEach { (name, v, col) ->
                val isPreview = LocalInspectionMode.current
                var trigger by remember { mutableStateOf(isPreview) }
                LaunchedEffect(Unit) { trigger = true }

                val animWidth by animateFloatAsState(
                    targetValue = if (trigger) v / maxV else 0f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 50f),
                    label = "distW_$name"
                )

                val animNum by animateIntAsState(
                    targetValue = if (trigger) v else 0,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 50f),
                    label = "distN_$name"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${categoryEmoji(name)} $name",
                        fontFamily = jetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = col,
                        modifier = Modifier.width(80.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(4.5.dp))
                            .background(ReactPanelLine)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animWidth)
                                .background(col, RoundedCornerShape(4.5.dp))
                                .shadow(8.dp, RoundedCornerShape(4.5.dp), ambientColor = col, spotColor = col)
                        )
                    }

                    Text(
                        text = animNum.toString(),
                        fontFamily = jetBrainsMono,
                        fontSize = 12.sp,
                        color = ReactInkDim,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityHeatmapPanel(heat: List<Float>) {
    val isPreview = LocalInspectionMode.current
    var trigger by remember { mutableStateOf(isPreview) }
    LaunchedEffect(Unit) { trigger = true }

    val alphaState by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 30f),
        label = "heatmapFade"
    )

    ReactPanel(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Column(modifier = Modifier.alpha(alphaState)) {
            heat.chunked(10).forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    row.forEachIndexed { colIdx, v ->
                        val i = rowIdx * 10 + colIdx
                        val today = i == 69
                        val bg = when {
                            today      -> ReactPurple
                            v > 0.66f  -> ReactGold
                            v > 0.40f  -> blendColors(ReactGold, ReactPanel, 0.55f)
                            v > 0.18f  -> blendColors(ReactGold, ReactPanel, 0.25f)
                            else       -> Color(0xFF13131F)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(
                                    if (today) Modifier.shadow(8.dp, RoundedCornerShape(3.dp), ambientColor = ReactPurple, spotColor = ReactPurple)
                                    else Modifier
                                )
                                .clip(RoundedCornerShape(3.dp))
                                .background(bg)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LESS", fontFamily = jetBrainsMono, fontSize = 9.sp, color = ReactInkFaint)
                listOf(
                    Color(0xFF13131F),
                    blendColors(ReactGold, ReactPanel, 0.25f),
                    blendColors(ReactGold, ReactPanel, 0.55f),
                    ReactGold
                ).forEach {
                    Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(it))
                }
                Text("MORE", fontFamily = jetBrainsMono, fontSize = 9.sp, color = ReactInkFaint)
            }
        }
    }
}

@Composable
fun CompletionRingPanel(pct: Int, modifier: Modifier = Modifier) {
    ReactPanel(
        modifier = modifier.fillMaxHeight(),
        padding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "COMPLETION",
                fontFamily = jetBrainsMono,
                fontSize = 9.5.sp,
                letterSpacing = 1.5.sp,
                color = ReactInkDim
            )
            Spacer(Modifier.height(12.dp))
            CompletionRing(pct = pct)
        }
    }
}

@Composable
private fun CompletionRing(pct: Int, size: androidx.compose.ui.unit.Dp = 88.dp) {
    val isPreview = LocalInspectionMode.current
    var trigger by remember { mutableStateOf(isPreview) }
    LaunchedEffect(pct) { trigger = true }

    val animPct by animateFloatAsState(
        targetValue = if (trigger) pct / 100f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 50f),
        label = "ringArc"
    )

    val animText by animateIntAsState(
        targetValue = if (trigger) pct else 0,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 50f),
        label = "ringText"
    )

    val color = ReactGreen

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8f
            val diameter = this.size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = ReactPanel,
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * animPct,
                useCenter = false, topLeft = topLeft, size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Text(
            text = "$animText%",
            fontFamily = orbitron,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color,
            style = TextStyle(shadow = Shadow(color.copy(alpha = 0.5f), blurRadius = 5f))
        )
    }
}

@Composable
fun KpiListPanel(questsThisWeek: Int, questsSkipped: Int, onTimePercentage: Int, modifier: Modifier = Modifier) {
    val rows = listOf(
        Triple("THIS WEEK", "$questsThisWeek", ReactCyan),
        Triple("SKIPPED", "$questsSkipped", ReactRed),
        Triple("ON TIME", "$onTimePercentage%", ReactGreen)
    )
    ReactPanel(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            rows.forEachIndexed { i, (l, v, c) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(l, fontFamily = jetBrainsMono, fontSize = 10.sp, color = ReactInkDim)
                    Text(v, fontFamily = orbitron, fontSize = 16.sp, color = c)
                }
                if (i < rows.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun RankJourneyPanel(achievedRankIndex: Int) {
    val ranks = listOf("E", "D", "C", "B", "A", "S")
    ReactPanel(padding = PaddingValues(horizontal = 14.dp, vertical = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ranks.forEachIndexed { i, r ->
                val achieved = i <= achievedRankIndex
                val isCurrent = i == achievedRankIndex
                val col = rankColor(r)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .then(
                                if (isCurrent) Modifier.shadow(
                                    14.dp, RoundedCornerShape(9.dp),
                                    ambientColor = col, spotColor = col
                                ) else Modifier
                            )
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (achieved) blendColors(col, Color.Transparent, 0.16f) else Color.Transparent
                            )
                            .border(
                                1.5.dp,
                                if (achieved) col else ReactPanelLine,
                                RoundedCornerShape(9.dp)
                            )
                            .alpha(if (achieved) 1f else 0.5f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            r,
                            fontFamily = orbitron,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (achieved) col else ReactInkFaint
                        )
                    }
                    if (isCurrent) {
                        Text(
                            "YOU",
                            fontFamily = jetBrainsMono,
                            fontSize = 8.sp,
                            color = rankColor("C")
                        )
                    } else {
                        Box(Modifier.height(11.dp))
                    }
                }

                if (i < ranks.size - 1) {
                    val isPreview = LocalInspectionMode.current
                    var trigger by remember { mutableStateOf(isPreview) }
                    LaunchedEffect(achievedRankIndex) { trigger = true }

                    val animWidth by animateFloatAsState(
                        targetValue = if (trigger && i < achievedRankIndex) 1f else 0f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 50f),
                        label = "rankLine"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 2.dp)
                            .offset(y = (-8).dp)
                            .background(ReactPanelLine)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animWidth)
                                .background(Brush.horizontalGradient(listOf(ReactPurple, ReactCyan)))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ReactPurple, strokeWidth = 2.dp)
    }
}

/* ============================================================
 * HELPERS
 * ============================================================ */
fun pseudoSeq(n: Int, seed: Int): List<Float> {
    val out = mutableListOf<Float>()
    var s = seed.toLong()
    for (i in 0 until n) {
        s = (s * 9301 + 49297) % 233280
        out.add(s.toFloat() / 233280f)
    }
    return out
}

fun blendColors(c1: Color, c2: Color, weight: Float): Color {
    val w = weight.coerceIn(0f, 1f)
    return Color(
        red   = c1.red * w + c2.red * (1 - w),
        green = c1.green * w + c2.green * (1 - w),
        blue  = c1.blue * w + c2.blue * (1 - w),
        alpha = c1.alpha * w + c2.alpha * (1 - w)
    )
}

fun categoryEmoji(name: String): String = when (name.lowercase(Locale.ROOT)) {
    "tech"     -> "💻"
    "physical" -> "⚔"
    "mental"   -> "🧠"
    "social"   -> "🗣"
    "finance"  -> "📈"
    else       -> "◈"
}

fun rankColor(rank: String): Color = when (rank) {
    "E" -> Color(0xFF8B9DA8)
    "D" -> Color(0xFF7DB0E8)
    "C" -> ReactCyan
    "B" -> Color(0xFF8B5CF6)
    "A" -> ReactPurple
    "S" -> ReactGold
    else -> ReactCyan
}

fun formatBigNum(n: Int): String =
    if (n >= 1000) "%,d".format(n) else n.toString()

/* ============================================================
 * PREVIEWS
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Stats Dashboard")
@Composable
fun StatsScreenPreview() {
    MaterialTheme {
        StatsScreenContent(
            isLoading = false, error = null,
            level = 23, totalXp = 15400, totalQuests = 147,
            bestStreak = 28,
            heatmapFloats = pseudoSeq(70, 219),
            xpHistory = listOf(10f, 25f, 15f, 40f, 30f, 60f, 45f),
            totalXpLast30Days = 2450,
            questDistribution = listOf(
                Triple("Health", 45, Color(0xFFE91E63)),
                Triple("Mind", 30, Color(0xFF2196F3)),
                Triple("Wealth", 25, Color(0xFFFFC107))
            ),
            questsThisWeek = 12,
            questsSkipped = 2,
            onTimePercentage = 85.5f
        )
    }
}