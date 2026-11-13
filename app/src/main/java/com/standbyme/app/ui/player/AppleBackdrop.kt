package com.standbyme.app.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

private const val DEGREES_PER_MS = 360f / 90_000f

/**
 * Fond flouté : deux copies de la pochette, agrandies et tournant lentement en sens
 * inverse, sous un flou fort. Sans pochette : dégradé de la couleur principale.
 */
@Composable
fun AppleBackdrop(
    art: Bitmap?,
    isPlaying: Boolean,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
) {
    val safeArt = art?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val targetImage = remember(safeArt) {
        safeArt?.takeIf { !it.isRecycled }?.let { bmp ->
            runCatching { bmp.asImageBitmap() }.getOrNull()
        }
    }

    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameMs = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs ->
                val deltaMs = (frameMs - lastFrameMs).coerceIn(0L, 100L)
                lastFrameMs = frameMs
                angle = (angle + deltaMs * DEGREES_PER_MS) % 360f
            }
        }
    }

    var currentImage by remember { mutableStateOf(targetImage) }
    var previousImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val fadeProgress = remember { Animatable(1f) }

    LaunchedEffect(targetImage) {
        if (targetImage === currentImage) return@LaunchedEffect
        previousImage = currentImage
        currentImage = targetImage
        fadeProgress.snapTo(0f)
        fadeProgress.animateTo(1f, animationSpec = tween(900))
        previousImage = null
    }

    Box(modifier = modifier.background(Color.Black)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .blur(80.dp),
        ) {
            val t = fadeProgress.value.coerceIn(0f, 1f)
            val prev = previousImage
            val curr = currentImage
            val diagonal = sqrt(size.width * size.width + size.height * size.height)

            if (curr == null || (prev == null && t < 1f)) {
                drawRect(Brush.verticalGradient(listOf(fallbackColor.copy(alpha = 0.55f), Color.Black)))
            }

            if (prev != null && t < 1f) {
                val prevAlpha = if (curr == null) 1f - t else 1f
                drawRotatedBackdrop(prev, diagonal, angle, prevAlpha)
            }
            if (curr != null) {
                drawRotatedBackdrop(curr, diagonal, angle, t)
            }

            drawRect(Color.Black.copy(alpha = 0.32f))
        }
    }
}

private fun DrawScope.drawRotatedBackdrop(
    bmp: ImageBitmap,
    diagonal: Float,
    angle: Float,
    alpha: Float,
) {
    if (alpha <= 0f || bmp.width <= 0 || bmp.height <= 0) return
    drawRotatedArt(bmp, diagonal * 1.25f, angle, alpha)
    drawRotatedArt(bmp, diagonal * 1.6f, 140f - angle, 0.5f * alpha)
}

private fun DrawScope.drawRotatedArt(bmp: ImageBitmap, side: Float, degrees: Float, alpha: Float) {
    val s = side.toInt().coerceAtLeast(1)
    rotate(degrees) {
        drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset((center.x - side / 2f).toInt(), (center.y - side / 2f).toInt()),
            dstSize = IntSize(s, s),
            alpha = alpha.coerceIn(0f, 1f),
        )
    }
}
