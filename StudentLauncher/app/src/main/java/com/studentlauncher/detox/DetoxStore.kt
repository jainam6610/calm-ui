package com.studentlauncher.detox

import android.content.SharedPreferences

/**
 * Serializes the running Detoxify session into the launcher's own prefs so a reboot, a process
 * kill or a force-stop cannot lose it. Nothing here decides anything — all rules live in
 * [DetoxPolicy] where they are unit tested; this file only moves bytes in and out.
 */
class DetoxStore(private val prefs: SharedPreferences) {

    /** The running session, or null when Detoxify is off. */
    fun load(): DetoxPolicy.Session? {
        if (!prefs.getBoolean(K_ACTIVE, false)) return null
        val started = prefs.getLong(K_STARTED, 0L)
        val len = prefs.getLong(K_LEN, 0L)
        if (started == 0L || len == 0L) return null
        val allowed = prefs.getString(K_ALLOWED, "").orEmpty()
            .split(',').filter { it.isNotBlank() }.toSet()
        return DetoxPolicy.Session(
            started, len, allowed,
            savedMono = prefs.getLong(K_SAVED_MONO, 0L),
            savedLeft = prefs.getLong(K_SAVED_LEFT, len)
        )
    }

    fun save(s: DetoxPolicy.Session) {
        prefs.edit()
            .putBoolean(K_ACTIVE, true)
            .putLong(K_STARTED, s.startedWall)
            .putLong(K_LEN, s.lengthMs)
            .putString(K_ALLOWED, s.allowed.joinToString(","))
            .putLong(K_SAVED_MONO, s.savedMono)
            .putLong(K_SAVED_LEFT, s.savedLeft)
            .apply()
    }

    /** Ends the session; keeps the summary (if any) and cancels a pending early exit. */
    fun clear() {
        prefs.edit()
            .putBoolean(K_ACTIVE, false)
            .putLong(K_RELEASE, 0L)
            .remove(K_STARTED).remove(K_LEN).remove(K_ALLOWED).remove(K_SAVED_MONO).remove(K_SAVED_LEFT)
            .apply()
    }

    /** Wall-clock deadline of a pending early exit, or 0 when none is pending. */
    var releaseAt: Long
        get() = prefs.getLong(K_RELEASE, 0L)
        set(value) { prefs.edit().putLong(K_RELEASE, value).apply() }

    /**
     * The last finished session, pipe-joined for the summary sheet
     * (startedWall|lengthMs|allowedCount|endedByTimer). Empty string = none pending.
     */
    var summary: String
        get() = prefs.getString(K_SUMMARY, "").orEmpty()
        set(value) { prefs.edit().putString(K_SUMMARY, value).apply() }

    private companion object {
        const val K_ACTIVE = "detox_active"
        const val K_STARTED = "detox_started_wall"
        const val K_LEN = "detox_len_ms"
        const val K_ALLOWED = "detox_allowed"
        const val K_SAVED_MONO = "detox_saved_mono"
        const val K_SAVED_LEFT = "detox_saved_left"
        const val K_RELEASE = "detox_release_at"
        const val K_SUMMARY = "detox_summary"
    }
}
