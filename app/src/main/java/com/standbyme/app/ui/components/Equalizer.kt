package com.standbyme.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.StartOffset

/** Petit égaliseur animé (barres qui montent et descendent en décalé). */
@Composable
fun Equalizer(
    bars: Int,
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 2.5.dp,
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(bars) { i ->
            val scale = transition.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 130),
                ),
                label = "wave$i",
            )
            Box(
                Modifier
                    .width(barWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = scale.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}
