package com.calm.launcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Silences non-essential notifications and saves them for the digest. Calls, messages and alarms stay live. */
class NotifListener : NotificationListenerService() {
    private val live = listOf("dialer", "telecom", "incallui", "messaging", "mms", "deskclock", "clockpackage", "com.android.phone", "systemui")
    private val liveCats = listOf(Notification.CATEGORY_CALL, Notification.CATEGORY_ALARM, Notification.CATEGORY_NAVIGATION,
        Notification.CATEGORY_TRANSPORT, Notification.CATEGORY_PROGRESS, Notification.CATEGORY_SERVICE, Notification.CATEGORY_REMINDER)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val p = getSharedPreferences("calm", MODE_PRIVATE)
        val pkg = sbn.packageName
        if (pkg == packageName || sbn.isOngoing || !sbn.isClearable) return
        val n = sbn.notification
        if (n.category in liveCats) return
        if (live.any { pkg.contains(it) }) return
        if (pkg.startsWith("com.whatsapp") && p.getBoolean("wa_live", true)) return
        val e = n.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (title.isEmpty() && text.isEmpty()) return
        val line = listOf(pkg, title, text, System.currentTimeMillis().toString()).joinToString("\u0001").replace("\n", " ")
        val old = p.getString("digest", "")!!.split("\n").filter { it.isNotBlank() }
        p.edit().putString("digest", (old + line).takeLast(100).joinToString("\n")).apply()
        cancelNotification(sbn.key)
    }
}
