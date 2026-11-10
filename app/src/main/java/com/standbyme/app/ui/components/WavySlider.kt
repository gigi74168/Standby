package com.standbyme.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Barre de progression / curseur. La partie déjà écoulée ondule quand [wavy] est vrai
 * (amplitude [amplitude]), façon lecteur multimédia d'Android 13+.
 *
 * [onValueChange] est appelé à la fin du glissé ou sur un appui (valeur entre 0 et 1).
 */
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    wavy: Boolean = false,
    amplitude: Dp = 2.dp,
    trackColor: Color,
    activeColor: Color,
    thumbColor: Color = activeColor,
    commitOnRelease: Boolean = true,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // Glissement doux vers la nouvelle valeur quand on ne touche pas le curseur.
    val glided by animateFloatAsState(value.coerceIn(0f, 1f), tween(1000), label = "scrubberGlide")
    val shown = if (dragging) dragValue else glided

    val animatedAmplitude by animateDpAsState(
        targetValue = if (wavy && !dragging) amplitude else 0.dp,
        animationSpec = tween(400),
        label = "waveAmplitude",
    )
    val phase by rememberInfiniteTransition(label = "wavePhase").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "phase",
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    onValueChange((pos.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(commitOnRelease) {
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        dragging = true
                        dragValue = (pos.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging = false
                        if (commitOnRelease) onValueChange(dragValue)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        if (!commitOnRelease) onValueChange(dragValue)
                    },
                )
            },
    ) {
        if (size.width <= 0f) return@Canvas
        val stroke = 6.dp.toPx()
        val centerY = size.height / 2f
        val split = size.width * shown

        // Piste restante (droite).
        drawLine(
            color = trackColor,
            start = Offset(split, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        // Partie écoulée (gauche), ondulée.
        val amp = animatedAmplitude.toPx()
        val wavelength = 42.dp.toPx()
        clipRect(right = split) {
            if (amp <= 0.01f) {
                drawLine(activeColor, Offset(0f, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
            } else {
                val path = Path()
                var x = 0f
                while (x < size.width) {
                    val y = sin(phase + x / wavelength * 2f * PI.toFloat()) * amp + centerY
                    if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                    x += 4f
                }
                drawPath(path, activeColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }

        // Curseur.
        drawCircle(thumbColor, radius = if (dragging) 9.dp.toPx() else 7.dp.toPx(), center = Offset(split, centerY))
    }
}
