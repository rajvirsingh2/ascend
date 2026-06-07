package com.ascend.app.ui.attributes

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DangerRed
import com.ascend.app.ui.theme.GoldAccent
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextMuted

@Composable
fun AttributesScreen(
    onBack: () -> Unit,
    viewModel: AttributesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AttributesScreenContent(
        user = state.user,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributesScreenContent(
    user: com.ascend.app.domain.model.User?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ATTRIBUTES",
                        fontFamily = orbitron,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0C0C16)
                )
            )
        },
        containerColor = Color(0xFF0C0C16)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Status
                    Text(
                        "RANK: ${user.rankTitle.uppercase()}",
                        fontFamily = jetBrainsMono,
                        color = CyanAccent,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "LEVEL ${user.level}",
                        fontFamily = orbitron,
                        color = GoldAccent,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(32.dp))

                    // Attribute Bars
                    AttributeBar(label = "STRENGTH", value = user.strength, color = DangerRed)
                    Spacer(Modifier.height(24.dp))
                    AttributeBar(label = "AGILITY", value = user.agility, color = CyanAccent)
                    Spacer(Modifier.height(24.dp))
                    AttributeBar(label = "MANA", value = user.mana, color = PurplePrimary)

                    Spacer(Modifier.weight(1f))
                    
                    Text(
                        "Complete quests to grow your attributes.",
                        fontFamily = jetBrainsMono,
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                CircularProgressIndicator(
                    color = CyanAccent,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun AttributeBar(label: String, value: Int, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val infinitePulse = rememberInfiniteTransition(label = "pulseGlowAnim")
    val translateX by infinitePulse.animateFloat(
        initialValue = -20f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "translate"
    )
    val opacityFloat by infinitePulse.animateFloat(
        initialValue = 0f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "opacity"
    )
    val pulseOpacity = 1f - kotlin.math.abs(1f - opacityFloat)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontFamily = orbitron,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
            Text(
                value.toString(),
                fontFamily = jetBrainsMono,
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1F1F2E))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            // Fill
            val fillFraction = (value / 100f).coerceIn(0.05f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fillFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
                    .shadow(12.dp, spotColor = color.copy(alpha = glowAlpha))
            ) {
                // Sweeping pulse glow
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(20.dp)
                        .absoluteOffset(x = translateX.dp)
                        .alpha(pulseOpacity)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f))
                            )
                        )
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun AttributesScreenPreview() {
    com.ascend.app.ui.theme.AscendTheme {
        AttributesScreenContent(
            user = com.ascend.app.domain.model.User(
                id = "1",
                username = "Sung Jin-Woo",
                email = "hunter@shadow.com",
                level = 15,
                currentXp = 450,
                xpToNext = 1000,
                avatarUrl = null,
                strength = 80,
                agility = 65,
                mana = 40
            ),
            onBack = {}
        )
    }
}
