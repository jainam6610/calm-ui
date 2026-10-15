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
}
