package com.mehra.hide

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifBlocker : NotificationListenerService() {

    companion object {
        var instance: NotifBlocker? = null
    }

    override fun onListenerConnected() {
        instance = this
        sweep()
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val p = Prefs(this)
        if (p.on && sbn.packageName in p.hiddenApps) cancelNotification(sbn.key)
    }

    /** Jo notification pehle se aayi hui hain unhe bhi hata do */
    fun sweep() {
        val p = Prefs(this)
        if (!p.on) return
        activeNotifications?.forEach {
            if (it.packageName in p.hiddenApps) cancelNotification(it.key)
        }
    }
}
