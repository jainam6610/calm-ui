package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSpansTest {

    @Test fun fullFrameHeightsMatchDecisionTable() {
        assertEquals(104f, widgetFrameHeight(1, compact = false).value)
        assertEquals(144f, widgetFrameHeight(2, compact = false).value)
        assertEquals(204f, widgetFrameHeight(3, compact = false).value)
        assertEquals(264f, widgetFrameHeight(4, compact = false).value)
    }

    @Test fun compactFrameHeightsKeepOldCaps() {
        assertEquals(84f, widgetFrameHeight(1, compact = true).value)
        assertEquals(114f, widgetFrameHeight(2, compact = true).value)
        assertEquals(164f, widgetFrameHeight(3, compact = true).value)
        assertEquals(224f, widgetFrameHeight(4, compact = true).value)
    }

    @Test fun outOfRangeSpansClamp() {
        assertEquals(104f, widgetFrameHeight(0, compact = false).value)
        assertEquals(264f, widgetFrameHeight(9, compact = false).value)
    }

    @Test fun contentHeightSubtractsFramePadding() {
        assertEquals(80f, widgetContentHeight(1, compact = false).value)
        assertEquals(240f, widgetContentHeight(4, compact = false).value)
    }

    @Test fun providerMinimumChoosesSmallestFittingSpan() {
        assertEquals(1, rowSpanForFrameHeight(0))
        assertEquals(1, rowSpanForFrameHeight(100))
        assertEquals(2, rowSpanForFrameHeight(105))
        assertEquals(2, rowSpanForFrameHeight(144))
        assertEquals(3, rowSpanForFrameHeight(145))
        assertEquals(4, rowSpanForFrameHeight(205))
        assertEquals(4, rowSpanForFrameHeight(999))
    }
}
