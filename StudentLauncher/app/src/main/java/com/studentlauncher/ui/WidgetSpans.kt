package com.studentlauncher.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentlauncher.data.MAX_WIDGET_ROW_SPAN
import com.studentlauncher.data.MIN_WIDGET_COL_SPAN
import com.studentlauncher.data.MIN_WIDGET_ROW_SPAN
import com.studentlauncher.data.WIDGET_COLUMNS
import com.studentlauncher.data.clampWidgetRowSpan
import kotlin.math.roundToInt

/**
 * Every tile is built from the same square-ish row unit plus the same gap the columns use.
 * That is what makes a two-row tile next to a three-row tile line up on a shared grid
 * instead of the old 104/144/204/264 step table.
 *
 * Span 2..6 therefore renders at:
 * full    144, 221, 298, 375, 452 dp
 * compact 114, 176, 238, 300, 362 dp
 */
internal const val WIDGET_ROW_UNIT_DP = 67
internal const val WIDGET_ROW_UNIT_COMPACT_DP = 52
internal const val WIDGET_GRID_GAP_DP = 10

internal val WIDGET_FRAME_HEIGHTS = (MIN_WIDGET_ROW_SPAN..MAX_WIDGET_ROW_SPAN).map {
    WIDGET_ROW_UNIT_DP * it + WIDGET_GRID_GAP_DP * (it - 1)
}
internal val WIDGET_FRAME_HEIGHTS_COMPACT = (MIN_WIDGET_ROW_SPAN..MAX_WIDGET_ROW_SPAN).map {
    WIDGET_ROW_UNIT_COMPACT_DP * it + WIDGET_GRID_GAP_DP * (it - 1)
}

internal fun widgetRowUnit(compact: Boolean): Dp =
    (if (compact) WIDGET_ROW_UNIT_COMPACT_DP else WIDGET_ROW_UNIT_DP).dp

internal fun widgetFrameHeight(rowSpan: Int, compact: Boolean): Dp {
    val i = clampWidgetRowSpan(rowSpan) - MIN_WIDGET_ROW_SPAN
    return (if (compact) WIDGET_FRAME_HEIGHTS_COMPACT else WIDGET_FRAME_HEIGHTS)[i].dp
}

/** Content cap inside [widgetFrameHeight]: frame height minus 2 × 12 dp padding. */
internal fun widgetContentHeight(rowSpan: Int, compact: Boolean): Dp {
    val frame = (if (compact) WIDGET_FRAME_HEIGHTS_COMPACT else WIDGET_FRAME_HEIGHTS)[
        clampWidgetRowSpan(rowSpan) - MIN_WIDGET_ROW_SPAN
    ]
    return (frame - 24).dp
}

/** Column width for a four-column grid, after the gaps between the columns. */
internal fun widgetCellWidth(available: Dp, columns: Int = WIDGET_COLUMNS): Dp {
    if (columns <= 0) return available
    val gaps = (WIDGET_GRID_GAP_DP * (columns - 1)).dp
    return ((available - gaps) / columns).coerceAtLeast(1.dp)
}

/** Width of a tile that spans [colSpan] columns. */
internal fun widgetSpanWidth(colSpan: Int, cellWidth: Dp, columns: Int = WIDGET_COLUMNS): Dp {
    val span = colSpan.coerceIn(1, columns)
    return cellWidth * span + (WIDGET_GRID_GAP_DP * (span - 1)).dp
}

/**
 * Rounded, not truncated: a drag of half a cell already snaps to the next size, so the
 * handle feels decisive instead of needing a full cell of travel.
 */
internal fun snapSpanDelta(deltaPx: Float, stepPx: Float): Int =
    if (stepPx <= 0f) 0 else (deltaPx / stepPx).roundToInt()

/** The span a drag lands on, clamped to the grid. */
internal fun snappedSpan(
    startSpan: Int,
    deltaPx: Float,
    stepPx: Float,
    min: Int = MIN_WIDGET_ROW_SPAN,
    max: Int = MAX_WIDGET_ROW_SPAN
): Int = (startSpan + snapSpanDelta(deltaPx, stepPx)).coerceIn(min, max)

/** Smallest span that can host a provider's minimum height, capped at the table end. */
internal fun rowSpanForFrameHeight(minHeightDp: Int): Int {
    val i = WIDGET_FRAME_HEIGHTS.indexOfFirst { it >= minHeightDp }
    return if (i < 0) MAX_WIDGET_ROW_SPAN else i + MIN_WIDGET_ROW_SPAN
}

/** Sheet choices: plain row/column counts, the same numbers the live badge shows. */
internal fun widgetRowSpanChoices(): List<Pair<Int, String>> =
    (MIN_WIDGET_ROW_SPAN..MAX_WIDGET_ROW_SPAN).map { it to it.toString() }

internal fun widgetColSpanChoices(): List<Pair<Int, String>> =
    (MIN_WIDGET_COL_SPAN..WIDGET_COLUMNS).map {
        it to when (it) {
            WIDGET_COLUMNS -> "Full"
            MIN_WIDGET_COL_SPAN -> "Half"
            else -> it.toString()
        }
    }
