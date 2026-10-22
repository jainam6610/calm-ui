package com.studentlauncher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutMathTest {

    @Test
    fun preferenceAlwaysWins() {
        assertTrue(isCompactHome(true, 900f))
        assertTrue(isCompactHome(true, 640f))
    }

    @Test
    fun shortScreensAreCompactWithoutThePreference() {
        assertTrue(isCompactHome(false, 639f))
        assertFalse(isCompactHome(false, 640f))
        assertFalse(isCompactHome(false, 900f))
    }
}
