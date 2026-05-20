package com.ascend.app.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients
import java.io.ByteArrayOutputStream

@Composable
fun AvatarPicker(
    currentUrl: String?,
    username: String,
    isUploading: Boolean,
    onImageSelected: (String) -> Unit   // callback with base64
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(it) ?: return@let
            val original = BitmapFactory.decodeStream(stream)
            stream.close()

            // resize to 512×512 before encoding
            val scaled = Bitmap.createScaledBitmap(original, 512, 512, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            val base64 = "data:image/jpeg;base64," +
                    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)

            onImageSelected(base64)
        }
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(Gradients.ArcaneFlow))
            .border(2.dp, DarkColors.Arcane, CircleShape)
            .clickable { launcher.launch("image/*") },
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(32.dp)
            )
        } else if (!currentUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                text = username.take(2).uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // edit overlay
        if (!isUploading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("EDIT", fontSize = 9.sp, color = Color.White,
                    fontWeight = FontWeight.Medium, letterSpacing = 0.08.sp)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "Avatar Picker States")
@Composable
fun AvatarPickerPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Default State (No Image, shows initials)
            AvatarPicker(
                currentUrl = null,
                username = "Hunter",
                isUploading = false,
                onImageSelected = {}
            )

            // 2. Loading / Uploading State
            AvatarPicker(
                currentUrl = null,
                username = "Hunter",
                isUploading = true,
                onImageSelected = {}
            )

            // 3. Image Exists State
            // Note: Coil's AsyncImage might render blank in preview unless you provide a placeholder,
            // but this tests the layout and the "EDIT" overlay rendering.
            AvatarPicker(
                currentUrl = "https://example.com/dummy.jpg",
                username = "Hunter",
                isUploading = false,
                onImageSelected = {}
            )
        }
    }
}