package com.standbyme.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.standbyme.app.R
import com.standbyme.app.media.PlaybackUiState
import com.standbyme.app.ui.components.Equalizer
import com.standbyme.app.ui.components.PlayPauseButton
import com.standbyme.app.ui.components.RoundIconButton
import com.standbyme.app.ui.components.WavySlider
import com.standbyme.app.ui.components.formatDuration
import com.standbyme.app.ui.components.rememberCutoutSafeHorizontalPadding

/** Lecteur « classique » (Material You) avec halo et barre ondulée. */
@Composable
fun NowPlayingScreen(
    playback: PlaybackUiState,
    positionMs: Long,
    waveIntensity: Float,
    auraIntensity: Float,
    auraEnabled: Boolean,
    auraUseAlbumColor: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onOpenOutputPicker: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val contentPadding = rememberCutoutSafeHorizontalPadding(
        baseStart = 34.dp,
        baseEnd = 34.dp,
        extraCutoutClearance = 14.dp,
        extraStartWhenNoCutout = 10.dp,
        top = 30.dp,
        bottom = 30.dp,
    )
    Row(
        Modifier
            .fillMaxSize()
            .background(colors.surface, RoundedCornerShape(30.dp))
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!playback.hasSession) {
            EmptyPlayback()
            return@Row
        }

        ArtworkWithAura(playback, 250.dp, auraIntensity, auraEnabled, auraUseAlbumColor)
        Spacer(Modifier.width(32.dp))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Equalizer(bars = 8, height = 15.dp, color = colors.primary)
                Text(
                    stringResource(R.string.now_playing_on, playback.appLabel).uppercase(),
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                )
            }
            Column {
                Text(
                    playback.title,
                    color = colors.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(playback.artist, playback.album).filter { it.isNotBlank() }.joinToString(" · "),
                    color = colors.onSurfaceVariant,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            PlaybackProgress(playback, positionMs, waveIntensity, onSeek)

            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Filled.SkipPrevious, stringResource(R.string.previous), colors.onSurface, onPrevious)
                Spacer(Modifier.width(18.dp))
                PlayPauseButton(playback.isPlaying, colors.primary, colors.onPrimary, onTogglePlay)
                Spacer(Modifier.width(18.dp))
                RoundIconButton(Icons.Filled.SkipNext, stringResource(R.string.next), colors.onSurface, onNext)
                Spacer(Modifier.weight(1f))
                VolumeControl(playback, onSetVolume)
                Spacer(Modifier.width(10.dp))
                OutputButton(playback.isRemoteVolume, onOpenOutputPicker)
            }
        }
    }
}

@Composable
private fun EmptyPlayback() {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.MusicNote, null, Modifier.size(40.dp), tint = colors.onSurfaceVariant)
        Text(
            stringResource(R.string.no_playback_title),
            color = colors.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(R.string.no_playback_body),
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlaybackProgress(
    playback: PlaybackUiState,
    positionMs: Long,
    waveIntensity: Float,
    onSeek: (Long) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val duration = playback.durationMs
    val fraction = if (duration > 0) positionMs.toFloat() / duration else 0f
    Column {
        WavySlider(
            value = fraction,
            onValueChange = { if (duration > 0) onSeek((it * duration).toLong()) },
            wavy = playback.isPlaying,
            amplitude = (waveIntensity.coerceIn(0f, 1f) * 3f + 1f).dp,
            trackColor = colors.primary.copy(alpha = 0.16f),
            activeColor = colors.primary,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), color = colors.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "−" + formatDuration((duration - positionMs).coerceAtLeast(0L)),
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun VolumeControl(playback: PlaybackUiState, onSetVolume: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            Icons.Filled.VolumeUp,
            contentDescription = stringResource(R.string.volume),
            modifier = Modifier.size(17.dp),
            tint = if (playback.isRemoteVolume) colors.tertiary else colors.onSurfaceVariant,
        )
        WavySlider(
            value = playback.volumeFraction,
            onValueChange = { if (playback.canAdjustVolume) onSetVolume(it) },
            modifier = Modifier.width(90.dp),
            trackColor = colors.primary.copy(alpha = 0.16f),
            activeColor = colors.onSurface,
            commitOnRelease = false,
        )
    }
}

@Composable
private fun OutputButton(isRemote: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isRemote) colors.tertiary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Cast,
            contentDescription = stringResource(R.string.output_picker),
            modifier = Modifier.size(18.dp),
            tint = if (isRemote) colors.tertiary else colors.onSurfaceVariant,
        )
    }
}
