package com.standbyme.app.data

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Réglages persistés dans les SharedPreferences "standbyme_settings" (mêmes clés qu'en 1.6.1). */
class SettingsRepository(app: Application) {

    private val prefs = app.getSharedPreferences("standbyme_settings", Context.MODE_PRIVATE)

    private val _waveIntensity = MutableStateFlow(prefs.getFloat(KEY_WAVE, 0.4f).coerceIn(0f, 1f))
    val waveIntensity: StateFlow<Float> = _waveIntensity.asStateFlow()

    private val _auraIntensity = MutableStateFlow(prefs.getFloat(KEY_AURA_INTENSITY, 0.6f).coerceIn(0f, 1f))
    val auraIntensity: StateFlow<Float> = _auraIntensity.asStateFlow()

    private val _auraEnabled = MutableStateFlow(prefs.getBoolean(KEY_AURA_ENABLED, true))
    val auraEnabled: StateFlow<Boolean> = _auraEnabled.asStateFlow()

    private val _auraUseAlbumColor = MutableStateFlow(prefs.getBoolean(KEY_AURA_ALBUM_COLOR, true))
    val auraUseAlbumColor: StateFlow<Boolean> = _auraUseAlbumColor.asStateFlow()

    private val _appleDesign = MutableStateFlow(prefs.getBoolean(KEY_APPLE_DESIGN, false))
    val appleDesign: StateFlow<Boolean> = _appleDesign.asStateFlow()

    fun setWaveIntensity(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _waveIntensity.value = v
        prefs.edit { putFloat(KEY_WAVE, v) }
    }

    fun setAuraIntensity(value: Float) {
        val v = value.coerceIn(0f, 1f)
        _auraIntensity.value = v
        prefs.edit { putFloat(KEY_AURA_INTENSITY, v) }
    }

    fun setAuraEnabled(value: Boolean) {
        _auraEnabled.value = value
        prefs.edit { putBoolean(KEY_AURA_ENABLED, value) }
    }

    fun setAuraUseAlbumColor(value: Boolean) {
        _auraUseAlbumColor.value = value
        prefs.edit { putBoolean(KEY_AURA_ALBUM_COLOR, value) }
    }

    fun setAppleDesign(value: Boolean) {
        _appleDesign.value = value
        prefs.edit { putBoolean(KEY_APPLE_DESIGN, value) }
    }

    private companion object {
        const val KEY_WAVE = "wave_intensity"
        const val KEY_AURA_INTENSITY = "aura_intensity"
        const val KEY_AURA_ENABLED = "aura_enabled"
        const val KEY_AURA_ALBUM_COLOR = "aura_use_album_color"
        const val KEY_APPLE_DESIGN = "apple_design"
    }
}
