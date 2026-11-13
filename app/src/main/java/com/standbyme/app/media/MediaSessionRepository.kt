package com.standbyme.app.media

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

/**
 * Suit la session multimédia la plus pertinente du système et expose son état,
 * plus les commandes de transport (lecture, piste suivante, volume…).
 */
class MediaSessionRepository(private val app: Application) {

    private val sessionManager = app.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(app, StandbyNotificationListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeController: MediaController? = null

    private val cacheLock = Any()
    private var cachedSessionToken: android.media.session.MediaSession.Token? = null
    private var cachedKey: MetadataCacheKey? = null
    private var cachedArt: Bitmap? = null

    private data class MetadataCacheKey(
        val packageName: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
    )

    fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)

    /** Flux froid : s'abonne aux sessions tant qu'il est collecté. */
    fun playbackStates(): Flow<PlaybackUiState> = callbackFlow {
        var controllerCallback: MediaController.Callback? = null

        fun detach() {
            controllerCallback?.let { cb -> activeController?.unregisterCallback(cb) }
            activeController = null
            controllerCallback = null
        }

        fun onSessionsChanged(sessions: List<MediaController>?) {
            val chosen = pickController(sessions.orEmpty())
            if (chosen == null) {
                detach()
                emitState(null)
                return
            }
            if (activeController?.sessionToken == chosen.sessionToken) {
                emitState(chosen)
                return
            }
            detach()
            activeController = chosen
            val cb = object : MediaController.Callback() {
                override fun onAudioInfoChanged(info: MediaController.PlaybackInfo) = emitState(chosen)
                override fun onMetadataChanged(metadata: MediaMetadata?) = emitState(chosen)
                override fun onPlaybackStateChanged(state: PlaybackState?) = emitState(chosen)
                override fun onQueueChanged(queue: MutableList<android.media.session.MediaSession.QueueItem>?) =
                    emitState(chosen)

                override fun onSessionDestroyed() {
                    detach()
                    emitState(null)
                }
            }
            chosen.registerCallback(cb, mainHandler)
            controllerCallback = cb
            emitState(chosen)
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { onSessionsChanged(it) }

        // Le volume système peut changer depuis les boutons physiques.
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = emitState(activeController)
        }
        ContextCompat.registerReceiver(
            app, volumeReceiver, IntentFilter(VOLUME_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        if (hasNotificationAccess()) {
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsListener, listenerComponent, mainHandler)
                onSessionsChanged(sessionManager.getActiveSessions(listenerComponent))
            } catch (_: SecurityException) {
                emitState(null)
            }
        } else {
            emitState(null)
        }

        awaitClose {
            detach()
            app.unregisterReceiver(volumeReceiver)
            try {
                sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (_: Exception) {
            }
        }
    }

    // --- Commandes ---------------------------------------------------------------------------

    fun togglePlayPause() {
        val controller = activeController ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun seekTo(positionMs: Long) {
        activeController?.transportControls?.seekTo(positionMs)
    }

    fun setVolume(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        val controller = activeController
        val info = controller?.playbackInfo
        if (controller != null && info != null && info.maxVolume > 0) {
            controller.setVolumeTo((f * info.maxVolume).roundToInt(), 0)
        } else {
            val audio = app.getSystemService(AudioManager::class.java) ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) audio.setStreamVolume(AudioManager.STREAM_MUSIC, (f * max).roundToInt(), 0)
        }
    }

    /** Ouvre le sélecteur de sortie audio de SystemUI pour la session en cours. */
    fun openOutputPicker() {
        val controller = activeController ?: return
        try {
            app.sendBroadcast(
                Intent()
                    .setAction("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
                    .setPackage("com.android.systemui")
                    .putExtra("package_name", controller.packageName)
                    .putExtra("key_media_session_token", controller.sessionToken),
            )
        } catch (_: Exception) {
        }
    }

    // --- Interne -----------------------------------------------------------------------------

    private fun ProducerScope<PlaybackUiState>.emitState(controller: MediaController?) {
        trySend(buildState(controller))
    }

    private fun systemVolumeFraction(): Float {
        val audio = app.getSystemService(AudioManager::class.java) ?: return 0f
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0f
        return (audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max).coerceIn(0f, 1f)
    }

    private fun buildState(controller: MediaController?): PlaybackUiState {
        if (controller == null) {
            synchronized(cacheLock) {
                cachedSessionToken = null
                cachedKey = null
                cachedArt = null
            }
            return PlaybackUiState(volumeFraction = systemVolumeFraction())
        }

        val packageName = controller.packageName.orEmpty()
        val metadata = controller.metadata
        val playback = controller.playbackState

        val appLabel = try {
            val pm = app.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) {
            packageName
        }

        val (title, artist, album, durationMs, art) = resolveMetadataAndArtwork(
            token = controller.sessionToken,
            packageName = packageName,
            metadata = metadata,
            playbackState = playback?.state,
        )

        val activeQueueId = playback?.activeQueueItemId ?: -1L
        val queue = controller.queue.orEmpty().mapNotNull { item ->
            val description = item.description ?: return@mapNotNull null
            QueueItem(
                id = item.queueId,
                title = description.title?.toString().orEmpty(),
                subtitle = description.subtitle?.toString().orEmpty(),
                isCurrent = item.queueId == activeQueueId,
            )
        }

        val info = controller.playbackInfo
        val volume = if (info.maxVolume > 0) info.currentVolume.toFloat() / info.maxVolume else systemVolumeFraction()

        return PlaybackUiState(
            hasSession = true,
            packageName = packageName,
            sessionToken = controller.sessionToken,
            appLabel = appLabel,
            title = title,
            artist = artist,
            album = album,
            albumArt = art,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            positionMs = playback?.position?.coerceAtLeast(0L) ?: 0L,
            positionAnchorRealtimeMs = SystemClock.elapsedRealtime(),
            playbackSpeed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            durationMs = durationMs,
            queue = queue,
            volumeFraction = volume,
            isRemoteVolume = info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
            canAdjustVolume = info.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_FIXED,
        )
    }

    private data class ResolvedMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val art: Bitmap?,
    )

    private fun resolveMetadataAndArtwork(
        token: android.media.session.MediaSession.Token,
        packageName: String,
        metadata: MediaMetadata?,
        playbackState: Int?,
    ): ResolvedMetadata = synchronized(cacheLock) {
        val validCachedArt = cachedArt?.takeIf { !it.isRecycled }
        val sameSession = cachedSessionToken == token

        if (metadata == null) {
            val isActiveTransition = sameSession &&
                cachedKey != null &&
                playbackState != null &&
                playbackState != PlaybackState.STATE_NONE &&
                playbackState != PlaybackState.STATE_STOPPED
            if (isActiveTransition) {
                val prevKey = cachedKey!!
                return@synchronized ResolvedMetadata(
                    title = prevKey.title,
                    artist = prevKey.artist,
                    album = prevKey.album,
                    durationMs = prevKey.durationMs,
                    art = validCachedArt,
                )
            }
            cachedSessionToken = token
            cachedKey = null
            cachedArt = null
            return@synchronized ResolvedMetadata("", "", "", 0L, null)
        }

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val key = MetadataCacheKey(packageName, title, artist, album, durationMs)

        val rawArt: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val resolvedArt: Bitmap? = when {
            rawArt != null && !rawArt.isRecycled -> {
                if (
                    validCachedArt != null &&
                    (
                        rawArt === validCachedArt ||
                            (
                                rawArt.width == validCachedArt.width &&
                                    rawArt.height == validCachedArt.height &&
                                    rawArt.sameAs(validCachedArt)
                                )
                        )
                ) {
                    validCachedArt
                } else {
                    rawArt
                }
            }
            sameSession && cachedKey == key && title.isNotBlank() -> validCachedArt
            else -> null
        }

        cachedSessionToken = token
        cachedKey = key
        cachedArt = resolvedArt

        ResolvedMetadata(title, artist, album, durationMs, resolvedArt)
    }

    /**
     * Priorité : une session en lecture avec un titre, puis la session active courante si
     * elle reste pertinente (évite de basculer vers une autre session en pause), puis n'importe
     * quelle session avec titre, puis une session en lecture, puis la première.
     */
    private fun pickController(sessions: List<MediaController>): MediaController? {
        val playing = sessions.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        val activeToken = activeController?.sessionToken
        val currentActive = activeToken?.let { token ->
            sessions.firstOrNull { it.sessionToken == token }
        }
        return playing.firstOrNull(::isRelevant)
            ?: currentActive?.takeIf(::isRelevant)
            ?: sessions.firstOrNull(::isRelevant)
            ?: playing.firstOrNull()
            ?: currentActive
            ?: sessions.firstOrNull()
    }

    private fun isRelevant(controller: MediaController): Boolean {
        if (controller.packageName == "com.android.server.telecom") return false
        return !controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
    }

    private companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
