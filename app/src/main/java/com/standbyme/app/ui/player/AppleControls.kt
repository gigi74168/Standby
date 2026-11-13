package com.standbyme.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.standbyme.app.R

private val AppleControlWhite = Color(0xFFF0F0ED)

/**
 * Custom SF-Symbols-inspired plump play triangle with generously rounded corners,
 * slightly optically shifted to the right for visual centering inside circular buttons.
 */
@Composable
fun ApplePlayIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val triH = size.height * 0.76f
        val triW = triH * 0.866f
        val left = (size.width - triW) * 0.5f + size.width * 0.055f
        val right = left + triW
        val top = (size.height - triH) * 0.5f
        val bottom = top + triH
        val midY = size.height * 0.5f

        val path = buildRoundedTrianglePath(
            p0 = Offset(left, top),
            p1 = Offset(right, midY),
            p2 = Offset(left, bottom),
            cornerFraction = 0.25f,
        )
        drawPath(path = path, color = tint)
    }
}

/**
 * Custom SF-Symbols-inspired pause glyph made of two thick vertical capsule bars.
 */
@Composable
fun ApplePauseIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width * 0.24f
        val barHeight = size.height * 0.72f
        val gap = size.width * 0.16f
        val totalWidth = barWidth * 2f + gap
        val startX = (size.width - totalWidth) * 0.5f
        val topY = (size.height - barHeight) * 0.5f
        val corner = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)
        val barSize = Size(barWidth, barHeight)

        drawRoundRect(
            color = tint,
            topLeft = Offset(startX, topY),
            size = barSize,
            cornerRadius = corner,
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(startX + barWidth + gap, topY),
            size = barSize,
            cornerRadius = corner,
        )
    }
}

/**
 * Custom SF-Symbols-inspired backward glyph (`backward.fill`):
 * two overlapping generously rounded triangles pointing left.
 */
@Composable
fun AppleBackwardIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    AppleDoubleTriangleIcon(
        forward = false,
        tint = tint,
        modifier = modifier,
    )
}

/**
 * Custom SF-Symbols-inspired forward glyph (`forward.fill`):
 * two overlapping generously rounded triangles pointing right.
 */
@Composable
fun AppleForwardIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    AppleDoubleTriangleIcon(
        forward = true,
        tint = tint,
        modifier = modifier,
    )
}

@Composable
private fun AppleDoubleTriangleIcon(
    forward: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val baseH = minOf(size.height * 0.62f, size.width * 0.65f)
        val baseW = baseH * 0.84f
        val baseStep = baseW * 0.82f
        val rawTotalW = baseW + baseStep
        val fitScale = minOf(1f, (size.width * 0.90f) / rawTotalW)

        val triH = baseH * fitScale
        val triW = baseW * fitScale
        val step = baseStep * fitScale
        val totalW = triW + step

        val startX = (size.width - totalW) * 0.5f
        val topY = (size.height - triH) * 0.5f
        val bottomY = topY + triH
        val midY = size.height * 0.5f

        val first: Path
        val second: Path
        if (forward) {
            first = buildRoundedTrianglePath(
                p0 = Offset(startX, topY),
                p1 = Offset(startX + triW, midY),
                p2 = Offset(startX, bottomY),
                cornerFraction = 0.25f,
            )
            second = buildRoundedTrianglePath(
                p0 = Offset(startX + step, topY),
                p1 = Offset(startX + step + triW, midY),
                p2 = Offset(startX + step, bottomY),
                cornerFraction = 0.25f,
            )
        } else {
            first = buildRoundedTrianglePath(
                p0 = Offset(startX, midY),
                p1 = Offset(startX + triW, topY),
                p2 = Offset(startX + triW, bottomY),
                cornerFraction = 0.25f,
            )
            second = buildRoundedTrianglePath(
                p0 = Offset(startX + step, midY),
                p1 = Offset(startX + step + triW, topY),
                p2 = Offset(startX + step + triW, bottomY),
                cornerFraction = 0.25f,
            )
        }

        val combined = Path().apply {
            op(first, second, PathOperation.Union)
        }
        drawPath(path = combined, color = tint)
    }
}

private fun buildRoundedTrianglePath(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    cornerFraction: Float,
): Path {
    val vertices = arrayOf(p0, p1, p2)
    return Path().apply {
        for (i in 0 until 3) {
            val prev = vertices[(i + 2) % 3]
            val curr = vertices[i]
            val next = vertices[(i + 1) % 3]

            val toPrev = prev - curr
            val toNext = next - curr
            val lenPrev = toPrev.getDistance()
            val lenNext = toNext.getDistance()
            if (lenPrev == 0f || lenNext == 0f) return Path()

            val setback = minOf(lenPrev, lenNext) * cornerFraction
            val start = curr + (toPrev / lenPrev) * setback
            val end = curr + (toNext / lenNext) * setback

            if (i == 0) {
                moveTo(start.x, start.y)
            } else {
                lineTo(start.x, start.y)
            }
            quadraticTo(curr.x, curr.y, end.x, end.y)
        }
        close()
    }
}

/**
 * Circular or pill translucent Apple glassmorphic button with subtle specular border,
 * spring press scale, and gentle highlight.
 */
@Composable
fun AppleGlassButton(
    size: Dp,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = size / 2,
    baseAlpha: Float = 0.14f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "appleGlassScale",
    )
    val pressHighlight by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "appleGlassHighlight",
    )
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(size)
            .clip(shape)
            .background(Color.White.copy(alpha = (baseAlpha + pressHighlight * 0.16f).coerceIn(0f, 1f)))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = (0.18f + pressHighlight * 0.10f).coerceIn(0f, 1f)),
                shape = shape,
            )
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Compact Apple glassmorphic Play/Pause button that smoothly morphs and cross-scales
 * between [ApplePauseIcon] and [ApplePlayIcon].
 */
@Composable
fun ApplePlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 74.dp,
    iconSize: Dp = 36.dp,
) {
    val morph by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "applePlayPauseMorph",
    )
    val label = stringResource(if (isPlaying) R.string.pause else R.string.play)

    AppleGlassButton(
        size = buttonSize,
        contentDescription = label,
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ApplePauseIcon(
                tint = AppleControlWhite,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = morph
                        val s = 0.55f + 0.45f * morph
                        scaleX = s
                        scaleY = s
                    },
            )
            ApplePlayIcon(
                tint = AppleControlWhite,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = 1f - morph
                        val s = 1f - 0.45f * morph
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

/**
 * Rounded pill translucent Apple glass skip button with spring press scale,
 * gentle horizontal nudge on tap, and [AppleForwardIcon] / [AppleBackwardIcon].
 */
@Composable
fun AppleSkipButton(
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(if (forward) R.string.next else R.string.previous)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "appleSkipScale",
    )
    val pressHighlight by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "appleSkipHighlight",
    )

    var nudgeTrigger by remember { mutableIntStateOf(0) }
    val nudgeOffset = remember { Animatable(0f) }
    LaunchedEffect(nudgeTrigger) {
        if (nudgeTrigger > 0) {
            nudgeOffset.animateTo(1f, tween(durationMillis = 80))
            nudgeOffset.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 480f))
        }
    }

    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(width = 68.dp, height = 54.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f + pressHighlight * 0.16f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f + pressHighlight * 0.12f),
                shape = shape,
            )
            .semantics {
                this.contentDescription = label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    nudgeTrigger++
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        val iconModifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                translationX = (if (forward) 1f else -1f) * nudgeOffset.value * 5.dp.toPx()
            }
        if (forward) {
            AppleForwardIcon(tint = AppleControlWhite, modifier = iconModifier)
        } else {
            AppleBackwardIcon(tint = AppleControlWhite, modifier = iconModifier)
        }
    }
}
