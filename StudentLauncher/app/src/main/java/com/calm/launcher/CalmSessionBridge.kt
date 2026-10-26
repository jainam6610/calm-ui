package com.calm.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Handoff from the Calm leave flow to the launcher that owns the Detoxify session.
 *
 * Both activities run in the same process, so a direct callback is the reliable path: Calm ends
 * the session before it finishes, and the launcher never re-opens Calm on its way back. The
 * shared-prefs flag stays as the fallback for a cold start (process killed while Calm was open),
 * where no callback is registered yet.
 */
object CalmSessionBridge {
    const val PREFS = "calm"

    /** Written by the leave flow, read and cleared by the launcher. */
    const val KEY_LEFT = "left_session"

    private val main = Handler(Looper.getMainLooper())
    private var listener: (() -> Unit)? = null

    /** The launcher registers here; the newest ViewModel wins. */
    fun listen(onLeft: (() -> Unit)?) {
        listener = onLeft
    }

    /**
     * Call after the wait, phrase and reason have all passed. `commit()` rather than `apply()`:
     * the launcher may read the flag on its very next start, even if the process dies right away.
     */
    fun notifyLeft(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LEFT, true).commit()
        listener?.let { main.post(it) }
    }

    /** Reads and clears the fallback flag. True when a leave was waiting. */
    fun consumeLeft(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_LEFT, false)) return false
        p.edit().putBoolean(KEY_LEFT, false).commit()
        return true
    }

    /** Drops any stale signal, so an old leave cannot end a new session. */
    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LEFT).commit()
    }
}
