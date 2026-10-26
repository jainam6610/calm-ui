package com.studentlauncher.detox

/**
 * Pure rules for Detoxify: the four session lengths, the allow-list, and the remaining-time
 * maths that keeps a session honest across reboots and clock changes.
 *
 * No Android types in this file, so everything is unit tested on the JVM. Blocking scope note:
 * a launcher can only police launches that go through it (first-run plan, "try not to allow" but
 * without any special permission), so these rules are deliberately about time and permission to
 * launch — never about scanning the phone.
 */
object DetoxPolicy {

    /** The four lengths offered on the start sheet. One month is a plain thirty days. */
    const val HOUR_MS: Long = 3_600_000L
    const val DAY_MS: Long = 24 * HOUR_MS
    const val WEEK_MS: Long = 7 * DAY_MS
    const val MONTH_MS: Long = 30 * DAY_MS

    val durations: List<Pair<Long, String>> = listOf(
        HOUR_MS to "1 hour",
        DAY_MS to "1 day",
        WEEK_MS to "1 week",
        MONTH_MS to "1 month"
    )

    /** Early exit: right PIN first, then a ten-minute cooling-off the user can still cancel. */
    const val RELEASE_WAIT_MS: Long = 10 * 60_000L

    /**
     * Packages that every session keeps usable: phone, camera, clocks/timers and calculators.
     * Vendors rename these often, so this is the static fallback; the launcher also asks the
     * package manager for the system's own handlers. Calm's tools use the clock/calculator
     * subsets, so the two can never drift apart.
     */
    val phonePackages: Set<String> = setOf(
        "com.android.phone", "com.android.systemui",
        "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer"
    )

    val cameraPackages: Set<String> = setOf(
        "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera",
        "com.sec.android.app.camera", "com.samsung.android.camera"
    )

    val clockPackages: Set<String> = setOf(
        "com.android.deskclock", "com.google.android.deskclock", "com.sec.android.app.clockpackage",
        "com.coloros.alarmclock", "com.oplus.alarmclock", "com.oneplus.deskclock",
        "com.transsion.clock", "com.vivo.alarmclock", "com.huawei.deskclock", "com.motorola.clock"
    )

    val calculatorPackages: Set<String> = setOf(
        "com.google.android.calculator", "com.android.calculator", "com.android.calculator2",
        "com.android.calculator3", "com.miui.calculator", "com.sec.android.app.popupcalculator",
        "com.coloros.calculator", "com.oneplus.calculator", "com.transsion.calculator",
        "com.vivo.calculator", "com.huawei.calculator"
    )

    val alwaysPackages: Set<String> =
        phonePackages + cameraPackages + clockPackages + calculatorPackages

    /**
     * A running session. The pair of anchors keeps it reboot-proof: [startedWall]+[lengthMs] is
     * an absolute wall-clock deadline, and [savedMono]/[savedLeft] snapshot the remaining time on
     * the monotonic clock so a user-set system clock cannot extend it.
     */
    data class Session(
        val startedWall: Long,
        val lengthMs: Long,
        val allowed: Set<String>,
        val savedMono: Long,
        val savedLeft: Long
    )

    /** A finished session, for the summary sheet. */
    data class Summary(
        val startedWall: Long,
        val lengthMs: Long,
        val allowedCount: Int,
        val endedByTimer: Boolean
    )

    /** One pipe-joined line; the store is the only thing that reads or writes it. */
    fun encodeSummary(s: Summary): String =
        "${s.startedWall}|${s.lengthMs}|${s.allowedCount}|${s.endedByTimer}"

    fun decodeSummary(raw: String): Summary? {
        if (raw.isBlank()) return null
        val p = raw.split('|')
        if (p.size != 4) return null
        val started = p[0].toLongOrNull() ?: return null
        val len = p[1].toLongOrNull() ?: return null
        val count = p[2].toIntOrNull() ?: return null
        val byTimer = p[3].toBooleanStrictOrNull() ?: return null
        return Summary(started, len, count, byTimer)
    }

    /** [always] holds the resolved phone/camera/clock packages; it beats the allow-list. */
    fun isAllowed(pkg: String, session: Session, always: Set<String>): Boolean =
        pkg in session.allowed || pkg in always

    /**
     * Remaining ms. Same boot (the monotonic clock kept moving, [nowMono] >= [savedMono]):
     * trust it alone, so moving the system clock cannot shorten or extend anything. After a
     * reboot ([nowMono] < [savedMono]): fall back to the wall-clock deadline, clamped by the
     * last known remaining time so a clock set backwards cannot buy extra time.
     */
    fun remainingMs(s: Session, nowMono: Long, nowWall: Long): Long {
        val left = if (nowMono >= s.savedMono) {
            s.savedLeft - (nowMono - s.savedMono)
        } else {
            minOf(s.startedWall + s.lengthMs - nowWall, s.savedLeft)
        }
        return left.coerceIn(0L, s.lengthMs)
    }

    /** Refreshes the anchors; call before writing state back to prefs. */
    fun snapshot(s: Session, nowMono: Long, nowWall: Long): Session =
        s.copy(savedMono = nowMono, savedLeft = remainingMs(s, nowMono, nowWall))

    /** Countdown label: "3d 4h" for a month, "4:12:08" for hours, "12:08" inside an hour. */
    fun label(leftMs: Long): String = when {
        leftMs >= DAY_MS -> "${leftMs / DAY_MS}d ${(leftMs % DAY_MS) / HOUR_MS}h"
        leftMs >= HOUR_MS ->
            "${leftMs / HOUR_MS}:${pct((leftMs % HOUR_MS) / 60_000L)}:${pct((leftMs % 60_000L) / 1000L)}"
        else -> "${pct(leftMs / 60_000L)}:${pct((leftMs % 60_000L) / 1000L)}"
    }

    private fun pct(v: Long): String = v.toString().padStart(2, '0')

    /**
     * Remaining ms of the leave wait that starts at [startMono]/[startWall]. The monotonic anchor
     * wins on the same boot so moving the system clock cannot skip the wait; after a reboot
     * ([nowMono] < [startMono]) the wall anchor is the fallback.
     */
    fun waitLeftMs(startMono: Long, startWall: Long, nowMono: Long, nowWall: Long): Long {
        if (startMono == 0L && startWall == 0L) return 0L
        val left = if (startMono != 0L && nowMono >= startMono) {
            RELEASE_WAIT_MS - (nowMono - startMono)
        } else {
            RELEASE_WAIT_MS - (nowWall - startWall)
        }
        return left.coerceIn(0L, RELEASE_WAIT_MS)
    }

    /**
     * Can a session start these [selected] packages given [always]? The sheet pins the always
     * set as locked-on, so the start state is the visible ticks plus it.
     */
    fun startAllowed(selected: Set<String>, always: Set<String>): Set<String> = selected + always
}
