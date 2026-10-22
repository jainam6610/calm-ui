package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSpanMigrationTest {

    private fun seed(
        half: Boolean = false,
        height: Int = 2,
        colSpan: Int? = null,
        rowSpan: Int? = null
    ) = SpanSeed(
        explicitColSpan = colSpan,
        explicitRowSpan = rowSpan,
        legacyHalf = half,
        legacyHeight = height
    )

    @Test fun loneHalfKeepsRenderedFullWidth() {
        val spans = migrateWidgetSpans(listOf(seed(half = true, height = 1), seed(height = 2)))
        assertEquals(listOf(WidgetSpan(4, 1), WidgetSpan(4, 2)), spans)
    }

    @Test fun pairedHalvesStayHalfWidth() {
        val spans = migrateWidgetSpans(listOf(seed(half = true, height = 1), seed(half = true, height = 1)))
        assertEquals(listOf(WidgetSpan(2, 1), WidgetSpan(2, 1)), spans)
    }

    @Test fun oddThirdHalfBecomesFullWidth() {
        val spans = migrateWidgetSpans(
            listOf(seed(half = true), seed(half = true), seed(half = true))
        )
        assertEquals(listOf(WidgetSpan(2, 2), WidgetSpan(2, 2), WidgetSpan(4, 2)), spans)
    }

    @Test fun fullWidgetBetweenHalvesBreaksPairing() {
        val spans = migrateWidgetSpans(
            listOf(seed(half = true), seed(), seed(half = true))
        )
        assertEquals(listOf(WidgetSpan(4, 2), WidgetSpan(4, 2), WidgetSpan(4, 2)), spans)
    }

    @Test fun explicitSpansWinAndAreClamped() {
        val spans = migrateWidgetSpans(
            listOf(seed(half = true, colSpan = 3, rowSpan = 4), seed(half = true, colSpan = 9, rowSpan = 0))
        )
        assertEquals(listOf(WidgetSpan(3, 4), WidgetSpan(4, 1)), spans)
    }

    @Test fun flowRowsPackBySpanAndLeaveHoles() {
        assertEquals(listOf(listOf(0, 1), listOf(2)), flowRowsOfSpans(listOf(2, 2, 4)))
        assertEquals(listOf(listOf(0), listOf(1, 2)), flowRowsOfSpans(listOf(4, 2, 2)))
        assertEquals(listOf(listOf(0), listOf(1), listOf(2)), flowRowsOfSpans(listOf(3, 2, 4)))
        assertEquals(listOf(listOf(0, 1), listOf(2)), flowRowsOfSpans(listOf(2, 2, 2)))
    }

    @Test fun rowUsedColumnsIsCapped() {
        val row = listOf(
            WidgetItem("a", "x", colSpan = 3),
            WidgetItem("b", "x", colSpan = 3)
        )
        assertEquals(4, rowUsedColumns(row))
    }
}
