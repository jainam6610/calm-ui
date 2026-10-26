package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphMatrixSpecTest {

    @Test
    fun maskHasExactly489Cells() {
        assertEquals(489, GlyphMatrixSpec.count)
    }

    @Test
    fun maskIsSymmetric() {
        for (x in 0 until GlyphMatrixSpec.GRID) {
            for (y in 0 until GlyphMatrixSpec.GRID) {
                assertTrue(GlyphMatrixSpec.contains(x, y) == GlyphMatrixSpec.contains(GlyphMatrixSpec.GRID - 1 - x, GlyphMatrixSpec.GRID - 1 - y))
            }
        }
    }

    @Test
    fun centreAndAxisExtremesAreInsideWhileCornersAreOutside() {
        assertTrue(GlyphMatrixSpec.contains(12, 12))
        assertTrue(GlyphMatrixSpec.contains(12, 0))
        assertTrue(GlyphMatrixSpec.contains(0, 12))
        assertFalse(GlyphMatrixSpec.contains(0, 0))
        assertFalse(GlyphMatrixSpec.contains(24, 24))
    }

    @Test
    fun cellsArrayMatchesTheMask() {
        val set = HashSet<Int>()
        val c = GlyphMatrixSpec.cells
        var i = 0
        while (i < c.size) {
            set.add(c[i + 1] * GlyphMatrixSpec.GRID + c[i])
            i += 2
        }
        for (x in 0 until GlyphMatrixSpec.GRID) {
            for (y in 0 until GlyphMatrixSpec.GRID) {
                assertEquals(GlyphMatrixSpec.contains(x, y), (y * GlyphMatrixSpec.GRID + x) in set)
            }
        }
    }

    @Test
    fun staticReactionsReportNoFrameLoop() {
        assertEquals(0, GlyphModes.durationMs(GlyphModes.CLOCK))
        assertEquals(0, GlyphModes.durationMs(GlyphModes.LOW))
        assertTrue(GlyphModes.durationMs(GlyphModes.PULSE) > 0)
    }

    @Test
    fun modeCycleCoversEveryModeWithoutRepeating() {
        var mode = GlyphModes.ALL.first()
        val seen = mutableSetOf(mode)
        repeat(GlyphModes.ALL.size - 1) {
            mode = GlyphModes.next(mode)
            seen.add(mode)
        }
        assertEquals(GlyphModes.ALL.toSet(), seen)
    }

    @Test
    fun clockReactionLightsOnlyTheClockCells() {
        val lit = timeCells(10, 30)
        assertTrue(lit.isNotEmpty())
        val ctx = GlyphReaction(lit, 100, false)
        for (y in 0 until GlyphMatrixSpec.GRID) {
            for (x in 0 until GlyphMatrixSpec.GRID) {
                val level = reactionLevel(GlyphModes.CLOCK, x, y, 0f, ctx)
                val idx = y * GlyphMatrixSpec.GRID + x
                if (idx in lit) assertTrue(level >= 0.9f) else assertTrue(level < 0.2f)
            }
        }
    }

    @Test
    fun catalogCoversEveryModeExactlyOnce() {
        assertEquals(GlyphModes.ALL.size, GlyphModes.catalog.size)
        assertEquals(GlyphModes.ALL.toSet(), GlyphModes.catalog.map { it.id }.toSet())
        GlyphModes.catalog.forEach { m ->
            assertTrue(m.id, m.label.isNotBlank())
            assertTrue(m.id, m.summary.isNotBlank())
            assertTrue(m.id, m.category in GlyphModes.categories)
        }
        assertEquals(GlyphModes.catalog.size, GlyphModes.categories.sumOf { GlyphModes.byCategory(it).size })
    }

    @Test
    fun everyModeStaysWithinBrightnessBounds() {
        val ctx = GlyphReaction(emptySet(), 72, true, progress = 0.66f)
        for (mode in GlyphModes.ALL) {
            for (t in floatArrayOf(0f, 0.33f, 0.66f, 1f)) {
                for (y in 0 until GlyphMatrixSpec.GRID) {
                    for (x in 0 until GlyphMatrixSpec.GRID) {
                        val level = reactionLevel(mode, x, y, t, ctx)
                        assertTrue("$mode at $x,$y", level in 0f..1f)
                    }
                }
            }
        }
    }

    @Test
    fun focusRingTracksTimerProgress() {
        val empty = GlyphReaction(emptySet(), 100, false, progress = 0f)
        val full = GlyphReaction(emptySet(), 100, false, progress = 1f)
        // At 0% the ring sits inside; at 100% it reaches the outer cells and lights them.
        assertTrue(reactionLevel(GlyphModes.FOCUS, 12, 9, 0f, empty) >= 0.5f)
        assertTrue(reactionLevel(GlyphModes.FOCUS, 12, 9, 0f, full) < 0.5f)
        assertTrue(reactionLevel(GlyphModes.FOCUS, 24, 12, 0f, full) >= 0.5f)
    }

    @Test
    fun taskArcGrowsClockwiseFromTheTop() {
        val quarter = GlyphReaction(emptySet(), 100, false, progress = 0.25f)
        val threeQuarters = GlyphReaction(emptySet(), 100, false, progress = 0.75f)
        // The top of the ring is angle 0; the bottom is angle 0.5.
        assertTrue(reactionLevel(GlyphModes.TASKS, 12, 2, 0f, quarter) >= 0.9f)
        assertTrue(reactionLevel(GlyphModes.TASKS, 12, 22, 0f, quarter) < 0.2f)
        assertTrue(reactionLevel(GlyphModes.TASKS, 12, 22, 0f, threeQuarters) >= 0.9f)
    }

    @Test
    fun quietAndOffAreStatic() {
        assertEquals(0, GlyphModes.durationMs(GlyphModes.QUIET))
        assertEquals(0, GlyphModes.durationMs(GlyphModes.FOCUS))
        assertEquals(0, GlyphModes.durationMs(GlyphModes.TASKS))
        assertEquals(0, GlyphModes.durationMs(GlyphModes.OFF))
        val ctx = GlyphReaction(emptySet(), 100, false)
        assertTrue(reactionLevel(GlyphModes.QUIET, 12, 12, 0f, ctx) > 0.5f)
        assertTrue(reactionLevel(GlyphModes.QUIET, 20, 12, 0f, ctx) < 0.1f)
        for (y in 0 until GlyphMatrixSpec.GRID) {
            for (x in 0 until GlyphMatrixSpec.GRID) {
                assertEquals(0f, reactionLevel(GlyphModes.OFF, x, y, 0f, ctx), 0f)
            }
        }
    }

    @Test
    fun creativeBurstsAreFinite() {
        assertTrue(GlyphModes.durationMs(GlyphModes.ORBIT) in 1..6000)
        assertTrue(GlyphModes.durationMs(GlyphModes.DRIFT) in 1..6000)
        assertTrue(GlyphModes.durationMs(GlyphModes.SPARK) in 1..6000)
        assertTrue(GlyphModes.ambient.all { GlyphModes.durationMs(it) == 0 })
    }

    @Test
    fun customArtRoundTripsAndDropsCellsOutsideTheMask() {
        val inside = 12 * GlyphMatrixSpec.GRID + 12
        val outside = 0 // (0, 0) is outside the circular mask
        val encoded = encodeGlyphArt(setOf(inside, outside))
        assertEquals(GlyphMatrixSpec.GRID * GlyphMatrixSpec.GRID, encoded.length)
        assertEquals(setOf(inside), decodeGlyphArt(encoded))
    }

    @Test
    fun customArtRejectsMalformedStorage() {
        assertTrue(decodeGlyphArt(null).isEmpty())
        assertTrue(decodeGlyphArt("").isEmpty())
        assertTrue(decodeGlyphArt("10101").isEmpty())
    }

    @Test
    fun customModeLightsOnlySavedCells() {
        val saved = setOf(12 * GlyphMatrixSpec.GRID + 12, 12 * GlyphMatrixSpec.GRID + 8)
        val ctx = GlyphReaction(emptySet(), 100, false, custom = saved)
        saved.forEach { idx ->
            val x = idx % GlyphMatrixSpec.GRID
            val y = idx / GlyphMatrixSpec.GRID
            assertEquals(1f, reactionLevel(GlyphModes.CUSTOM, x, y, 0f, ctx), 0f)
        }
        assertEquals(0.04f, reactionLevel(GlyphModes.CUSTOM, 20, 12, 0f, ctx), 0f)
    }
}
