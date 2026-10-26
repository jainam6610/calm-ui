package com.studentlauncher.detox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.studentlauncher.MainActivity
import com.studentlauncher.R

/**
 * Keeps a Detoxify session honest while another app is on screen.
 *
 * The launcher and Calm only tick while they are visible, so a session could otherwise outlive
 * its deadline whenever the user stays inside an allowed app. This foreground service owns the
 * deadline instead: it wakes at most once an hour, saves a fresh snapshot, and ends the session
 * (store + summary + notification) the moment the countdown reaches zero.
 *
 * It is started only while the launcher is in the foreground, so the Android 12+ background
 * start rules are satisfied. No new data leaves the phone: the notification is local.
 */
class DetoxGuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val store by lazy { DetoxStore(getSharedPreferences(PREFS, MODE_PRIVATE)) }
    private var foreground = false

    private val tick = object : Runnable {
        override fun run() {
            if (check()) handler.postDelayed(this, nextDelayMs())
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val s = store.load()
        if (s == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(s)
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    /** One wake-up. Returns true when the service should keep going. */
    private fun check(): Boolean {
        val s = store.load()
        if (s == null) {
            stopSelf()
            return false
        }
        val nowMono = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val left = DetoxPolicy.remainingMs(s, nowMono, nowWall)
        if (left <= 0L) {
            finish(s)
            return false
        }
        // Keep the reboot fallback current: it may be hours before the next wake.
        store.save(DetoxPolicy.snapshot(s, nowMono, nowWall))
        notifyProgress(left)
        return true
    }

    /** Wake exactly at the deadline once it is close, otherwise refresh at most once an hour. */
    private fun nextDelayMs(): Long {
        val s = store.load() ?: return 0L
        val left = DetoxPolicy.remainingMs(s, SystemClock.elapsedRealtime(), System.currentTimeMillis())
        return left.coerceIn(1_000L, MAX_SLEEP_MS)
    }

    private fun finish(s: DetoxPolicy.Session) {
        val summary = DetoxPolicy.Summary(s.startedWall, s.lengthMs, s.allowed.size, true)
        store.clear()
        store.summary = DetoxPolicy.encodeSummary(summary)
        // Calm keeps its own copy of the countdown; drop it so a stale screen cannot keep ticking.
        // session_id stays behind on purpose: it is what makes an old launch intent recognisable
        // and stops a finished session from being restored.
        getSharedPreferences(CALM_PREFS, MODE_PRIVATE).edit()
            .remove("session_end_wall").remove("session_end_mono").remove("session_left")
            .remove("leave_at").remove("leave_mono").apply()
        postDone(summary)
        stopForeground(STOP_FOREGROUND_REMOVE)
        foreground = false
        stopSelf()
    }

    private fun startInForeground(s: DetoxPolicy.Session) {
        val left = DetoxPolicy.remainingMs(s, SystemClock.elapsedRealtime(), System.currentTimeMillis())
        val n = progressNotification(left)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        foreground = true
    }

    private fun ensureChannel(id: String, name: String, importance: Int) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(
                NotificationChannel(id, name, importance).apply {
                    setShowBadge(false)
                    if (importance <= NotificationManager.IMPORTANCE_LOW) {
                        setSound(null, null)
                    }
                }
            )
        }
    }

    private fun progressNotification(left: Long): Notification {
        ensureChannel(CHANNEL, "Detoxify countdown", NotificationManager.IMPORTANCE_LOW)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Detoxify")
            .setContentText("${DetoxPolicy.label(left)} left")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // The system ticks the countdown itself, so the text stays right between our hourly
            // wake-ups and no extra alarm is needed.
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() + left)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openLauncher())
            .build()
    }

    private fun notifyProgress(left: Long) {
        if (!foreground) return
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, progressNotification(left))
    }

    private fun postDone(summary: DetoxPolicy.Summary) {
        ensureChannel(DONE_CHANNEL, "Detoxify", NotificationManager.IMPORTANCE_DEFAULT)
        val n = NotificationCompat.Builder(this, DONE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(if (summary.endedByTimer) "Detoxify complete" else "Detoxify ended")
            .setContentText("Your phone is back to normal.")
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openLauncher())
            .build()
        getSystemService(NotificationManager::class.java)?.notify(DONE_ID, n)
    }

    private fun openLauncher(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val PREFS = "student_launcher"
        private const val CALM_PREFS = "calm"
        private const val CHANNEL = "detox_guard"
        private const val DONE_CHANNEL = "detox"
        private const val NOTIF_ID = 4344
        private const val DONE_ID = 4343
        private const val MAX_SLEEP_MS = 60 * 60_000L

        /** Starts (or restarts) the guard for the session already saved in [DetoxStore]. */
        fun start(ctx: Context) {
            runCatching {
                ContextCompat.startForegroundService(ctx, Intent(ctx, DetoxGuardService::class.java))
            }
        }

        fun stop(ctx: Context) {
            val i = Intent().setClass(ctx, DetoxGuardService::class.java)
            ctx.stopService(i)
        }
    }
}
