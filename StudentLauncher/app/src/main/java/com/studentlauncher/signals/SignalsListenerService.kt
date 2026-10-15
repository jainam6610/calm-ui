package com.studentlauncher.signals

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Tier 1 signals. A [NotificationListenerService] that reads **metadata only**.
 *
 * It looks at the package name, category, post time, the group-summary flag and the ranking
 * importance. It never touches `Notification.extras`, the title, the body, people, icons or any
 * other content, and [SignalStore] keeps counts in memory only — nothing is written to disk or
 * sent anywhere.
 *
 * The user has to grant notification access in system Settings; that is a special access, not a
 * `uses-permission`, so the app's permission surface does not change.
 */
class SignalsListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        val n = sbn ?: return
        val note = n.notification ?: return

        // Ranking importance is metadata (how loud the notification is), never its content.
        var important = false
        if (rankingMap != null) {
            val ranking = Ranking()
            if (rankingMap.getRanking(n.key, ranking)) {
                important = ranking.importance >= NotificationManager.IMPORTANCE_HIGH
            }
        }

        val meta = NotificationMeta(
            pkg = n.packageName ?: return,
            category = note.category,
            postTime = n.postTime,
            ongoing = n.isOngoing,
            summary = (note.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            important = important
        )
        SignalStore.record(meta, packageName, SystemClock.elapsedRealtime())
    }
}

/** True when the user has granted notification access to this app. */
fun notificationAccessGranted(ctx: Context): Boolean {
    val flat = runCatching {
        Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
    }.getOrNull() ?: return false
    return flat.split(':').any { it.contains(ctx.packageName) }
}
