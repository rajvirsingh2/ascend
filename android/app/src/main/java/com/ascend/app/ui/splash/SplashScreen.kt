package com.ascend.app.ui.splash

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.PanelDark
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextMuted
import com.ascend.app.ui.theme.TextPrimary
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
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    var dotCount by remember{ mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while(true){
            delay(500)
            dotCount=(dotCount+1)%4
        }
    }
    val animatedDots=".".repeat(dotCount)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SystemBlack, PanelDark, SystemBlack))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ASCEND",
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
                color = TextPrimary,
                modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "LEVEL UP IN REAL LIFE",
                fontSize = 11.sp,
                letterSpacing = 5.sp,
                color = CyanAccent.copy(alpha = glowAlpha),
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(48.dp))
            Text(
                text = "INITIALIZING SYSTEM$animatedDots",
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = TextMuted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, name = "Splash Screen Preview")
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreenContent()
    }
}