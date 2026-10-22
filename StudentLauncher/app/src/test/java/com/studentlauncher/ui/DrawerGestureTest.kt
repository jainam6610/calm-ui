package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerGestureTest {

    @Test
    fun progressTracksTheFingerAndClamps() {
        assertEquals(0f, drawerProgress(0f, 180f), 0.001f)
        assertEquals(0.5f, drawerProgress(-90f, 180f), 0.001f)
        assertEquals(1f, drawerProgress(-400f, 180f), 0.001f)
        assertEquals(0f, drawerProgress(40f, 180f), 0.001f)
    }

    @Test
    fun progressNeverDividesByZero() {
        assertEquals(1f, drawerProgress(-10f, 0f), 0.001f)
    }

    @Test
    fun releaseOpensPastTheDistanceThreshold() {
        assertTrue(drawerShouldOpen(0.35f, 0f))
        assertFalse(drawerShouldOpen(0.34f, 0f))
    }

    @Test
    fun deliberateUpwardFlingOpensBelowTheDistanceThreshold() {
        assertTrue(drawerShouldOpen(0.12f, -1_200f))
        assertFalse(drawerShouldOpen(0.12f, -400f))
    }

    @Test
    fun aTinyDragIsNotAFling() {
        assertFalse(drawerShouldOpen(0.04f, -2_000f))
    }

    @Test
    fun horizontalDragIsLeftToThePager() {
        assertFalse(drawerGestureOwns(80f, -20f, 8f))
        assertTrue(drawerGestureOwns(10f, -30f, 8f))
        assertFalse(drawerGestureOwns(0f, 30f, 8f))
        assertFalse(drawerGestureOwns(0f, -4f, 8f))
    }

    @Test
    fun startZoneIsTheBandAboveTheGestureInset() {
        assertTrue(drawerStartAllowed(1_900f, 2_000f, 48f, 180f))
        assertFalse(drawerStartAllowed(1_600f, 2_000f, 48f, 180f))
    }
}
