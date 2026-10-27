package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DefaultWidgetsTest {

    /** The first-run home is one borderless three-row Dot clock, with date and quote lines. */
    @Test
    fun firstRunHomeIsOneBorderlessClock() {
        val widgets = defaultWidgets()
        assertEquals(1, widgets.size)
        val clock = widgets.single()
        assertEquals(WT.CLOCK, clock.type)
        assertEquals(WidgetBg.None, clock.bg)
        assertEquals(WIDGET_COLUMNS, clock.colSpan)
        assertEquals(3, clock.rowSpan)
        assertEquals("auto", clock.tone)
        assertEquals("Dot", clock.get("style"))
        assertEquals("sys", clock.get("fmt"))
        assertEquals("1", clock.get("date"))
        assertEquals("1", clock.get("quote"))
    }

    @Test
    fun defaultMatchesTheShippedLook() {
        val expected = WidgetItem(
            id = "id",
            type = WT.CLOCK,
            bg = WidgetBg.None,
            colSpan = WIDGET_COLUMNS,
            rowSpan = 3,
            size = 0,
            tone = "auto",
            cfg = mapOf("style" to "Dot", "fmt" to "sys", "date" to "1", "quote" to "1")
        )
        assertEquals(expected, defaultWidgets().single().copy(id = "id"))
    }

    @Test
    fun everyDefaultGetsItsOwnId() {
        val a = defaultWidgets().single().id
        val b = defaultWidgets().single().id
        assertNotEquals(a, b)
    }
}
