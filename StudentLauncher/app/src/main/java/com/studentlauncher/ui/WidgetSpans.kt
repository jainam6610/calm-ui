package com.studentlauncher.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentlauncher.data.MAX_WIDGET_ROW_SPAN
import com.studentlauncher.data.clampWidgetRowSpan

/**
 * Span 1..4 maps to a fixed frame height. These are frame heights, not content caps:
 * the 12 dp frame padding is already included. Compact uses the same table minus 20 dp.
 */
internal val WIDGET_FRAME_HEIGHTS = listOf(104, 144, 204, 264)
internal val WIDGET_FRAME_HEIGHTS_COMPACT = listOf(84, 114, 164, 224)

internal fun widgetFrameHeight(rowSpan: Int, compact: Boolean): Dp {
    val i = clampWidgetRowSpan(rowSpan) - 1
    return (if (compact) WIDGET_FRAME_HEIGHTS_COMPACT else WIDGET_FRAME_HEIGHTS)[i].dp
}

/** Content cap inside [widgetFrameHeight]: frame height minus 2 × 12 dp padding. */
internal fun widgetContentHeight(rowSpan: Int, compact: Boolean): Dp {
    val frame = (if (compact) WIDGET_FRAME_HEIGHTS_COMPACT else WIDGET_FRAME_HEIGHTS)[
        clampWidgetRowSpan(rowSpan) - 1
    ]
    return (frame - 24).dp
}

/** Smallest span that can host a provider's minimum height, capped at the table end. */
internal fun rowSpanForFrameHeight(minHeightDp: Int): Int {
    val i = WIDGET_FRAME_HEIGHTS.indexOfFirst { it >= minHeightDp }
    return if (i < 0) MAX_WIDGET_ROW_SPAN else i + 1
}
