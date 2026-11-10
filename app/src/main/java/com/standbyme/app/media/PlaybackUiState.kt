package com.standbyme.app.media

import android.graphics.Bitmap
import android.media.session.MediaSession

data class QueueItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val isCurrent: Boolean,
)

/** Instantané de ce qui joue actuellement, quelle que soit l'application source. */
data class PlaybackUiState(
    val hasSession: Boolean = false,
    val packageName: String = "",
    val sessionToken: MediaSession.Token? = null,
    val appLabel: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    /** Position rapportée par la session au moment [positionAnchorRealtimeMs]. */
    val positionMs: Long = 0L,
    val positionAnchorRealtimeMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val durationMs: Long = 0L,
    val queue: List<QueueItem> = emptyList(),
    val volumeFraction: Float = 0f,
    /** Vrai quand la lecture est diffusée (Cast…) et que le volume est distant. */
    val isRemoteVolume: Boolean = false,
    val canAdjustVolume: Boolean = true,
) {
    /** Extrapole la position courante à partir de l'horloge [nowRealtimeMs]. */
    fun positionAt(nowRealtimeMs: Long): Long {
        if (isPlaying && durationMs > 0) {
            val elapsed = ((nowRealtimeMs - positionAnchorRealtimeMs) * playbackSpeed).toLong()
            return (positionMs + elapsed).coerceIn(0L, durationMs)
        }
        val max = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return positionMs.coerceIn(0L, max)
    }

    companion object {
        val Empty = PlaybackUiState()
    }
}
