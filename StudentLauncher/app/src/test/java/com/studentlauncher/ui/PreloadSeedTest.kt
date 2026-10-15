package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreloadSeedTest {

    @Test
    fun maxShuffleIsFive() {
        assertEquals(5, Wallpapers.MAX_SHUFFLE)
    }

    @Test
    fun slotForDayCoversPreloadCount() {
        val count = 2
        val slot0 = Wallpapers.slotForDay(count, 0L)
        val slot1 = Wallpapers.slotForDay(count, 1L)
        val slot2 = Wallpapers.slotForDay(count, 2L)
        assertEquals(0, slot0)
        assertEquals(1, slot1)
        assertEquals(0, slot2)
    }
}
