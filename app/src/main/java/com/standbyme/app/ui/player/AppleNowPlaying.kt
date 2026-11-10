package com.standbyme.app.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.media.PlaybackUiState
import com.standbyme.app.ui.components.formatDuration
import com.standbyme.app.ui.theme.Nunito
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private val AppleWhite = Color(0xFFF0F0ED)
private val AppleSecondary = Color.White.copy(alpha = 0.45f)
private val AppleTrack = Color.White.copy(alpha = 0.5f)

/**
 * Fond flouté : deux copies de la pochette, agrandies et tournant lentement en sens
 * inverse, sous un flou fort. Sans pochette : dégradé de la couleur principale.
 */
@Composable
fun AppleBackdrop(art: Bitmap?, isPlaying: Boolean, fallbackColor: Color, modifier: Modifier = Modifier) {
    val rotation by rememberInfiniteTransition(label = "backdrop").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "backdropRotation",
    )
    val image = remember(art) { art?.asImageBitmap() }
    Crossfade(targetState = image, animationSpec = tween(900), label = "appleBackdrop", modifier = modifier) { bmp ->
        Canvas(
            Modifier
                .fillMaxSize()
                .blur(80.dp),
        ) {
            if (bmp == null) {
                drawRect(Brush.verticalGradient(listOf(fallbackColor.copy(alpha = 0.55f), Color.Black)))
            } else {
                val diagonal = sqrt(size.width * size.width + size.height * size.height)
                val angle = if (isPlaying) rotation else 0f
                drawRotatedArt(bmp, diagonal * 1.25f, angle, 1f)
                drawRotatedArt(bmp, diagonal * 1.6f, 140f - angle, 0.5f)
            }
            drawRect(Color.Black.copy(alpha = 0.32f))
        }
    }
}

private fun DrawScope.drawRotatedArt(bmp: ImageBitmap, side: Float, degrees: Float, alpha: Float) {
    rotate(degrees) {
        val s = side.toInt()
        drawImage(
            image = bmp,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bmp.width, bmp.height),
            dstOffset = IntOffset((center.x - side / 2f).toInt(), (center.y - side / 2f).toInt()),
            dstSize = IntSize(s, s),
            alpha = alpha,
        )
    }
}

/** Mise en page du lecteur « design Apple » (pochette à gauche, infos et commandes à droite). */
@Composable
fun AppleNowPlaying(
    playback: PlaybackUiState,
    positionMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onOpenOutputPicker: () -> Unit,
) {
    if (!playback.hasSession) {
        AppleEmpty()
        return
    }
    val context = LocalContext.current
    val listeningOn = remember(playback.isRemoteVolume, playback.packageName) {
        listeningOnLabel(context, playback.isRemoteVolume)
    }
    val gutter = 44.dp

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleArtwork(playback.albumArt, playback.isPlaying, 260.dp)
        Spacer(Modifier.width(gutter))
        Column(
            Modifier
                .weight(1f)
                .height(260.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AppleTopBar(playback, onSetVolume, onOpenOutputPicker)
            Column {
                Text(
                    playback.title,
                    color = AppleWhite,
                    fontSize = 42.sp,
                    lineHeight = 50.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Nunito,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(playback.artist, playback.album).filter { it.isNotBlank() }.joinToString(" • "),
                    modifier = Modifier.fillMaxWidth(),
                    color = AppleSecondary,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Nunito,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(listeningOn, color = AppleSecondary, fontSize = 14.sp, fontFamily = Nunito, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                AppleSkipButton(forward = false, onClick = onPrevious)
                ApplePlayPause(playback.isPlaying, onTogglePlay)
                AppleSkipButton(forward = true, onClick = onNext)
            }
            AppleProgress(playback, positionMs, onSeek)
        }
    }
}

@Composable
private fun AppleEmpty() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.MusicNote, null, Modifier.size(44.dp), tint = AppleSecondary)
        Text(stringResource(R.string.no_playback_title), color = AppleWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito)
        Text(stringResource(R.string.no_playback_body), color = AppleSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = Nunito)
    }
}

/** Pochette qui « respire » : plus grande et plus d'ombre en lecture, réduite en pause. */
@Composable
private fun AppleArtwork(art: Bitmap?, isPlaying: Boolean, size: androidx.compose.ui.unit.Dp) {
    val scale by animateFloatAsState(if (isPlaying) 1f else 0.86f, spring(0.62f, 220f), label = "appleArtScale")
    val shadow by animateDpAsState(if (isPlaying) 24.dp else 8.dp, spring(stiffness = 220f), label = "appleArtShadow")
    val shape = RoundedCornerShape(18.dp)
    val image = remember(art) { art?.asImageBitmap() }
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .shadow(shadow, shape)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(image, animationSpec = tween(500), label = "appleArt") { bmp ->
            if (bmp != null) {
                Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MusicNote, null, Modifier.size(64.dp), tint = AppleSecondary)
                }
            }
        }
    }
}

/** Ligne du haut : bouton volume (qui déplie un curseur) et bouton sortie audio. */
@Composable
private fun AppleTopBar(playback: PlaybackUiState, onSetVolume: (Float) -> Unit, onOpenOutputPicker: () -> Unit) {
    var volumeOpen by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableStateOf(0) }

    // Le curseur de volume se replie tout seul après 3 s sans interaction.
    LaunchedEffect(volumeOpen, lastTouch) {
        if (volumeOpen) {
            delay(3_000)
            volumeOpen = false
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = volumeOpen,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
        ) {
            AppleBar(
                fraction = playback.volumeFraction,
                onChange = {
                    lastTouch++
                    if (playback.canAdjustVolume) onSetVolume(it)
                },
                modifier = Modifier
                    .width(220.dp)
                    .padding(end = 12.dp),
            )
        }
        AppleCircleButton(56.dp, stringResource(R.string.volume), onClick = { volumeOpen = !volumeOpen }) {
            Icon(Icons.Filled.VolumeUp, null, Modifier.size(26.dp), tint = AppleWhite)
        }
        Spacer(Modifier.width(12.dp))
        AppleCircleButton(56.dp, stringResource(R.string.output_picker), onClick = onOpenOutputPicker) {
            Icon(Icons.Filled.Speaker, null, Modifier.size(26.dp), tint = AppleWhite)
        }
    }
}

/** Bouton rond translucide qui s'éclaircit à l'appui. */
@Composable
private fun AppleCircleButton(
    size: androidx.compose.ui.unit.Dp,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val glow by animateFloatAsState(if (pressed) 1f else 0f, label = "applePress")
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f + glow * 0.16f))
            .clickable(interactionSource = interaction, indication = null, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ApplePlayPause(isPlaying: Boolean, onClick: () -> Unit) {
    val morph by animateFloatAsState(if (isPlaying) 1f else 0f, spring(0.72f, 420f), label = "applePlayPause")
    val label = stringResource(if (isPlaying) R.string.pause else R.string.play)
    AppleCircleButton(100.dp, label, onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.Pause, null,
                Modifier
                    .size(64.dp)
                    .graphicsLayer { alpha = morph; scaleX = 0.6f + 0.4f * morph; scaleY = scaleX },
                tint = AppleWhite,
            )
            Icon(
                Icons.Filled.PlayArrow, null,
                Modifier
                    .size(64.dp)
                    .graphicsLayer { alpha = 1f - morph; scaleX = 1f - 0.4f * morph; scaleY = scaleX },
                tint = AppleWhite,
            )
        }
    }
}

@Composable
private fun AppleSkipButton(forward: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (forward) R.string.next else R.string.previous)
    var nudge by remember { mutableStateOf(0) }
    val shift by animateFloatAsState(if (nudge % 2 == 1) 1f else 0f, spring(0.5f, 600f), label = "appleSkip")
    Box(
        Modifier
            .size(width = 88.dp, height = 66.dp)
            .clip(RoundedCornerShape(33.dp))
            .pointerInput(onClick) {
                detectTapGestures(onTap = {
                    nudge++
                    onClick()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (forward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
            contentDescription = label,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer { translationX = (if (forward) 1 else -1) * shift * 10.dp.toPx() },
            tint = AppleWhite,
        )
    }
}

/** Barre de progression épaisse qui grossit pendant qu'on la touche. */
@Composable
private fun AppleProgress(playback: PlaybackUiState, positionMs: Long, onSeek: (Long) -> Unit) {
    val duration = playback.durationMs
    val fraction = if (duration > 0) positionMs.toFloat() / duration else 0f
    Column {
        AppleBar(fraction, onChange = { if (duration > 0) onSeek((it * duration).toLong()) }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppleTime(formatDuration(positionMs))
            AppleTime("−" + formatDuration((duration - positionMs).coerceAtLeast(0L)))
        }
    }
}

@Composable
private fun AppleTime(text: String) {
    Text(text, color = AppleSecondary, fontSize = 19.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Nunito)
}

@Composable
private fun AppleBar(fraction: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    var touching by remember { mutableStateOf(false) }
    val barHeight by animateDpAsState(if (touching) 18.dp else 10.dp, spring(0.7f, 600f), label = "appleBarHeight")
    val alpha by animateFloatAsState(if (touching) 0.9f else 1f, label = "appleBarAlpha")
    Box(
        modifier
            .height(24.dp)
            .pointerInput(Unit) {
                awaitPointerEventScopeLoop(
                    onDown = { touching = true; onChange((it / size.width).coerceIn(0f, 1f)) },
                    onMove = { onChange((it / size.width).coerceIn(0f, 1f)) },
                    onUp = { touching = false },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(AppleTrack.copy(alpha = AppleTrack.alpha * alpha + 0.4f)),
            )
        }
    }
}

/** Boucle de gestes simple : appui, déplacement, relâchement (coordonnée X). */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitPointerEventScopeLoop(
    onDown: (Float) -> Unit,
    onMove: (Float) -> Unit,
    onUp: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: continue
            onDown(down.position.x)
            down.consume()
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.pressed) {
                    onUp()
                    break
                }
                onMove(change.position.x)
                change.consume()
            }
        }
    }
}
