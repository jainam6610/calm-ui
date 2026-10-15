package com.studentlauncher.ui

import com.studentlauncher.data.WT
import com.studentlauncher.data.newWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStyleTest {

    @Test
    fun defaultStyleIsDot() {
        val w = newWidget(WT.CLOCK)
        assertEquals("Dot", w.get("style"))
    }

    @Test
    fun allNothingStylesAreValid() {
        val nothingStyles = listOf("c1", "c2", "c3", "c4", "c5", "Dot", "Glyph Arc", "Micrographics", "Day Name", "Segment LCD")
        val w = newWidget(WT.CLOCK)
        nothingStyles.forEach { styleKey ->
            val configured = w.put("style", styleKey)
            assertEquals(styleKey, configured.get("style"))
        }
    }
}
