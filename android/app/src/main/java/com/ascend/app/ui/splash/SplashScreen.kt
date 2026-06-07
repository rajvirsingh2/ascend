package com.ascend.app.ui.splash

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.R
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.PanelDark
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToInterests: () -> Unit
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Login -> onNavigateToLogin()
            is SplashDestination.Dashboard -> onNavigateToDashboard()
            is SplashDestination.InterestsOnboarding -> onNavigateToInterests()
            is SplashDestination.PhysiqueSetup -> onNavigateToDashboard()
            null -> Unit
        }
    }
    SplashScreenContent()
}

@Composable
fun SplashScreenContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val haloAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "halo"
    )
    val scaleState = infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    val orbitron= FontFamily(
        Font(R.font.orbitron_black, FontWeight.Normal)
    )
    var dotCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }
    val animatedDots = remember(dotCount) { ".".repeat(dotCount) }

    var progress by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (progress < 100) {
            delay(10)
            progress = (progress + 4).coerceAtMost(100)
        }
    }

    val titleBrush = remember {
        Brush.verticalGradient(
            0f to Color.White,
            0.55f to Color(0xFFC9B8FF),
            1f to CyanAccent
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SystemBlack, PanelDark, SystemBlack)))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(30.dp))
            Box(contentAlignment = Alignment.Center) {
                // Background glowing halo
                Box(
                    modifier = Modifier
                        .size(320.dp) // Increased slightly to encompass the new wider box
                        .graphicsLayer {
                            alpha = haloAlphaState.value
                        }
                        .blur(32.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7C3AED).copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // The new styled box holding the text
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // Moved scale animation to the container
                            scaleX = scaleState.value
                            scaleY = scaleState.value
                        }
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = Color(0xFF7C3AED),
                            spotColor = Color(0xFF7C3AED)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1B1736), // Deep dark purple
                                    Color(0xFF0F0E1E)  // Almost black
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF7C3AED).copy(alpha = 0.3f), // Subtle border glow
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 16.dp), // Padding around the text
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ASCEND",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        style = TextStyle(
                            brush = titleBrush,
                            shadow = Shadow(
                                color = Color(0xFF7C3AED).copy(alpha = 0.8f),
                                blurRadius = 25f
                            )
                        ),
                        fontFamily = orbitron
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier
                    .size(34.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = CyanAccent,
                        spotColor = CyanAccent
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(220.dp)
            ) {
                Text(
                    text = "INITIALIZING SYSTEM$animatedDots",
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1A1A2E))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF7C3AED), CyanAccent)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "v0.1.0",
                        fontSize = 9.5.sp,
                        letterSpacing = 2.sp,
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$progress%",
                        fontSize = 9.5.sp,
                        letterSpacing = 2.sp,
                        color = TextSecondary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreenContent()
    }
}