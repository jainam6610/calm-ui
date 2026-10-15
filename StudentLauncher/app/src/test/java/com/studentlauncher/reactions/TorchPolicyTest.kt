package com.studentlauncher.reactions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorchPolicyTest {

    private val base = TorchConditions(
        optedIn = true,
        reducedMotion = false,
        powerSave = false,
        thermalOk = true,
        batteryPct = 80,
        charging = false,
        quietNow = false
    )

    private fun allowed(c: TorchConditions = base, now: Long = 1_000_000L, recent: List<Long> = emptyList(), lastEnd: Long = 0L, hour: Int = 0, session: Long = 0L) =
        torchBlockReason(c, now, recent, lastEnd, hour, session)

    @Test
    fun allowedWhenEverythingIsFine() {
        assertNull(allowed())
    }

    @Test
    fun torchIsOffUnlessOptedIn() {
        assertNotNull(allowed(base.copy(optedIn = false)))
    }

    @Test
    fun reducedMotionBlocks() {
        assertNotNull(allowed(base.copy(reducedMotion = true)))
    }

    @Test
    fun batterySaverBlocks() {
        assertNotNull(allowed(base.copy(powerSave = true)))
    }

    @Test
    fun warmDeviceBlocks() {
        assertNotNull(allowed(base.copy(thermalOk = false)))
    }

    @Test
    fun quietHoursAndDndBlock() {
        assertNotNull(allowed(base.copy(quietNow = true)))
    }

    @Test
    fun lowBatteryBlocksOnlyWhileNotCharging() {
        assertNotNull(allowed(base.copy(batteryPct = 15, charging = false)))
        assertNull(allowed(base.copy(batteryPct = 15, charging = true)))
        assertNull(allowed(base.copy(batteryPct = 16, charging = false)))
    }

    @Test
    fun hourlyLimitBlocks() {
        assertNotNull(allowed(hour = TorchLimits.MAX_PER_HOUR))
        assertNull(allowed(hour = TorchLimits.MAX_PER_HOUR - 1))
    }

    @Test
    fun coolDownBlocksForTenSeconds() {
        assertNotNull(allowed(now = 1_000_000L, lastEnd = 1_000_000L - TorchLimits.COOL_DOWN_MS + 1))
        assertNull(allowed(now = 1_000_000L, lastEnd = 1_000_000L - TorchLimits.COOL_DOWN_MS))
    }

    @Test
    fun atMostThreeFlashesInTheLastSecond() {
        val recent = listOf(999_500L, 999_700L, 999_900L)
        assertNotNull(allowed(now = 1_000_000L, recent = recent))
        assertNull(allowed(now = 1_000_000L, recent = listOf(999_500L, 999_900L)))
        // Old flashes fall out of the sliding window.
        assertNull(allowed(now = 1_000_000L, recent = listOf(998_900L, 998_950L, 998_990L)))
    }

    @Test
    fun sessionOnTimeIsCappedAtThreeSeconds() {
        assertNotNull(allowed(session = TorchLimits.MAX_TOTAL_ON_MS))
        assertNull(allowed(session = TorchLimits.MAX_TOTAL_ON_MS - 1))
    }

    @Test
    fun patternBlockedWhenItWouldExceedThePerSecondCap() {
        val recent = listOf(999_500L)
        assertNotNull(patternBlockReason(TorchPatterns.ALERT, 1_000_000L, recent, 0, 0))
        assertNull(patternBlockReason(TorchPatterns.DONE, 1_000_000L, recent, 0, 0))
    }

    @Test
    fun patternBlockedWhenItWouldExceedTheHourlyCap() {
        assertNotNull(patternBlockReason(TorchPatterns.DONE, 1_000_000L, emptyList(), TorchLimits.MAX_PER_HOUR - 1, 0))
    }

    @Test
    fun patternBlockedWhenItWouldExceedTheTotalOnTime() {
        assertNotNull(patternBlockReason(TorchPatterns.DONE, 1_000_000L, emptyList(), 0, TorchLimits.MAX_TOTAL_ON_MS - 100))
    }

    @Test
    fun emptyPatternIsRejected() {
        assertNotNull(patternBlockReason(emptyList(), 1_000_000L, emptyList(), 0, 0))
    }

    @Test
    fun alertPatternStaysInsideEveryHardLimit() {
        assertEquals(3, TorchPatterns.ALERT.size)
        assertTrue(TorchPatterns.ALERT.size <= TorchLimits.MAX_FLASHES_PER_SECOND)
        assertTrue(patternTotalOnMs(TorchPatterns.ALERT) <= TorchLimits.MAX_TOTAL_ON_MS)
        // 150 ms on + 250 ms gap = 2.5 Hz, i.e. three flashes inside a one second window.
        val span = TorchPatterns.ALERT.dropLast(1).sumOf { it.onMs + it.gapMs }
        assertTrue(span < TorchLimits.WINDOW_MS)
    }

    @Test
    fun donePatternIsTwoQuickFlashes() {
        assertEquals(2, TorchPatterns.DONE.size)
        assertTrue(patternTotalOnMs(TorchPatterns.DONE) <= TorchLimits.MAX_TOTAL_ON_MS)
    }

    @Test
    fun tapPatternIsReservedAndShort() {
        assertEquals(1, TorchPatterns.TAP.size)
        assertEquals(120L, TorchPatterns.TAP.first().onMs)
    }

    @Test
    fun flashesInWindowCountsOnlyRecentOnes() {
        assertEquals(2, flashesInWindow(listOf(0L, 500L, 2000L), nowMs = 1000L))
        assertEquals(0, flashesInWindow(listOf(0L, 500L), nowMs = 9000L))
    }
}
