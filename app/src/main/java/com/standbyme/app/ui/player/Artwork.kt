package com.standbyme.app.ui.player

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.standbyme.app.media.PlaybackUiState

/**
 * Couleur dominante d'une pochette : moyenne sur une miniature 10×10, puis saturation
 * et luminosité recadrées pour donner un halo lumineux mais jamais criard.
 */
fun dominantAuraColor(bitmap: Bitmap): Color {
    val small = Bitmap.createScaledBitmap(bitmap, 10, 10, true)
    val pixels = IntArray(100)
    small.getPixels(pixels, 0, 10, 0, 0, 10, 10)
    if (small !== bitmap) small.recycle()
    var r = 0L
    var g = 0L
    var b = 0L
    for (p in pixels) {
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
    }
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV((r / 100).toInt(), (g / 100).toInt(), (b / 100).toInt(), hsv)
    hsv[1] = (hsv[1] * 1.4f).coerceIn(0.35f, 1f)
    hsv[2] = hsv[2].coerceIn(0.45f, 0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/** Pochette arrondie, avec éventuellement un halo coloré flouté derrière. */
@Composable
fun ArtworkWithAura(
    playback: PlaybackUiState,
    size: Dp,
    auraIntensity: Float,
    auraEnabled: Boolean,
    useAlbumColor: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val art = playback.albumArt
    val albumColor = remember(art) { art?.let(::dominantAuraColor) }
    val aura: Color? = when {
        !auraEnabled -> null
        useAlbumColor -> albumColor ?: colors.primary
        else -> colors.primary
    }
    val strength = auraIntensity.coerceIn(0f, 1f)

    Box(contentAlignment = Alignment.Center) {
        if (aura != null && strength > 0f) {
            Box(
                Modifier
                    .size(size + 30.dp + 90.dp * strength)
                    .blur(18.dp + 46.dp * strength)
                    .background(
                        Brush.radialGradient(listOf(aura.copy(alpha = strength), aura.copy(alpha = 0f))),
                        CircleShape,
                    ),
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Image(
                    bitmap = remember(art) { art.asImageBitmap() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, Modifier.size(56.dp), tint = colors.onPrimaryContainer)
            }
        }
    }
}

/** Assombrit/désature une couleur de pochette pour le fond du design Apple. */
fun backdropTint(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.copy(alpha = 1f).toArgb(), hsv)
    hsv[1] *= 0.85f
    hsv[2] = (hsv[2] * 0.92f).coerceIn(0.12f, 0.78f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
