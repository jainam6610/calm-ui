package com.studentlauncher.data

/** One removable item and where it came from, valid until [expiresAt] (wall clock ms). */
data class UndoEntry<T>(
    val item: T,
    val index: Int,
    val listKey: String,
    val expiresAt: Long
)

/**
 * Tiny single-slot undo buffer. Deliberately not tied to Android: the ViewModel supplies the
 * clock and owns the side effects (widget host release, persistence, UI notice).
 */
class UndoBuffer<T>(private val windowMs: Long = 5_000L) {

    var pending: UndoEntry<T>? = null
        private set

    fun stage(item: T, index: Int, listKey: String, now: Long): UndoEntry<T> {
        val entry = UndoEntry(item, index, listKey, now + windowMs)
        pending = entry
        return entry
    }

    /** Re-arm a pending entry loaded from disk with its original expiry. */
    fun restore(entry: UndoEntry<T>) {
        pending = entry
    }

    /** Consume the pending entry if it is still inside its window. */
    fun undo(now: Long): UndoEntry<T>? {
        val entry = pending ?: return null
        if (now > entry.expiresAt) {
            pending = null
            return null
        }
        pending = null
        return entry
    }

    /** Consume the pending entry only once its window has passed. */
    fun takeExpired(now: Long): UndoEntry<T>? {
        val entry = pending ?: return null
        if (now <= entry.expiresAt) return null
        pending = null
        return entry
    }

    fun clear() {
        pending = null
    }
}
