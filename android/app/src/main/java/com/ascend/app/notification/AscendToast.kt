package com.ascend.app.notification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.ascend.app.ui.theme.*
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.auth.jetBrainsMono

/**
 * Global singleton to fire in-app toasts from anywhere (e.g. repo, viewmodel, fcm foreground)
 */
object AscendToastBus {
    private val _events = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 5)
    val events = _events.asSharedFlow()

    fun show(event: ToastEvent) {
        _events.tryEmit(event)
    }
}

/**
 * Mount this AT THE ROOT of your MainActivity (above your NavHost)
 */
@Composable
fun AscendToastHost() {
    val events = AscendToastBus.events.collectAsStateWithLifecycle(initialValue = null)
    var currentToast by remember { mutableStateOf<ToastEvent?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(events.value) {
        events.value?.let { evt ->
            isVisible = false
            delay(150) // small gap if replacing another toast
            currentToast = evt
            isVisible = true
            delay(evt.durationMs)
            isVisible = false
            delay(300) // wait for exit animation
            if (currentToast == evt) currentToast = null
        }
    }

    currentToast?.let { toast ->
        Dialog(
            onDismissRequest = { isVisible = false },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp), // below status bar
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn(tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(250, easing = FastOutLinearInEasing)
                    ) + fadeOut(tween(250))
                ) {
                    ToastOverlay(toast)
                }
            }
        }
    }
}

@Composable
private fun ToastOverlay(toast: ToastEvent) {
    val accent = toast.type.accentColor

    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(12.dp))
            .background(SystemBlack.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .shadow(16.dp, RoundedCornerShape(12.dp), ambientColor = accent, spotColor = accent)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(toast.type.icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(toast.title.uppercase(),
                    fontFamily = orbitron, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accent)
                Spacer(Modifier.height(2.dp))
                Text(toast.message,
                    fontFamily = jetBrainsMono, fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f), lineHeight = 15.sp)
            }

            // Optional XP Badge
            toast.xpDelta?.let { xp ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.2f))
                        .border(1.dp, GoldAccent.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+$xp XP",
                        fontFamily = jetBrainsMono, fontSize = 10.sp,
                        fontWeight = FontWeight.Black, color = GoldAccent)
                }
            }
        }
    }
}
