package com.studentlauncher.signals

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSignalsTest {

    private val self = "com.studentlauncher"
    private fun meta(
        pkg: String = "com.example.chat",
        category: String? = Notification.CATEGORY_MESSAGE,
        ongoing: Boolean = false,
        summary: Boolean = false,
        important: Boolean = false
    ) = NotificationMeta(pkg, category, postTime = 1_000L, ongoing = ongoing, summary = summary, important = important)

    @Test
    fun ownNotificationsNeverCount() {
        assertFalse(isCountableSignal(meta(pkg = self), self))
        assertTrue(isCountableSignal(meta(pkg = "com.example.chat"), self))
    }

    @Test
    fun ongoingWorkNeverCounts() {
        assertFalse(isCountableSignal(meta(ongoing = true), self))
    }

    @Test
    fun backgroundNoiseIsIgnored() {
        for (c in listOf(
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_ALARM
        )) {
            assertFalse("category $c should be ignored", isCountableSignal(meta(category = c), self))
        }
    }

    @Test
    fun messagesSocialAndUnknownCategoriesCount() {
        assertTrue(isCountableSignal(meta(category = Notification.CATEGORY_MESSAGE), self))
        assertTrue(isCountableSignal(meta(category = Notification.CATEGORY_SOCIAL), self))
        assertTrue(isCountableSignal(meta(category = null), self))
    }

    @Test
    fun bucketsComeFromMetadataOnly() {
        assertEquals(SignalKind.Digest, signalKind(meta(summary = true)))
        assertEquals(SignalKind.Message, signalKind(meta(category = Notification.CATEGORY_MESSAGE)))
        assertEquals(SignalKind.Message, signalKind(meta(category = Notification.CATEGORY_EMAIL)))
        assertEquals(SignalKind.Other, signalKind(meta(category = Notification.CATEGORY_SOCIAL)))
    }

    @Test
    fun aBurstOfMessagesFiresAtMostOneReactionPerGap() {
        val last = 10_000L
        assertFalse(shouldReact(last, last + 1_999L))
        assertTrue(shouldReact(last, last + 2_000L))
        assertTrue(shouldReact(last, last + 1_000L, minGapMs = 1_000L))
    }

    @Test
    fun tallyCountsTotalAndRollingWindow() {
        val tally = SignalTally(windowMs = 3_600_000L)
        tally.record(0L)
        tally.record(1_000L)
        tally.record(3_600_000L)
        assertEquals(3, tally.total)
        // The first stamp is exactly one window old, so it falls out.
        assertEquals(2, tally.countInWindow(3_600_000L))
    }

    @Test
    fun tallyResetClearsEverything() {
        val tally = SignalTally(windowMs = 3_600_000L)
        tally.record(0L)
        tally.reset()
        assertEquals(0, tally.total)
        assertEquals(0, tally.countInWindow(0L))
    }

    @Test
    fun storeFansOutOnlyCountableSignals() {
        var seen = 0
        val listener: (NotificationMeta) -> Unit = { seen++ }
        SignalStore.addListener(listener)
        try {
            SignalStore.record(meta(pkg = self), self, 0L)          // own app: ignored
            SignalStore.record(meta(ongoing = true), self, 0L)      // ongoing: ignored
            SignalStore.record(meta(), self, 1_000L)                // real signal
            SignalStore.record(meta(), self, 2_000L)                // real signal
        } finally {
            SignalStore.removeListener(listener)
        }
        val (total, inHour) = SignalStore.counts(2_000L)
        assertEquals(2, seen)
        assertEquals(2, total)
        assertEquals(2, inHour)
        SignalStore.reset()
    }
}
