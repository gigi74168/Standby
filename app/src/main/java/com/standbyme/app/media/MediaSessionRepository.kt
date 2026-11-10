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
        if (controller == null) return PlaybackUiState(volumeFraction = systemVolumeFraction())

        val metadata = controller.metadata
        val playback = controller.playbackState

        val appLabel = try {
            val pm = app.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(controller.packageName, 0)).toString()
        } catch (_: Exception) {
            controller.packageName.orEmpty()
        }

        val art: Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

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
            packageName = controller.packageName.orEmpty(),
            sessionToken = controller.sessionToken,
            appLabel = appLabel,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            albumArt = art,
            isPlaying = playback?.state == PlaybackState.STATE_PLAYING,
            positionMs = playback?.position?.coerceAtLeast(0L) ?: 0L,
            positionAnchorRealtimeMs = SystemClock.elapsedRealtime(),
            playbackSpeed = playback?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            queue = queue,
            volumeFraction = volume,
            isRemoteVolume = info.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE,
            canAdjustVolume = info.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_FIXED,
        )
    }

    /**
     * Priorité : une session en lecture avec un titre, puis n'importe quelle session avec titre,
     * puis une session en lecture, puis la première. Les appels téléphoniques sont ignorés.
     */
    private fun pickController(sessions: List<MediaController>): MediaController? {
        val playing = sessions.filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        return playing.firstOrNull(::isRelevant)
            ?: sessions.firstOrNull(::isRelevant)
            ?: playing.firstOrNull()
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
