package com.standbyme.app.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.media.PlaybackUiState
import com.standbyme.app.ui.components.formatDuration
import com.standbyme.app.ui.components.rememberCutoutSafeHorizontalPadding
import com.standbyme.app.ui.theme.Nunito

private val AppleWhite = Color(0xFFF0F0ED)
private val AppleSecondary = Color.White.copy(alpha = 0.45f)

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
    val gutter = 44.dp
    val safePadding = rememberCutoutSafeHorizontalPadding(
        baseStart = gutter,
        baseEnd = gutter,
        extraCutoutClearance = 14.dp,
        extraStartWhenNoCutout = 12.dp,
    )
    if (!playback.hasSession) {
        AppleEmpty(safePadding)
        return
    }
    val context = LocalContext.current
    val listeningOn = remember(playback.isRemoteVolume, playback.packageName) {
        listeningOnLabel(context, playback.isRemoteVolume)
    }

    Row(
        Modifier
            .fillMaxSize()
            .padding(safePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppleArtwork(playback.albumArt, playback.isPlaying, 260.dp)
        Spacer(Modifier.width(gutter))
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 260.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            AppleTopBar(playback, onSetVolume, onOpenOutputPicker)
            Column {
                Text(
                    playback.title,
                    color = AppleWhite,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = Nunito,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(playback.artist, playback.album).filter { it.isNotBlank() }.joinToString(" • "),
                    modifier = Modifier.fillMaxWidth(),
                    color = AppleSecondary,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Nunito,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listeningOn,
                    color = AppleSecondary,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontFamily = Nunito,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppleSkipButton(forward = false, onClick = onPrevious)
                ApplePlayPauseButton(
                    isPlaying = playback.isPlaying,
                    onClick = onTogglePlay,
                    buttonSize = 72.dp,
                    iconSize = 34.dp,
                )
                AppleSkipButton(forward = true, onClick = onNext)
            }
            AppleProgress(playback, positionMs, onSeek)
        }
    }
}

@Composable
private fun AppleEmpty(safePadding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(safePadding),
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
private fun AppleArtwork(art: Bitmap?, isPlaying: Boolean, size: Dp) {
    val scale by animateFloatAsState(if (isPlaying) 1f else 0.86f, spring(0.62f, 220f), label = "appleArtScale")
    val shadow by animateDpAsState(if (isPlaying) 24.dp else 8.dp, spring(stiffness = 220f), label = "appleArtShadow")
    val shape = RoundedCornerShape(18.dp)
    val safeArt = art?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 }
    val image = remember(safeArt) {
        safeArt?.takeIf { !it.isRecycled }?.let { bmp ->
            runCatching { bmp.asImageBitmap() }.getOrNull()
        }
    }
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
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentOnChange by rememberUpdatedState(onChange)

    val barHeight by animateDpAsState(if (touching) 18.dp else 10.dp, spring(0.7f, 600f), label = "appleBarHeight")
    val fillAlpha by animateFloatAsState(if (touching) 0.95f else 0.84f, tween(120), label = "appleBarAlpha")
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "appleBarFraction",
    )
    val displayFraction = (dragFraction ?: animatedFraction).coerceIn(0f, 1f)

    Box(
        modifier
            .height(24.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    touching = true
                    if (size.width > 0) {
                        val initial = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = initial
                        currentOnChange(initial)
                    }
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            if (size.width > 0) {
                                val next = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                dragFraction = next
                                currentOnChange(next)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } finally {
                        touching = false
                        dragFraction = null
                    }
                }
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
            if (displayFraction > 0.002f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(displayFraction)
                        .background(AppleWhite.copy(alpha = fillAlpha), CircleShape),
                )
            }
        }
    }
}
