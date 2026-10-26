package com.studentlauncher.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSpansTest {

    @Test fun fullFrameHeightsUseOneRowUnit() {
        assertEquals(144f, widgetFrameHeight(2, compact = false).value)
        assertEquals(221f, widgetFrameHeight(3, compact = false).value)
        assertEquals(298f, widgetFrameHeight(4, compact = false).value)
        assertEquals(375f, widgetFrameHeight(5, compact = false).value)
        assertEquals(452f, widgetFrameHeight(6, compact = false).value)
    }

    @Test fun compactFrameHeightsKeepTheSameRhythm() {
        assertEquals(114f, widgetFrameHeight(2, compact = true).value)
        assertEquals(176f, widgetFrameHeight(3, compact = true).value)
        assertEquals(238f, widgetFrameHeight(4, compact = true).value)
        assertEquals(300f, widgetFrameHeight(5, compact = true).value)
        assertEquals(362f, widgetFrameHeight(6, compact = true).value)
    }

    @Test fun outOfRangeSpansClampToTheGrid() {
        assertEquals(144f, widgetFrameHeight(0, compact = false).value)
        assertEquals(144f, widgetFrameHeight(1, compact = false).value)
        assertEquals(452f, widgetFrameHeight(9, compact = false).value)
    }

    @Test fun contentHeightSubtractsFramePadding() {
        assertEquals(120f, widgetContentHeight(2, compact = false).value)
        assertEquals(428f, widgetContentHeight(6, compact = false).value)
    }

    @Test fun providerMinimumChoosesSmallestFittingSpan() {
        assertEquals(2, rowSpanForFrameHeight(0))
        assertEquals(2, rowSpanForFrameHeight(100))
        assertEquals(2, rowSpanForFrameHeight(144))
        assertEquals(3, rowSpanForFrameHeight(145))
        assertEquals(3, rowSpanForFrameHeight(221))
        assertEquals(4, rowSpanForFrameHeight(222))
        assertEquals(4, rowSpanForFrameHeight(298))
        assertEquals(5, rowSpanForFrameHeight(299))
        assertEquals(6, rowSpanForFrameHeight(452))
        assertEquals(6, rowSpanForFrameHeight(999))
    }

    @Test fun cellWidthSplitsTheAvailableWidthMinusGaps() {
        assertEquals(80f, widgetCellWidth(350.dp).value)
        assertEquals(170f, widgetSpanWidth(2, 80.dp).value)
        assertEquals(350f, widgetSpanWidth(4, 80.dp).value)
    }

    @Test fun dragSnapsAtHalfACell() {
        assertEquals(0, snapSpanDelta(30f, 67f))
        assertEquals(1, snapSpanDelta(40f, 67f))
        assertEquals(-1, snapSpanDelta(-40f, 67f))
        assertEquals(0, snapSpanDelta(0f, 0f))
    }

    @Test fun snappedSpanClampsToTheGrid() {
        assertEquals(4, snappedSpan(4, 10f, 67f))
        assertEquals(6, snappedSpan(4, 200f, 67f))
        assertEquals(2, snappedSpan(6, -400f, 67f))
        assertEquals(2, snappedSpan(2, -400f, 67f))
        assertEquals(3, snappedSpan(2, 67f, 67f))
    }

    @Test fun sheetChoicesCoverTheWholeGrid() {
        assertEquals(listOf(2, 3, 4, 5, 6), widgetRowSpanChoices().map { it.first })
        assertEquals(listOf(2, 3, 4), widgetColSpanChoices().map { it.first })
        assertEquals("Half", widgetColSpanChoices().first().second)
        assertEquals("Full", widgetColSpanChoices().last().second)
    }
}
