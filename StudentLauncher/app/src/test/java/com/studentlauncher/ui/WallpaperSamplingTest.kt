package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperSamplingTest {

    @Test
    fun picksTheSmallestSampleThatStillCoversTheTarget() {
        // 2400 -> 1200 still covers 1080, 600 does not.
        assertEquals(2, Wallpapers.sampleSizeFor(2400, 1800, 1080))
        // 1000 -> 500 -> 250 all cover 250 exactly.
        assertEquals(4, Wallpapers.sampleSizeFor(1000, 1000, 250))
        // 250 does not cover 251.
        assertEquals(2, Wallpapers.sampleSizeFor(1000, 1000, 251))
    }

    @Test
    fun neverUpscalesBeyondTheSource() {
        assertEquals(1, Wallpapers.sampleSizeFor(1200, 800, 4000))
        assertEquals(1, Wallpapers.sampleSizeFor(1200, 800, 1200))
    }

    @Test
    fun usesTheLongEdgeOnly() {
        // Portrait source, landscape target: the long edge is what matters.
        assertEquals(2, Wallpapers.sampleSizeFor(3000, 1000, 1500))
    }

    @Test
    fun degenerateInputsAreSafe() {
        assertEquals(1, Wallpapers.sampleSizeFor(0, 0, 100))
        assertEquals(1, Wallpapers.sampleSizeFor(100, 100, 0))
        assertEquals(1, Wallpapers.sampleSizeFor(-5, 10, 100))
    }
}
