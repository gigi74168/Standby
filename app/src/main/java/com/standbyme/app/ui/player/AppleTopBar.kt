package com.standbyme.app.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.standbyme.app.R
import com.standbyme.app.media.PlaybackUiState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

private val AppleTopBarWhite = Color(0xFFF0F0ED)
private val ApplePillDarkTint = Color(0xFF1C1C1E)
private val TopBarHeight = 44.dp
private val VolumePillExpandedWidth = 212.dp
private val TopBarShape = RoundedCornerShape(22.dp)

/**
 * Barre supérieure du mode « Design Apple » : bouton Volume qui se déplie en capsule
 * façon Control Center, et bouton de sortie audio avec icône AirPlay.
 */
@Composable
fun AppleTopBar(
    playback: PlaybackUiState,
    onSetVolume: (Float) -> Unit,
    onOpenOutputPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var volumeOpen by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var isTouching by remember { mutableStateOf(false) }

    // Le curseur de volume se replie après 3 s sans interaction (sauf pendant un glisser actif).
    LaunchedEffect(volumeOpen, interactionTick, isTouching) {
        if (volumeOpen && !isTouching) {
            delay(3_000)
            volumeOpen = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleVolumeControl(
            volumeFraction = playback.volumeFraction,
            canAdjustVolume = playback.canAdjustVolume,
            volumeOpen = volumeOpen,
            onOpen = {
                volumeOpen = true
                interactionTick++
            },
            onCollapse = {
                volumeOpen = false
            },
            onSetVolume = {
                interactionTick++
                onSetVolume(it)
            },
            onTouchingChanged = { touching ->
                isTouching = touching
                if (!touching) interactionTick++
            },
        )
        Spacer(Modifier.width(10.dp))
        AppleOutputButton(
            isRemote = playback.isRemoteVolume,
            onClick = onOpenOutputPicker,
        )
    }
}

@Composable
private fun AppleVolumeControl(
    volumeFraction: Float,
    canAdjustVolume: Boolean,
    volumeOpen: Boolean,
    onOpen: () -> Unit,
    onCollapse: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onTouchingChanged: (Boolean) -> Unit,
) {
    val volumeLabel = stringResource(R.string.volume)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (!volumeOpen && pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "appleVolumePressScale",
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (!volumeOpen && pressed) 1f else 0f,
        animationSpec = tween(120),
        label = "appleVolumePressGlow",
    )
    val pillWidth by animateDpAsState(
        targetValue = if (volumeOpen) VolumePillExpandedWidth else TopBarHeight,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "appleVolumePillWidth",
    )
    val expandProgress = ((pillWidth - TopBarHeight) / (VolumePillExpandedWidth - TopBarHeight)).coerceIn(0f, 1f)

    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val animatedFraction by animateFloatAsState(
        targetValue = volumeFraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "appleVolumeFraction",
    )
    val displayFraction = (dragFraction ?: animatedFraction).coerceIn(0f, 1f)

    val iconOverFill = volumeOpen && expandProgress > 0.45f && displayFraction > 0.11f
    val iconTint by animateColorAsState(
        targetValue = if (iconOverFill) ApplePillDarkTint.copy(alpha = 0.85f) else AppleTopBarWhite,
        animationSpec = tween(150),
        label = "appleVolumeIconTint",
    )

    val currentCanAdjust by rememberUpdatedState(canAdjustVolume)
    val currentOnSetVolume by rememberUpdatedState(onSetVolume)
    val currentOnCollapse by rememberUpdatedState(onCollapse)
    val currentOnTouchingChanged by rememberUpdatedState(onTouchingChanged)

    val gestureModifier = if (!volumeOpen) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClickLabel = volumeLabel,
            onClick = onOpen,
        )
    } else {
        Modifier
            .semantics {
                contentDescription = volumeLabel
                role = Role.Button
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    currentOnTouchingChanged(true)

                    val iconZonePx = TopBarHeight.toPx()
                    val touchSlop = viewConfiguration.touchSlop
                    val startX = down.position.x
                    val startedOnIcon = startX <= iconZonePx
                    var dragged = false

                    if (!startedOnIcon && currentCanAdjust && size.width > 0) {
                        val fraction = (startX / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = fraction
                        currentOnSetVolume(fraction)
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (!change.pressed) {
                                change.consume()
                                if (!dragged && startedOnIcon) {
                                    currentOnCollapse()
                                }
                                break
                            }

                            val dx = change.position.x - startX
                            if (!dragged && abs(dx) > touchSlop) {
                                dragged = true
                            }
                            if (dragged && currentCanAdjust && size.width > 0) {
                                val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                dragFraction = fraction
                                currentOnSetVolume(fraction)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } finally {
                        dragFraction = null
                        currentOnTouchingChanged(false)
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(TopBarHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(TopBarShape)
            .background(Color.White.copy(alpha = 0.14f + pressGlow * 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f + expandProgress * 0.04f), TopBarShape)
            .then(gestureModifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (expandProgress > 0.01f && displayFraction > 0.002f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(displayFraction)
                    .background(
                        color = AppleTopBarWhite.copy(alpha = 0.90f * expandProgress),
                        shape = TopBarShape,
                    ),
            )
        }

        Box(
            modifier = Modifier.size(TopBarHeight),
            contentAlignment = Alignment.Center,
        ) {
            AppleVolumeIcon(
                volumeFraction = displayFraction,
                tint = iconTint,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun AppleOutputButton(
    isRemote: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(R.string.output_picker)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "appleOutputPressScale",
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(120),
        label = "appleOutputPressGlow",
    )
    val baseFillAlpha = if (isRemote) 0.22f else 0.14f
    val borderAlpha = if (isRemote) 0.34f else 0.18f

    Box(
        modifier = Modifier
            .size(TopBarHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = baseFillAlpha + pressGlow * 0.12f))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AppleAirPlayIcon(
            isRemote = isRemote,
            tint = if (isRemote) Color.White else AppleTopBarWhite,
            modifier = Modifier.size(21.dp),
        )
    }
}

/**
 * Icône de volume style Apple SF Symbols (`speaker.wave.3.fill` / `2` / `1` / `speaker.slash.fill`) :
 * silhouette de haut-parleur arrondie et ondes concentriques à extrémités rondes.
 */
@Composable
fun AppleVolumeIcon(
    volumeFraction: Float,
    isMuted: Boolean = volumeFraction <= 0.01f,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val muted = isMuted || volumeFraction <= 0.01f
    val waveCount = when {
        muted -> 0
        volumeFraction < 0.34f -> 1
        volumeFraction < 0.68f -> 2
        else -> 3
    }
    val wave1Alpha by animateFloatAsState(if (waveCount >= 1) 1f else 0f, tween(140), label = "wave1")
    val wave2Alpha by animateFloatAsState(if (waveCount >= 2) 1f else 0f, tween(140), label = "wave2")
    val wave3Alpha by animateFloatAsState(if (waveCount >= 3) 1f else 0f, tween(140), label = "wave3")
    val slashAlpha by animateFloatAsState(if (muted) 1f else 0f, tween(160), label = "slash")

    Canvas(
        modifier = modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val s = min(size.width, size.height)
        if (s <= 0f) return@Canvas
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun px(u: Float) = ox + u * s
        fun py(v: Float) = oy + v * s

        // Silhouette arrondie du haut-parleur (corps capsule + cône évasé aux sommets adoucis)
        val speakerPath = Path().apply {
            moveTo(px(0.11f), py(0.42f))
            cubicTo(px(0.11f), py(0.392f), px(0.122f), py(0.38f), px(0.15f), py(0.38f))
            lineTo(px(0.24f), py(0.38f))
            lineTo(px(0.395f), py(0.245f))
            cubicTo(px(0.422f), py(0.222f), px(0.445f), py(0.238f), px(0.445f), py(0.275f))
            lineTo(px(0.445f), py(0.725f))
            cubicTo(px(0.445f), py(0.762f), px(0.422f), py(0.778f), px(0.395f), py(0.755f))
            lineTo(px(0.24f), py(0.62f))
            lineTo(px(0.15f), py(0.62f))
            cubicTo(px(0.122f), py(0.62f), px(0.11f), py(0.608f), px(0.11f), py(0.58f))
            close()
        }
        drawPath(path = speakerPath, color = tint)

        // Ondes sonores concentriques à bouts arrondis
        val strokeWidth = 0.068f * s
        val arcStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val waveCx = px(0.44f)
        val waveCy = py(0.50f)
        val radii = floatArrayOf(0.155f * s, 0.28f * s, 0.405f * s)
        val alphas = floatArrayOf(wave1Alpha, wave2Alpha, wave3Alpha)

        for (i in radii.indices) {
            val a = alphas[i]
            if (a > 0.01f) {
                val r = radii[i]
                drawArc(
                    color = tint.copy(alpha = tint.alpha * a),
                    startAngle = -42f,
                    sweepAngle = 84f,
                    useCenter = false,
                    topLeft = Offset(waveCx - r, waveCy - r),
                    size = Size(r * 2f, r * 2f),
                    style = arcStroke,
                )
            }
        }

        // Barre oblique subtile façon speaker.slash.fill avec léger détourage sur le haut-parleur
        if (slashAlpha > 0.01f) {
            val slashStart = Offset(px(0.12f), py(0.21f))
            val slashEnd = Offset(px(0.68f), py(0.77f))
            drawLine(
                color = Color.Black.copy(alpha = slashAlpha),
                start = slashStart,
                end = slashEnd,
                strokeWidth = 0.145f * s,
                cap = StrokeCap.Round,
                blendMode = BlendMode.DstOut,
            )
            drawLine(
                color = tint.copy(alpha = tint.alpha * 0.92f * slashAlpha),
                start = slashStart,
                end = slashEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Icône AirPlay Audio style Apple SF Symbols (`airplayaudio`) :
 * trois arcs circulaires concentriques ouverts en bas, avec un triangle plein arrondi centré dans l'ouverture.
 */
@Composable
fun AppleAirPlayIcon(
    isRemote: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val arcAlphaFactor = if (isRemote) 1f else 0.86f
    Canvas(modifier = modifier) {
        val s = min(size.width, size.height)
        if (s <= 0f) return@Canvas
        val ox = (size.width - s) / 2f
        val oy = (size.height - s) / 2f
        fun px(u: Float) = ox + u * s
        fun py(v: Float) = oy + v * s

        val strokeWidth = 0.068f * s
        val arcStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val cx = px(0.50f)
        val cy = py(0.51f)
        val arcColor = tint.copy(alpha = tint.alpha * arcAlphaFactor)

        val radii = floatArrayOf(0.145f * s, 0.265f * s, 0.385f * s)
        for (r in radii) {
            drawArc(
                color = arcColor,
                startAngle = 134f,
                sweepAngle = 272f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = arcStroke,
            )
        }

        // Triangle plein aux angles généreusement arrondis centré dans l'ouverture inférieure
        val trianglePath = Path().apply {
            moveTo(px(0.534f), py(0.632f))
            lineTo(px(0.652f), py(0.814f))
            cubicTo(px(0.682f), py(0.860f), px(0.678f), py(0.870f), px(0.622f), py(0.870f))
            lineTo(px(0.378f), py(0.870f))
            cubicTo(px(0.322f), py(0.870f), px(0.318f), py(0.860f), px(0.348f), py(0.814f))
            lineTo(px(0.466f), py(0.632f))
            cubicTo(px(0.488f), py(0.598f), px(0.512f), py(0.598f), px(0.534f), py(0.632f))
            close()
        }
        drawPath(path = trianglePath, color = tint)
    }
}
