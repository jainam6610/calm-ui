package com.studentlauncher.detox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetoxPolicyTest {

    private val HOUR = DetoxPolicy.HOUR_MS
    private val DAY = DetoxPolicy.DAY_MS
    private val WEEK = DetoxPolicy.WEEK_MS
    private val MONTH = DetoxPolicy.MONTH_MS

    private fun session(
        startedWall: Long = 1_000_000_000_000L,
        lengthMs: Long = HOUR,
        allowed: Set<String> = emptySet(),
        savedMono: Long = 60_000L,
        savedLeft: Long = lengthMs
    ) = DetoxPolicy.Session(startedWall, lengthMs, allowed, savedMono, savedLeft)

    @Test
    fun oneMonthIsThirtyPlainDays() {
        assertEquals(30 * 24 * 3_600_000L, MONTH)
        assertEquals(30 * DAY, MONTH)
        assertEquals(7 * DAY, WEEK)
        assertEquals(24 * HOUR, DAY)
        assertEquals(4, DetoxPolicy.durations.size)
    }

    @Test
    fun emptyAllowListStillAllowsTheAlwaysSetAndNothingElse() {
        val always = setOf("com.android.dialer", "camera", "clock")
        val s = session(allowed = emptySet())
        assertTrue(DetoxPolicy.isAllowed("com.android.dialer", s, always))
        assertTrue(DetoxPolicy.isAllowed("camera", s, always))
        assertTrue(DetoxPolicy.isAllowed("clock", s, always))
        assertFalse(DetoxPolicy.isAllowed("com.whatsapp", s, always))
    }

    @Test
    fun theAllowListBeatsNothingAndTheAlwaysSetBeatsEverything() {
        val s = session(allowed = setOf("com.whatsapp"))
        assertTrue(DetoxPolicy.isAllowed("com.whatsapp", s, emptySet()))
        assertFalse(DetoxPolicy.isAllowed("com.spotify", s, emptySet()))
    }

    @Test
    fun calculatorsAndTimersAreAlwaysAvailable() {
        val always = DetoxPolicy.alwaysPackages
        assertTrue("com.google.android.calculator" in always)
        assertTrue("com.android.calculator2" in always)
        assertTrue("com.android.deskclock" in always)
        assertTrue("com.sec.android.app.clockpackage" in always)
        val s = session(allowed = emptySet())
        assertTrue(DetoxPolicy.isAllowed("com.google.android.calculator", s, always))
        assertTrue(DetoxPolicy.isAllowed("com.android.deskclock", s, always))
    }

    @Test
    fun theAlwaysSetIsIncludedInWhatAStartSendsHome() {
        val allowed = DetoxPolicy.startAllowed(
            selected = setOf("com.whatsapp", "com.spotify"),
            always = setOf("dialer", "camera", "clock")
        )
        assertEquals(setOf("com.whatsapp", "com.spotify", "dialer", "camera", "clock"), allowed)
    }

    @Test
    fun sameBootRemainingTicksDownOnTheMonotonicClock() {
        // started 14:00 wall, snapshot taken 10 real minutes into a 1 hour session
        val offset = 600_000L
        val s = session(lengthMs = HOUR, savedMono = offset, savedLeft = 50 * 60_000L)
        assertEquals(50 * 60_000L, DetoxPolicy.remainingMs(s, offset, 0L))
        assertEquals(49 * 60_000L, DetoxPolicy.remainingMs(s, offset + 60_000L, 0L))
        assertEquals(0L, DetoxPolicy.remainingMs(s, offset + 51 * 60_000L, 0L))
    }

    @Test
    fun sameBootRemainingIsCappedAtTheSessionLength() {
        val s = session(lengthMs = HOUR, savedMono = 0L, savedLeft = HOUR)
        assertEquals(HOUR, DetoxPolicy.remainingMs(s, -5_000L, 0L))
    }

    @Test
    fun afterARebootTheWallClockDeadlineDecides() {
        val startedWall = 10L * DAY
        val s = session(startedWall = startedWall, lengthMs = HOUR, savedMono = 30_000_000L, savedLeft = HOUR)
        // device reboots (elapsedRealtime now small), clock is honest: 20 minutes into the session
        val nowWall = startedWall + 20 * 60_000L
        assertEquals(40 * 60_000L, DetoxPolicy.remainingMs(s, 120_000L, nowWall))
    }

    @Test
    fun aClockRolledBackAfterRebootCannotExtendTheSession() {
        val startedWall = 10L * DAY
        val s = session(startedWall = startedWall, lengthMs = WEEK, savedMono = 30_000_000L, savedLeft = 3 * DAY)
        // user drags the clock two days back: wall "deadline" looks far away now,
        // but the last known remaining (3d) caps it.
        val rolledBack = startedWall - 2 * DAY
        assertEquals(3 * DAY, DetoxPolicy.remainingMs(s, 120_000L, rolledBack))
    }

    @Test
    fun aClockMovedForwardEndsTheSessionEarly() {
        val startedWall = 10L * DAY
        val s = session(startedWall = startedWall, lengthMs = WEEK, savedMono = 30_000_000L, savedLeft = 3 * DAY)
        val jumped = startedWall + WEEK + 500L
        assertEquals(0L, DetoxPolicy.remainingMs(s, 120_000L, jumped))
    }

    @Test
    fun snapshotMovesTheAnchorsAndStaysConsistent() {
        val offset = 600_000L
        val s = session(lengthMs = HOUR, savedMono = offset, savedLeft = 50 * 60_000L)
        val later = DetoxPolicy.snapshot(s, offset + 90_000L, 0L)
        assertEquals(offset + 90_000L, later.savedMono)
        // 50:00 minus the 1:30 that passed while the snapshot was taken
        assertEquals(2_910_000L, later.savedLeft)
        assertEquals(2_910_000L, DetoxPolicy.remainingMs(later, offset + 90_000L, 0L))
    }

    @Test
    fun countdownLabelsMatchTheCalmStyle() {
        assertEquals("3d 4h", DetoxPolicy.label(3 * DAY + 4 * HOUR + 30 * 60_000L + 999L))
        assertEquals("1d 0h", DetoxPolicy.label(DAY))
        assertEquals("1:00:00", DetoxPolicy.label(HOUR))
        assertEquals("59:59", DetoxPolicy.label(HOUR - 1))
        assertEquals("12:08", DetoxPolicy.label(12 * 60_000L + 8_000L))
        assertEquals("00:00", DetoxPolicy.label(0L))
    }

    @Test
    fun summaryRoundTripsAndRejectsGarbage() {
        val s = DetoxPolicy.Summary(1_000_000_000_000L, MONTH, 3, true)
        val raw = DetoxPolicy.encodeSummary(s)
        assertEquals(s, DetoxPolicy.decodeSummary(raw))
        assertEquals(s.copy(endedByTimer = false), DetoxPolicy.decodeSummary(DetoxPolicy.encodeSummary(s.copy(endedByTimer = false))))
        assertEquals(null, DetoxPolicy.decodeSummary(""))
        assertEquals(null, DetoxPolicy.decodeSummary("x|1|2|true"))
        assertEquals(null, DetoxPolicy.decodeSummary("1|1|1|often"))
    }

    @Test
    fun clockAndCalculatorSetsStayInsideTheAlwaysSet() {
        assertTrue(DetoxPolicy.alwaysPackages.containsAll(DetoxPolicy.clockPackages))
        assertTrue(DetoxPolicy.alwaysPackages.containsAll(DetoxPolicy.calculatorPackages))
    }

    @Test
    fun leaveWaitIsTenMinutesAndTheMonotonicClockWins() {
        val mono = 1_000_000L
        val wall = 5_000_000L
        assertEquals(10 * 60_000L, DetoxPolicy.RELEASE_WAIT_MS)
        assertEquals(10 * 60_000L, DetoxPolicy.waitLeftMs(mono, wall, mono, wall))
        assertEquals(9 * 60_000L, DetoxPolicy.waitLeftMs(mono, wall, mono + 60_000L, wall + 60_000L))
        // Moving the system clock forward cannot skip the wait.
        assertEquals(9 * 60_000L, DetoxPolicy.waitLeftMs(mono, wall, mono + 60_000L, wall + 3 * HOUR))
        assertEquals(0L, DetoxPolicy.waitLeftMs(mono, wall, mono + 10 * 60_000L, wall))
        assertEquals(0L, DetoxPolicy.waitLeftMs(0L, 0L, mono, wall))
    }

    @Test
    fun afterARebootTheLeaveWaitFallsBackToTheWallClockWithinTheCap() {
        val wall = 5_000_000L
        // elapsedRealtime restarted below the stored start, so the wall anchor takes over.
        assertEquals(4 * 60_000L, DetoxPolicy.waitLeftMs(9_000_000L, wall, 1_000L, wall + 6 * 60_000L))
        // A clock dragged backwards cannot make the wait longer than ten minutes.
        assertEquals(10 * 60_000L, DetoxPolicy.waitLeftMs(9_000_000L, wall, 1_000L, wall - HOUR))
    }
}
