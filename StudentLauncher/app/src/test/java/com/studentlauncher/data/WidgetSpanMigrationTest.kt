package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(listOf(WidgetSpan(4, 2), WidgetSpan(4, 2)), spans)
    }

    @Test fun pairedHalvesStayHalfWidth() {
        val spans = migrateWidgetSpans(listOf(seed(half = true, height = 1), seed(half = true, height = 1)))
        assertEquals(listOf(WidgetSpan(2, 2), WidgetSpan(2, 2)), spans)
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
        assertEquals(listOf(WidgetSpan(3, 4), WidgetSpan(4, 2)), spans)
    }

    @Test fun oversizedSpansClampToSixRows() {
        val spans = migrateWidgetSpans(listOf(seed(rowSpan = 9), seed(colSpan = 1, rowSpan = 6)))
        assertEquals(listOf(WidgetSpan(4, 6), WidgetSpan(2, 6)), spans)
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

    @Test fun gridPacksPairsAndFillsTheHole() {
        val slots = packWidgetGrid(
            listOf(
                WidgetSpan(2, 2),
                WidgetSpan(2, 2),
                WidgetSpan(4, 2),
                WidgetSpan(2, 2)
            )
        )
        assertEquals(listOf(0, 0, 2, 4), slots.map { it.row })
        assertEquals(listOf(0, 2, 0, 0), slots.map { it.col })
    }

    @Test fun gridNeverOverlapsAndStaysInsideColumns() {
        val spans = listOf(
            WidgetSpan(4, 6), WidgetSpan(2, 2), WidgetSpan(3, 3),
            WidgetSpan(2, 4), WidgetSpan(4, 2), WidgetSpan(2, 5), WidgetSpan(3, 2)
        )
        val slots = packWidgetGrid(spans, pinnedIndex = 3, pinnedRow = 1, pinnedCol = 2)
        val used = HashSet<Int>()
        slots.forEach { s ->
            assertTrue(s.col >= 0)
            assertTrue(s.col + s.colSpan <= WIDGET_COLUMNS)
            for (r in s.row until s.row + s.rowSpan) {
                for (c in s.col until s.col + s.colSpan) {
                    assertTrue("cell $r/$c reused", used.add(r * WIDGET_COLUMNS + c))
                }
            }
        }
    }

    @Test fun pinnedTileKeepsItsCellWhileOthersReflow() {
        val spans = listOf(WidgetSpan(2, 2), WidgetSpan(2, 2), WidgetSpan(2, 2), WidgetSpan(2, 2))
        val slots = packWidgetGrid(spans, pinnedIndex = 2, pinnedRow = 2, pinnedCol = 2)
        assertEquals(2, slots[2].row)
        assertEquals(2, slots[2].col)
        assertEquals(2, slots[3].row)
    }

    @Test fun growingToFullWidthStaysInTheSameRow() {
        val spans = listOf(WidgetSpan(2, 2), WidgetSpan(4, 2), WidgetSpan(2, 2))
        val slots = packWidgetGrid(spans, pinnedIndex = 1, pinnedRow = 1, pinnedCol = 2)
        assertEquals(1, slots[1].row)
        assertEquals(0, slots[1].col)
    }

    @Test fun shrinkingAtilePullsLaterTilesUp() {
        val before = packWidgetGrid(listOf(WidgetSpan(2, 4), WidgetSpan(2, 2), WidgetSpan(4, 2)))
        val after = packWidgetGrid(listOf(WidgetSpan(2, 2), WidgetSpan(2, 2), WidgetSpan(4, 2)))
        assertEquals(4, before[2].row)
        assertEquals(2, after[2].row)
    }
}
