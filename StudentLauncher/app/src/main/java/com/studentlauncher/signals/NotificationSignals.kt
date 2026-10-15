package com.studentlauncher.signals

import android.app.Notification

/**
 * Notification metadata **only**.
 *
 * This type deliberately has no field for a title, body, extras, people, icons or any content.
 * The service that builds it reads only the package name, category, post time and two flags,
 * so there is nowhere for content to go even by mistake.
 */
data class NotificationMeta(
    val pkg: String,
    val category: String?,
    val postTime: Long,
    val ongoing: Boolean,
    val summary: Boolean,
    val important: Boolean
)

/** Coarse buckets. The launcher shows state and counts, never who or what. */
enum class SignalKind { Message, Digest, Other }

/**
 * Categories that are never a person trying to reach you: ongoing work, media and system noise.
 * [Notification.CATEGORY_CALL] is excluded because calls are handled by Tier 0.
 */
private val IGNORED_CATEGORIES = setOf(
    Notification.CATEGORY_CALL,
    Notification.CATEGORY_SERVICE,
    Notification.CATEGORY_PROGRESS,
    Notification.CATEGORY_NAVIGATION,
    Notification.CATEGORY_TRANSPORT,
    Notification.CATEGORY_SYSTEM,
    Notification.CATEGORY_ALARM,
    Notification.CATEGORY_EVENT,
    Notification.CATEGORY_REMINDER
)

/** Pure: should this notification become a signal at all? */
fun isCountableSignal(meta: NotificationMeta, selfPkg: String): Boolean {
    if (meta.pkg == selfPkg) return false
    if (meta.ongoing) return false
    val c = meta.category
    return c == null || c !in IGNORED_CATEGORIES
}

/** Pure: coarse bucket from the category and the group-summary flag only. */
fun signalKind(meta: NotificationMeta): SignalKind = when {
    meta.summary -> SignalKind.Digest
    meta.category == Notification.CATEGORY_MESSAGE || meta.category == Notification.CATEGORY_EMAIL ->
        SignalKind.Message
    else -> SignalKind.Other
}

/**
 * Pure rate limiter. A burst of messages (a busy group chat) must not fire a reaction per
 * notification, so at most one reaction lands per [minGapMs].
 */
fun shouldReact(lastReactMs: Long, nowMs: Long, minGapMs: Long = 2_000L): Boolean =
    nowMs - lastReactMs >= minGapMs

/**
 * In-memory tally. Nothing is persisted and nothing is transmitted; it exists only so the
 * launcher can show a count and so the reaction budget stays bounded.
 */
class SignalTally(private val windowMs: Long = 3_600_000L) {
    private val stamps = ArrayDeque<Long>()

    /** Signals seen since the process started. */
    var total: Int = 0
        private set

    fun record(nowMs: Long) {
        total++
        stamps.addLast(nowMs)
        prune(nowMs)
    }

    /** Signals inside the rolling window ending at [nowMs]. */
    fun countInWindow(nowMs: Long): Int {
        prune(nowMs)
        return stamps.size
    }

    fun reset() {
        stamps.clear()
        total = 0
    }

    private fun prune(nowMs: Long) {
        while (stamps.isNotEmpty() && nowMs - stamps.first() >= windowMs) stamps.removeFirst()
    }
}

/**
 * Process-wide, in-memory fan-out from [SignalsListenerService] to whoever is listening
 * (the launcher ViewModel, and only while it is started).
 */
object SignalStore {
    private val lock = Any()
    private val listeners = mutableListOf<(NotificationMeta) -> Unit>()
    private val tally = SignalTally()

    fun record(meta: NotificationMeta, selfPkg: String, nowMs: Long) {
        if (!isCountableSignal(meta, selfPkg)) return
        val targets: List<(NotificationMeta) -> Unit>
        synchronized(lock) {
            tally.record(nowMs)
            targets = listeners.toList()
        }
        // Listeners are cheap and must never be able to break the service.
        targets.forEach { runCatching { it(meta) } }
    }

    fun addListener(l: (NotificationMeta) -> Unit) {
        synchronized(lock) { if (!listeners.contains(l)) listeners.add(l) }
    }

    fun removeListener(l: (NotificationMeta) -> Unit) {
        synchronized(lock) { listeners.remove(l) }
    }

    fun counts(nowMs: Long): Pair<Int, Int> = synchronized(lock) {
        tally.total to tally.countInWindow(nowMs)
    }

    fun reset() = synchronized(lock) { tally.reset() }
}
