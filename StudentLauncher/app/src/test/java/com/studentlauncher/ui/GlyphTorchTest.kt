package com.studentlauncher.ui

import com.studentlauncher.reactions.TorchLimits
import com.studentlauncher.reactions.TorchPatterns
import com.studentlauncher.reactions.patternTotalOnMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The matrix-to-torch contract: only Done and priority signals may touch the camera light. */
class GlyphTorchTest {

    @Test
    fun doneReactionMapsToTheTwoFlashPattern() {
        assertEquals(TorchPatterns.DONE, GlyphTorch.pattern(GlyphModes.DONE))
    }

    @Test
    fun priorityReactionMapsToTheThreeFlashAlert() {
        assertEquals(TorchPatterns.ALERT, GlyphTorch.pattern(GlyphModes.PULSE))
    }

    @Test
    fun touchAndAmbientModesNeverReachTheTorch() {
        val never = listOf(
            GlyphModes.CLOCK,
            GlyphModes.QUIET,
            GlyphModes.FOCUS,
            GlyphModes.TASKS,
            GlyphModes.OFF,
            GlyphModes.LOW,
            GlyphModes.CHARGING,
            GlyphModes.BREATHE,
            GlyphModes.TAP,
            GlyphModes.WAVE,
            GlyphModes.SPIRAL,
            GlyphModes.ORBIT,
            GlyphModes.DRIFT,
            GlyphModes.SPARK,
            GlyphModes.CUSTOM
        )
        never.forEach { assertNull(it, GlyphTorch.pattern(it)) }
    }

    @Test
    fun mappedPatternsStayInsideTheHardLimits() {
        listOf(GlyphModes.DONE, GlyphModes.PULSE).forEach { mode ->
            val p = GlyphTorch.pattern(mode)!!
            assertTrue(mode, p.size <= TorchLimits.MAX_FLASHES_PER_SECOND)
            assertTrue(mode, patternTotalOnMs(p) <= TorchLimits.MAX_TOTAL_ON_MS)
            // Three flashes must fit inside the one-second window.
            val span = p.dropLast(1).sumOf { it.onMs + it.gapMs }
            assertTrue(mode, span < TorchLimits.WINDOW_MS)
        }
    }
}
