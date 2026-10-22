package com.studentlauncher.ui

import kotlin.math.abs

/** Fraction of the drag distance that counts as "open" when the finger is released. */
internal const val DRAWER_OPEN_FRACTION = 0.35f

/** Upward fling speed (px/s) that opens the drawer even below the distance threshold. */
internal const val DRAWER_FLING_VELOCITY = 900f

/** Maps an upward drag (negative dy) to a 0..1 panel progress. */
internal fun drawerProgress(dragPx: Float, distancePx: Float): Float =
    (-dragPx / distancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)

/** Release decision: distance threshold, or a decisive upward fling after a small drag. */
internal fun drawerShouldOpen(fraction: Float, velocityY: Float): Boolean =
    fraction >= DRAWER_OPEN_FRACTION ||
        (fraction > 0.05f && velocityY < -DRAWER_FLING_VELOCITY)

/**
 * True once a gesture is clearly a vertical upward drag. Horizontal dominance is left to the
 * Home pager; a downward drag is left to whatever is underneath.
 */
internal fun drawerGestureOwns(dx: Float, dy: Float, slop: Float): Boolean =
    dy < 0f && abs(dy) > slop && abs(dy) > abs(dx)

/** The bottom band above the system gesture inset is the only full-width drawer start zone. */
internal fun drawerStartAllowed(y: Float, height: Float, insetBottom: Float, band: Float): Boolean =
    y >= height - insetBottom - band
