package com.ascend.app.ui.profile.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.ascend.app.domain.model.User
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.components.formatNum
import com.ascend.app.ui.components.rankColor
import com.ascend.app.ui.components.rankForLevel
import com.ascend.app.ui.components.scanlineHorizontal
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.GoldAccent
import com.ascend.app.ui.theme.PanelDark
import com.ascend.app.ui.theme.PanelMid
import com.ascend.app.ui.theme.PurpleLight
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.SuccessGreen
import com.ascend.app.ui.theme.SystemBlack
import com.ascend.app.ui.theme.TextMuted
import com.ascend.app.ui.theme.TextPrimary
import com.ascend.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/* ============================================================
 *  SHARE CARD SHEET — full bottom sheet with preview + actions
 * ============================================================ */
@Composable
fun ShareCardSheet(
    user: User,
    completedQuests: Int = 0,
    streak: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    var isExporting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05050A).copy(alpha = 0.82f))
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(PanelDark)
                        .border(
                            1.dp,
                            PurplePrimary.copy(alpha = 0.5f),
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                        )
                        .shadow(
                            24.dp,
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            ambientColor = PurplePrimary,
                            spotColor = CyanAccent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* eat clicks */ }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── HEADER ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "▸ SHARE HUNTER CARD",
                            fontFamily = jetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = CyanAccent
                        )
                        Icon(
                            Icons.Filled.Close, null,
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDismiss() }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── DRAG HANDLE (visual) ──
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BorderGlow)
                    )

                    Spacer(Modifier.height(18.dp))

                    // ── CARD PREVIEW (captured via GraphicsLayer) ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                graphicsLayer.record { this@drawWithContent.drawContent() }
                                drawLayer(graphicsLayer)
                            }
                    ) {
                        HunterShareCard(
                            user = user,
                            completedQuests = completedQuests,
                            streak = streak
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── STATUS MESSAGE ──
                    statusMessage?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanAccent.copy(alpha = 0.10f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.CheckCircle, null,
                                    tint = CyanAccent, modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    it,
                                    fontFamily = jetBrainsMono,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = CyanAccent
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── ACTION GRID 2×2 ──
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShareActionButton(
                                icon = Icons.Filled.Image,
                                label = "SHARE IMAGE",
                                accent = PurpleLight,
                                primary = true,
                                isLoading = isExporting,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        isExporting = true
                                        statusMessage = null
                                        try {
                                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                            val uri = saveBitmapToCache(context, bitmap, "hunter_card.png")
                                            shareImage(context, uri, user)
                                        } catch (e: Exception) {
                                            statusMessage = "Failed: ${e.message}"
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
                            )
                            ShareActionButton(
                                icon = Icons.Filled.Share,
                                label = "SHARE TEXT",
                                accent = CyanAccent,
                                primary = false,
                                modifier = Modifier.weight(1f),
                                onClick = { shareText(context, user) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShareActionButton(
                                icon = Icons.Filled.ContentCopy,
                                label = "COPY",
                                accent = TextSecondary,
                                primary = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    copyToClipboard(context, buildShareText(user))
                                    statusMessage = "COPIED TO CLIPBOARD"
                                }
                            )
                            ShareActionButton(
                                icon = Icons.Filled.Download,
                                label = "SAVE",
                                accent = SuccessGreen,
                                primary = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    scope.launch {
                                        try {
                                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                            saveBitmapToGallery(context, bitmap)
                                            statusMessage = "SAVED TO GALLERY"
                                        } catch (e: Exception) {
                                            statusMessage = "Failed: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/* ============================================================
 *  HUNTER SHARE CARD — the visual being captured
 * ============================================================ */
@Composable
fun HunterShareCard(
    user: User,
    completedQuests: Int,
    streak: Int
) {
    val rank = rankForLevel(user.level)
    val rankCol = rankColor(rank)
    val xpFraction = (user.currentXp.toFloat() / user.xpToNext.coerceAtLeast(1))
        .coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E0E1A),
                        Color(0xFF1A0A2E)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(listOf(PurplePrimary, CyanAccent)),
                RoundedCornerShape(16.dp)
            )
            .scanlineHorizontal()
            .padding(20.dp)
    ) {
        // Radial halo behind everything
        Box(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.20f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PurplePrimary.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(150f, 80f),
                        radius = 320f
                    )
                )
                .blur(40.dp)
        )

        Column {
            // ── TOP: avatar + name + rank ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with rank badge overlay
                Box(modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PurplePrimary, CyanAccent))
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .shadow(14.dp, CircleShape,
                                ambientColor = PurplePrimary, spotColor = CyanAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.avatarUrl != null) {
                            coil.compose.AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                user.username.firstOrNull()?.uppercase() ?: "?",
                                fontFamily = orbitron,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                style = TextStyle(
                                    shadow = Shadow(Color.White.copy(alpha = 0.4f), blurRadius = 12f)
                                )
                            )
                        }
                    }
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(SystemBlack)
                            .border(1.5.dp, rankCol, RoundedCornerShape(7.dp))
                            .shadow(10.dp, RoundedCornerShape(7.dp),
                                ambientColor = rankCol, spotColor = rankCol),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            rank,
                            fontFamily = orbitron,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = rankCol
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        user.username.uppercase(Locale.ROOT),
                        fontFamily = orbitron,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = TextPrimary,
                        style = TextStyle(
                            shadow = Shadow(PurpleLight.copy(alpha = 0.4f), blurRadius = 10f)
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$rank-RANK · AWAKENED",
                        fontFamily = jetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = rankCol
                    )
                }
                // LEVEL badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "LVL",
                        fontFamily = jetBrainsMono,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )
                    Text(
                        user.level.toString(),
                        fontFamily = orbitron,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = CyanAccent,
                        style = TextStyle(
                            shadow = Shadow(CyanAccent.copy(alpha = 0.6f), blurRadius = 14f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── XP BAR ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "EXPERIENCE",
                    fontFamily = jetBrainsMono,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    color = PurpleLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatNum(user.currentXp)} / ${formatNum(user.xpToNext)}",
                    fontFamily = jetBrainsMono,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
            Spacer(Modifier.height(6.dp))
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
                        .fillMaxWidth(xpFraction)
                        .background(
                            Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── STATS ROW (3 columns) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardStat(
                    label = "TOTAL XP",
                    value = formatNum(if (user.totalXp > 0) user.totalXp else user.currentXp),
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
                CardStat(
                    label = "QUESTS",
                    value = completedQuests.toString(),
                    color = PurpleLight,
                    modifier = Modifier.weight(1f)
                )
                CardStat(
                    label = "STREAK",
                    value = "${streak}d",
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            // ── FOOTER (ASCEND brand) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Bolt, null,
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "ASCEND",
                        fontFamily = orbitron,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = TextPrimary
                    )
                }
                Text(
                    "LEVEL UP IRL",
                    fontFamily = jetBrainsMono,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun CardStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0C16))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                label,
                fontFamily = jetBrainsMono,
                fontSize = 8.5.sp,
                letterSpacing = 1.sp,
                color = TextMuted
            )
            Text(
                value,
                fontFamily = orbitron,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

/* ============================================================
 *  ACTION BUTTON
 * ============================================================ */
@Composable
private fun ShareActionButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    primary: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (primary) Modifier
                    .background(Brush.horizontalGradient(listOf(PurplePrimary, CyanAccent)))
                    .shadow(12.dp, RoundedCornerShape(10.dp),
                        ambientColor = PurplePrimary, spotColor = CyanAccent)
                else Modifier
                    .background(PanelMid.copy(alpha = 0.7f))
                    .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            )
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, null,
                    tint = if (primary) Color.White else accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    fontFamily = orbitron,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (primary) Color.White else accent
                )
            }
        }
    }
}

/* ============================================================
 *  SHARE / EXPORT HELPERS
 * ============================================================ */

/** Save bitmap to app cache dir, return shareable URI via FileProvider */
private suspend fun saveBitmapToCache(
    context: Context,
    bitmap: Bitmap,
    filename: String
): Uri = withContext(Dispatchers.IO) {
    val cachePath = File(context.cacheDir, "shared_cards").apply { mkdirs() }
    val file = File(cachePath, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/** Fire chooser with image attached */
private fun shareImage(context: Context, imageUri: Uri, user: User) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(
            Intent.EXTRA_TEXT,
            buildShareText(user)
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Hunter Card"))
}

/** Plain text share fallback */
private fun shareText(context: Context, user: User) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, buildShareText(user))
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Rank"))
}

/** Copy to clipboard */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Hunter Rank", text)
    clipboard.setPrimaryClip(clip)
}

/** Save bitmap to MediaStore (gallery) — Android 10+ scoped storage friendly */
private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) =
    withContext(Dispatchers.IO) {
        val filename = "ascend_card_${System.currentTimeMillis()}.png"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_PICTURES}/Ascend"
                )
            }
            val uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("Cannot create MediaStore entry")
            context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } else {
            // Legacy: needs WRITE_EXTERNAL_STORAGE permission
            val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_PICTURES
            )
            val ascendDir = File(picturesDir, "Ascend").apply { mkdirs() }
            val file = File(ascendDir, filename)
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

private fun buildShareText(user: User): String {
    val rank = rankForLevel(user.level)
    return "I'm a $rank-Rank Hunter at Level ${user.level} on Ascend ⚡ " +
            "Join me in leveling up IRL: https://ascend.app"
}

/* ============================================================
 *  PREVIEW
 * ============================================================ */
@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Share Card Sheet")
@Composable
fun ShareCardSheetPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(SystemBlack)) {
            ShareCardSheet(
                user = User(
                    id = "u1", email = "k@a.app",
                    username = "Kairo",
                    level = 23,
                    currentXp = 680, xpToNext = 1200,
                    avatarUrl = null, totalXp = 4250,
                    hp = 85, maxHp = 100
                ),
                completedQuests = 142,
                streak = 12,
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07070B, name = "Card Only")
@Composable
fun HunterShareCardPreview() {
    MaterialTheme {
        Box(Modifier.fillMaxSize().background(SystemBlack).padding(20.dp)) {
            HunterShareCard(
                user = User(
                    id = "u1", email = "k@a.app",
                    username = "Kairo",
                    level = 23,
                    currentXp = 680, xpToNext = 1200,
                    avatarUrl = null, totalXp = 4250,
                    hp = 85, maxHp = 100
                ),
                completedQuests = 142,
                streak = 12
            )
        }
    }
}
