package com.standbyme.app.ui.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.standbyme.app.R

private val BluetoothTypes = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_HEARING_AID,
)
private val WiredTypes = setOf(
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_USB_HEADSET,
)

private fun currentOutputDevice(audio: AudioManager): AudioDeviceInfo? = try {
    audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .firstOrNull { it.type in BluetoothTypes || it.type in WiredTypes }
} catch (_: Exception) {
    null
}

/** Libellé court de la sortie (puce de la file d'attente) : « Bluetooth », « Haut-parleur »… */
fun shortOutputLabel(context: Context, isRemote: Boolean): String {
    if (isRemote) return context.getString(R.string.output_cast)
    val audio = context.getSystemService(AudioManager::class.java) ?: return "—"
    val device = currentOutputDevice(audio)
    return when {
        device?.type in BluetoothTypes -> context.getString(R.string.output_bluetooth)
        device?.type in WiredTypes -> context.getString(R.string.output_wired)
        else -> context.getString(R.string.output_speaker)
    }
}

/** Libellé long du design Apple : « Écoute sur AirPods Pro », « Écoute sur Pixel 8 »… */
fun listeningOnLabel(context: Context, isRemote: Boolean): String {
    if (isRemote) return context.getString(R.string.listening_on_cast)
    val audio = context.getSystemService(AudioManager::class.java)
    val device = audio?.let(::currentOutputDevice)
    val name = when (device?.type) {
        in WiredTypes -> context.getString(R.string.output_wired)
        in BluetoothTypes -> device?.productName?.toString()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.output_bluetooth)
        else -> null
    } ?: Settings.Global.getString(context.contentResolver, "device_name")?.takeIf { it.isNotBlank() }
        ?: Build.MODEL
    return context.getString(R.string.listening_on, name)
}
