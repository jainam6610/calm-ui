package com.calm.launcher

import android.content.SharedPreferences

/**
 * One row on Calm's home screen.
 *
 * A default slot is tied to a function (messages, maps, music, ...) and resolves to the first
 * installed package in [pkgs] until the user points it somewhere else. Extra slots have no default
 * and stay empty until an app is picked. [friction] rows pause before opening; [counts] rows are
 * measured against the daily limit.
 */
data class CalmSlot(
    val id: String,
    val label: String,
    val friction: Boolean,
    val counts: Boolean,
    val pkgs: List<String>
)

/** Kept so Calm's activity can keep using its short `Slot` name. */
typealias Slot = CalmSlot

/**
 * The one place that defines Calm's home apps. Shared by the Calm activity and by the normal
 * launcher's Calm settings page, so both always show the same slots and the same prefs keys.
 */
object CalmConfig {
    const val PREFS = "calm"
    const val COUNT_KEY = "grid_count"
    const val COUNT_DEFAULT = 9
    const val COUNT_LARGE = 15

    // Seven default slots + phone and camera = the 9-app home.
    val appSlots = listOf(
        CalmSlot("messages", "messages", false, false, listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms")),
        CalmSlot("whatsapp", "whatsapp", true, true, listOf("com.whatsapp", "com.whatsapp.w4b")),
        CalmSlot("maps", "maps", false, false, listOf("com.google.android.apps.maps")),
        CalmSlot("pay", "pay", false, false, listOf("com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm")),
        CalmSlot("clock", "clock", false, false, listOf("com.google.android.deskclock", "com.sec.android.app.clockpackage", "com.android.deskclock")),
        CalmSlot("browser", "browser", true, true, emptyList()),
        CalmSlot("music", "music", true, false, listOf("com.spotify.music", "com.google.android.apps.youtube.music", "dev.brahmkshatriya.echo", "com.apple.android.music"))
    )

    // The 15-app home adds these six before phone and camera.
    val extraSlots = (1..6).map { CalmSlot("extra$it", "app $it", false, false, emptyList()) }

    val tailSlots = listOf(
        CalmSlot("phone", "phone", false, false, emptyList()),
        CalmSlot("camera", "camera", false, false, emptyList())
    )

    fun normalizeCount(raw: String?): Int =
        raw?.toIntOrNull()?.takeIf { it == COUNT_DEFAULT || it == COUNT_LARGE } ?: COUNT_DEFAULT

    fun slots(count: Int): List<CalmSlot> =
        appSlots + (if (count == COUNT_LARGE) extraSlots else emptyList()) + tailSlots

    fun isExtra(slot: CalmSlot): Boolean = slot.id.startsWith("extra")

    fun isFixed(slot: CalmSlot): Boolean =
        slot.id == "browser" || slot.id == "phone" || slot.id == "camera"

    // ---- slot prefs shared with the normal launcher ----
    // A loosening change (a default row pointed at another app) is parked under "p_slot_x"
    // until "pat_slot_x" passes, then promoted to "slot_x". Exactly what Calm's getS/setS do.

    const val LOOSEN_WAIT_MS: Long = 24 * 3_600_000L

    /** The app currently chosen for [id], promoting a due pending change first. */
    fun readSlot(prefs: SharedPreferences, id: String, now: Long = System.currentTimeMillis()): String {
        val key = "slot_$id"
        val at = prefs.getLong("pat_$key", 0L)
        if (at != 0L && now >= at) {
            val v = prefs.getString("p_$key", "") ?: ""
            prefs.edit().putString(key, v).putLong("pat_$key", 0L).remove("p_$key").apply()
            return v
        }
        return prefs.getString(key, "") ?: ""
    }

    /** True while a loosening change for [id] is still cooling off. */
    fun slotChangePending(prefs: SharedPreferences, id: String, now: Long = System.currentTimeMillis()): Boolean {
        val at = prefs.getLong("pat_slot_$id", 0L)
        return at != 0L && now < at
    }

    /** Writes [pkg]; a [loosening] change waits 24 hours. Returns true when it is only scheduled. */
    fun writeSlot(prefs: SharedPreferences, id: String, pkg: String, loosening: Boolean,
                  now: Long = System.currentTimeMillis()): Boolean {
        val key = "slot_$id"
        return if (loosening) {
            // Picking the same app again must not restart the 24 hours.
            if (prefs.getString("p_$key", "") == pkg && prefs.getLong("pat_$key", 0L) != 0L) {
                true
            } else {
                prefs.edit().putString("p_$key", pkg)
                    .putLong("pat_$key", now + LOOSEN_WAIT_MS).apply()
                true
            }
        } else {
            prefs.edit().putString(key, pkg).putLong("pat_$key", 0L)
                .remove("p_$key").apply()
            false
        }
    }
}
