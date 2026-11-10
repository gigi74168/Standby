package com.standbyme.app.media

import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service vide côté logique : sa seule présence (et l'autorisation accordée par l'utilisateur)
 * permet à [MediaSessionRepository] de lire les sessions multimédia actives.
 */
class StandbyNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _connected.value = false
    }

    companion object {
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()
    }
}
