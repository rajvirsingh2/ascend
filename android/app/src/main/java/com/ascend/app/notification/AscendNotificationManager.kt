package com.ascend.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System-level notifications builder. Call from FCM service or any
 * domain event handler. Auto-creates channels on first use.
 */
@Singleton
class AscendNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nm: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init { ensureChannels() }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val seen = mutableSetOf<String>()
        NotifType.entries.forEach { type ->
            if (type.channelId in seen) return@forEach
            seen.add(type.channelId)
            val importance = when (type) {
                NotifType.LEVEL_UP, NotifType.STREAK_BROKEN -> NotificationManager.IMPORTANCE_HIGH
                NotifType.STREAK_REMINDER, NotifType.DAILY_QUEST -> NotificationManager.IMPORTANCE_DEFAULT
                else -> NotificationManager.IMPORTANCE_LOW
            }
            val channel = NotificationChannel(type.channelId, type.channelName, importance).apply {
                description = type.channelDescription
                enableLights(true)
                lightColor = type.accentColor.toArgb()
                enableVibration(importance >= NotificationManager.IMPORTANCE_DEFAULT)
            }
            nm.createNotificationChannel(channel)
        }
    }

    /** Post a system notification for an event */
    fun post(
        item: NotifItem,
        deepLinkIntent: Intent? = null,
        smallIconRes: Int  // pass R.drawable.ic_notification (or your bolt icon)
    ) {
        val pending = deepLinkIntent?.let {
            PendingIntent.getActivity(
                context, item.id.hashCode(), it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val accent = item.type.accentColor.toArgb()
        val largeIcon = buildLargeIcon(item)

        val builder = NotificationCompat.Builder(context, item.type.channelId)
            .setSmallIcon(smallIconRes)
            .setColor(accent)
            .setColorized(true)
            .setLargeIcon(largeIcon)
            .setContentTitle(item.title)
            .setContentText(item.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(item.title)
                    .bigText(buildBigText(item))
                    .setSummaryText("◈ ${item.type.displayLabel}")
            )
            .setPriority(when (item.type) {
                NotifType.LEVEL_UP, NotifType.STREAK_BROKEN -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            })
            .setCategory(when (item.type) {
                NotifType.STREAK_REMINDER, NotifType.DAILY_QUEST -> NotificationCompat.CATEGORY_REMINDER
                NotifType.FRIEND_ACTIVITY -> NotificationCompat.CATEGORY_SOCIAL
                else -> NotificationCompat.CATEGORY_STATUS
            })
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(item.timestamp)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .apply {
                if (pending != null) setContentIntent(pending)
            }

        nm.notify(item.id.hashCode(), builder.build())
    }

    private fun buildBigText(item: NotifItem): String =
        if (item.xpDelta != null) "${item.body}\n\n+${item.xpDelta} XP"
        else item.body

    /**
     * Build a cyber-styled circular large icon: gradient bg + rank/type letter.
     * Avoids needing a separate asset per type.
     */
    private fun buildLargeIcon(item: NotifItem): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val accent = item.type.accentColor.toArgb()
        val accentLight = lighten(accent, 0.3f)

        // Background circle with gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                accent, accentLight, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // Letter (first char of displayLabel)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val letter = item.type.displayLabel.firstOrNull()?.toString() ?: "◈"
        val yPos = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, size / 2f, yPos, textPaint)
        return bitmap
    }

    private fun lighten(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.argb(a, r, g, b)
    }

    fun cancel(id: String) = nm.cancel(id.hashCode())
    fun cancelAll() = nm.cancelAll()
}

/** Compose Color → ARGB int (avoids dependency import in this file) */
private fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    val a = (alpha * 255f + 0.5f).toInt() shl 24
    val r = (red   * 255f + 0.5f).toInt() shl 16
    val g = (green * 255f + 0.5f).toInt() shl 8
    val b = (blue  * 255f + 0.5f).toInt()
    return a or r or g or b
}
