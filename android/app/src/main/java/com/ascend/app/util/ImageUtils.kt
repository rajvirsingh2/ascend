package com.ascend.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Scale down if image is too large (max dimension 800)
            val maxDim = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val bitmapToCompress = if (width > maxDim || height > maxDim) {
                val ratio = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * ratio).toInt(),
                    (height * ratio).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            // Compress as JPEG at 80% quality
            bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val bytes = outputStream.toByteArray()

            // Construct the Data URI format expected by the backend and Cloudinary
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
