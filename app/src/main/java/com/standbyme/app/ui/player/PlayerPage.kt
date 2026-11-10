package com.standbyme.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.standbyme.app.media.PlaybackUiState

/**
 * Page « Widgets » : lecteur en haut, file d'attente en glissant vers le bas.
 * En design Apple, un fond flouté occupe tout l'écran.
 */
@Composable
fun PlayerPage(
    playback: PlaybackUiState,
    positionMs: Long,
    waveIntensity: Float,
    auraIntensity: Float,
    auraEnabled: Boolean,
    auraUseAlbumColor: Boolean,
    appleDesign: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Float) -> Unit,
    onOpenOutputPicker: () -> Unit,
) {
    val pager = rememberPagerState { 2 }
    if (appleDesign) {
        Box(Modifier.fillMaxSize()) {
            AppleBackdrop(playback.albumArt, playback.isPlaying, MaterialTheme.colorScheme.primary, Modifier.fillMaxSize())
            VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    AppleNowPlaying(playback, positionMs, onTogglePlay, onNext, onPrevious, onSeek, onSetVolume, onOpenOutputPicker)
                } else {
                    Box(Modifier.fillMaxSize().padding(24.dp)) {
                        QueuePage(playback, onOpenOutputPicker, background = Color.Black.copy(alpha = 0.32f))
                    }
                }
            }
        }
    } else {
        VerticalPager(pager, Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                NowPlayingScreen(
                    playback, positionMs, waveIntensity, auraIntensity, auraEnabled, auraUseAlbumColor,
                    onTogglePlay, onNext, onPrevious, onSeek, onSetVolume, onOpenOutputPicker,
                )
            } else {
                QueuePage(playback, onOpenOutputPicker)
            }
        }
    }
}
