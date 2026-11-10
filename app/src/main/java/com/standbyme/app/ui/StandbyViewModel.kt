package com.standbyme.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.standbyme.app.data.SettingsRepository
import com.standbyme.app.media.MediaSessionRepository
import com.standbyme.app.media.PlaybackUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StandbyViewModel(application: Application) : AndroidViewModel(application) {

    private val media = MediaSessionRepository(application)
    private val settings = SettingsRepository(application)

    val playback: StateFlow<PlaybackUiState> = media.playbackStates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackUiState.Empty)

    val waveIntensity = settings.waveIntensity
    val auraIntensity = settings.auraIntensity
    val auraEnabled = settings.auraEnabled
    val auraUseAlbumColor = settings.auraUseAlbumColor
    val appleDesign = settings.appleDesign

    fun hasNotificationAccess(): Boolean = media.hasNotificationAccess()

    fun togglePlayPause() = media.togglePlayPause()
    fun skipNext() = media.skipNext()
    fun skipPrevious() = media.skipPrevious()
    fun seekTo(positionMs: Long) = media.seekTo(positionMs)
    fun setVolume(fraction: Float) = media.setVolume(fraction)
    fun openOutputPicker() = media.openOutputPicker()

    fun setWaveIntensity(value: Float) = settings.setWaveIntensity(value)
    fun setAuraIntensity(value: Float) = settings.setAuraIntensity(value)
    fun setAuraEnabled(value: Boolean) = settings.setAuraEnabled(value)
    fun setAuraUseAlbumColor(value: Boolean) = settings.setAuraUseAlbumColor(value)
    fun setAppleDesign(value: Boolean) = settings.setAppleDesign(value)
}
