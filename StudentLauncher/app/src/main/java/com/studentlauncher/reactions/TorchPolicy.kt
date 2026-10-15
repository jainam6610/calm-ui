package com.studentlauncher.reactions

/**
 * One torch pulse: the light is on for [onMs], then off for [gapMs] before the next pulse.
 * Every pattern below is well inside the safety limits (see [TorchLimits]).
 */
data class Flash(val onMs: Long, val gapMs: Long)

/**
 * The only patterns the launcher is allowed to play. Touch, breathe and low-battery states
 * never reach the torch; [TAP] is reserved for the manual Test button.
 */
object TorchPatterns {
    /** Manual Test button only. */
    val TAP = listOf(Flash(120, 0))

    /** A finished focus session. */
    val DONE = listOf(Flash(120, 160), Flash(120, 0))

    /** Priority / alert: three flashes at 2.5 Hz, inside the one-second window. */
    val ALERT = listOf(Flash(150, 250), Flash(150, 250), Flash(150, 0))
}

/** Everything that can veto a torch reaction. Cheap to read or passed in. */
data class TorchConditions(
    val optedIn: Boolean,
    val reducedMotion: Boolean,
    val powerSave: Boolean,
    val thermalOk: Boolean,
    val batteryPct: Int,
    val charging: Boolean,
    val quietNow: Boolean
)

/** Hard limits. These are safety rails, not preferences. */
object TorchLimits {
    const val MAX_FLASHES_PER_SECOND = 3
    const val WINDOW_MS = 1000L
    const val MAX_TOTAL_ON_MS = 3000L
    const val COOL_DOWN_MS = 10_000L
    const val MAX_PER_HOUR = 12
    const val HOUR_MS = 3_600_000L
    const val LOW_BATTERY_PCT = 15

    /** Watchdog: the light is forced off this long after a session starts, no matter what. */
    const val WATCHDOG_MS = 3000L
}

/** How many flashes happened within the sliding window ending at [nowMs]. */
fun flashesInWindow(times: List<Long>, nowMs: Long, windowMs: Long = TorchLimits.WINDOW_MS): Int =
    times.count { nowMs - it < windowMs }

/** Total time the light would be on for a whole pattern. */
fun patternTotalOnMs(pattern: List<Flash>): Long = pattern.sumOf { it.onMs }

/**
 * Pure gate evaluated before any light is shown. Returns null when a session may start,
 * otherwise a human readable reason to surface in settings.
 */
fun torchBlockReason(
    conditions: TorchConditions,
    nowMs: Long,
    recentFlashes: List<Long>,
    lastSessionEndMs: Long,
    flashesThisHour: Int,
    sessionOnMs: Long
): String? = when {
    !conditions.optedIn -> "Torch reactions are off"
    conditions.reducedMotion -> "Reduced motion is on"
    conditions.powerSave -> "Battery saver is on"
    !conditions.thermalOk -> "The device is running warm"
    conditions.quietNow -> "Quiet hours or Do Not Disturb"
    conditions.batteryPct <= TorchLimits.LOW_BATTERY_PCT && !conditions.charging ->
        "Battery is at ${conditions.batteryPct}% and not charging"
    flashesThisHour >= TorchLimits.MAX_PER_HOUR -> "Hourly limit reached (${TorchLimits.MAX_PER_HOUR})"
    sessionOnMs >= TorchLimits.MAX_TOTAL_ON_MS -> "Session limit reached"
    lastSessionEndMs > 0 && nowMs - lastSessionEndMs < TorchLimits.COOL_DOWN_MS ->
        "Cooling down for ${(TorchLimits.COOL_DOWN_MS - (nowMs - lastSessionEndMs) + 999) / 1000}s"
    flashesInWindow(recentFlashes, nowMs) >= TorchLimits.MAX_FLASHES_PER_SECOND ->
        "${TorchLimits.MAX_FLASHES_PER_SECOND} flashes per second is the limit"
    else -> null
}

/** Gate a specific pattern on top of the current session state. */
fun patternBlockReason(
    pattern: List<Flash>,
    nowMs: Long,
    recentFlashes: List<Long>,
    flashesThisHour: Int,
    sessionOnMs: Long
): String? = when {
    pattern.isEmpty() -> "Nothing to flash"
    pattern.size + flashesInWindow(recentFlashes, nowMs) > TorchLimits.MAX_FLASHES_PER_SECOND ->
        "Pattern would exceed ${TorchLimits.MAX_FLASHES_PER_SECOND} flashes per second"
    patternTotalOnMs(pattern) + sessionOnMs > TorchLimits.MAX_TOTAL_ON_MS ->
        "Pattern would exceed ${TorchLimits.MAX_TOTAL_ON_MS / 1000}s of light"
    flashesThisHour + pattern.size > TorchLimits.MAX_PER_HOUR ->
        "Pattern would exceed the hourly limit"
    else -> null
}
