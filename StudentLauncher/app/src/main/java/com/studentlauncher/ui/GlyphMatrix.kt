package com.studentlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import android.text.format.DateFormat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Circular 25x25 glyph matrix mask. A cell is inside the display when
 * `(x - 12)^2 + (y - 12)^2 <= 154`, which yields exactly 489 cells.
 *
 * Pure arithmetic so it can be unit tested without an Android runtime.
 */
object GlyphMatrixSpec {
    const val GRID = 25
    const val CENTER = 12

    /** Squared mask radius from the design sheet ("(x-12)^2 + (y-12)^2 <= 154"). */
    const val RADIUS_SQ = 154

    fun contains(x: Int, y: Int): Boolean {
        val dx = x - CENTER
        val dy = y - CENTER
        return dx * dx + dy * dy <= RADIUS_SQ
    }

    fun radius(x: Int, y: Int): Float {
        val dx = x - CENTER
        val dy = y - CENTER
        return sqrt((dx * dx + dy * dy).toFloat())
    }

    /** Packed (x, y) pairs for every in-mask cell, row-major. */
    val cells: IntArray = run {
        val out = ArrayList<Int>(512)
        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                if (contains(x, y)) {
                    out.add(x)
                    out.add(y)
                }
            }
        }
        out.toIntArray()
    }

    val count: Int get() = cells.size / 2
}

/** Reaction modes for the matrix. Durations of 0 are static (no frame loop, idle cost zero). */
object GlyphModes {
    const val CLOCK = "Clock"
    const val PULSE = "Pulse"
    const val SPIRAL = "Spiral"
    const val WAVE = "Wave"
    const val CHARGING = "Charging"
    const val BREATHE = "Breathe"
    const val DONE = "Done"
    const val TAP = "Tap"
    const val LOW = "Low"

    /** Cycle order used when tapping the glyph widget. */
    val ALL = listOf(CLOCK, PULSE, SPIRAL, WAVE, CHARGING, BREATHE, DONE, TAP, LOW)

    fun next(mode: String): String =
        ALL[(ALL.indexOf(mode).let { if (it < 0) 0 else it } + 1).mod(ALL.size)]

    /** Burst length in milliseconds; 0 means the frame loop never starts. */
    fun durationMs(mode: String): Int = when (mode) {
        PULSE -> 1400
        SPIRAL -> 1800
        WAVE -> 1600
        BREATHE -> 3600
        DONE -> 900
        CHARGING -> 1600
        TAP -> 700
        else -> 0
    }
}

/** Static clock/figure geometry drawn inside the matrix, computed once. */
private val CHECK_PARAM: FloatArray by lazy {
    figureParam(listOf(7.5f to 13.5f, 11f to 17f, 18f to 7.5f), 1.15f)
}
private val BOLT_PARAM: FloatArray by lazy {
    figureParam(listOf(14f to 4.5f, 9.5f to 13.5f, 12.5f to 13.5f, 10.5f to 20.5f), 1.05f)
}

/** For every cell, the normalized position along [pts] when within [width], else -1. */
private fun figureParam(pts: List<Pair<Float, Float>>, width: Float): FloatArray {
    val segs = ArrayList<FloatArray>(pts.size - 1)
    var acc = 0f
    for (i in 0 until pts.size - 1) {
        val (ax, ay) = pts[i]
        val (bx, by) = pts[i + 1]
        segs.add(floatArrayOf(ax, ay, bx, by, acc, acc + hypot(bx - ax, by - ay)))
        acc += hypot(bx - ax, by - ay)
    }
    val total = if (acc == 0f) 1f else acc
    val out = FloatArray(GlyphMatrixSpec.GRID * GlyphMatrixSpec.GRID) { -1f }
    for (y in 0 until GlyphMatrixSpec.GRID) {
        for (x in 0 until GlyphMatrixSpec.GRID) {
            var best = Float.MAX_VALUE
            var bestT = -1f
            for (s in segs) {
                val dx = s[2] - s[0]
                val dy = s[3] - s[1]
                val len2 = dx * dx + dy * dy
                val u = if (len2 == 0f) 0f else (((x + 0.5f - s[0]) * dx + (y + 0.5f - s[1]) * dy) / len2).coerceIn(0f, 1f)
                val d = hypot(x + 0.5f - (s[0] + u * dx), y + 0.5f - (s[1] + u * dy))
                if (d < best) {
                    best = d
                    bestT = (s[4] + u * (s[5] - s[4])) / total
                }
            }
            if (best <= width) out[y * GlyphMatrixSpec.GRID + x] = bestT
        }
    }
    return out
}

/** Context needed to evaluate a reaction at a point. */
class GlyphReaction(val clockLit: Set<Int>, val batteryPct: Int, val charging: Boolean)

/**
 * Brightness of a cell for a reaction. Pure, so the drawing code stays a thin loop and the
 * behaviour can be reasoned about (and tested) without a frame clock.
 */
fun reactionLevel(mode: String, x: Int, y: Int, t: Float, ctx: GlyphReaction): Float {
    val idx = y * GlyphMatrixSpec.GRID + x
    val r = GlyphMatrixSpec.radius(x, y)
    return when (mode) {
        GlyphModes.CLOCK -> if (idx in ctx.clockLit) 1f else 0.06f
        GlyphModes.PULSE -> {
            val front = t * 15.5f
            (1f - abs(r - front) / 2.4f).coerceIn(0f, 1f)
        }
        GlyphModes.TAP -> {
            val front = t * 14.5f
            (1f - abs(r - front) / 1.7f).coerceIn(0f, 1f)
        }
        GlyphModes.SPIRAL -> {
            val a = atan2((y - 12f), (x - 12f))
            (((sin(a * 2f + r * 0.7f - t * 12f) + 1f) / 2f) * (1f - 0.25f * t)).coerceIn(0f, 1f)
        }
        GlyphModes.WAVE -> {
            ((sin(x * 0.55f + t * 9f) + cos(y * 0.45f - t * 7f) + 2f) / 4f).coerceIn(0f, 1f)
        }
        GlyphModes.BREATHE -> {
            val rr = 7f + 3f * sin(t * Math.PI.toFloat() * 6f)
            (1f - abs(r - rr) / 1.4f).coerceIn(0f, 1f)
        }
        GlyphModes.DONE -> {
            val p = CHECK_PARAM[idx]
            when {
                p < 0f -> 0.05f
                p <= t -> 1f
                else -> 0.12f
            }
        }
        GlyphModes.CHARGING -> {
            val bolt = BOLT_PARAM[idx]
            val ringR = (ctx.batteryPct / 100f) * 12.2f
            // While plugged in this is a sustained state, so once the intro burst has played
            // (t >= 1) the ring and bolt render steady and cost no frames.
            val ring = if (t >= 1f) 0.85f else 0.55f + 0.45f * sin(t * Math.PI.toFloat() * 4f)
            when {
                bolt >= 0f -> 1f
                abs(r - ringR) <= 0.75f -> ring
                else -> 0.05f
            }
        }
        GlyphModes.LOW -> if (r <= 2.6f) 0.55f else 0.05f
        else -> 0.06f
    }.coerceIn(0f, 1f)
}

/** Cells that render in the accent colour for a reaction (bright edges and figures). */
fun reactionAccent(mode: String, x: Int, y: Int, t: Float, ctx: GlyphReaction): Boolean {
    val idx = y * GlyphMatrixSpec.GRID + x
    val r = GlyphMatrixSpec.radius(x, y)
    return when (mode) {
        GlyphModes.CLOCK -> idx == (12 * 25 + 12)
        GlyphModes.PULSE -> abs(r - t * 15.5f) < 0.9f
        GlyphModes.TAP -> abs(r - t * 14.5f) < 0.6f
        GlyphModes.DONE -> CHECK_PARAM[idx] >= 0f
        GlyphModes.CHARGING -> ctx.charging && BOLT_PARAM[idx] >= 0f
        else -> false
    }
}

/** Dot-glyph cells for a clock time; four 5x7 glyphs laid out as two stacked rows. */
internal fun timeCells(h: Int, m: Int): Set<Int> {
    val out = mutableSetOf<Int>()
    val digits = listOf(h / 10, h % 10, m / 10, m % 10)
    digits.forEachIndexed { i, dgt ->
        val g = GLYPHS[('0' + dgt)] ?: return@forEachIndexed
        val row0 = if (i < 2) 4 else 14
        val col0 = 7 + (i % 2) * 6
        for (r in g.indices) {
            for (c in g[r].indices) {
                if (g[r][c] == '1') out.add((row0 + r) * GlyphMatrixSpec.GRID + col0 + c)
            }
        }
    }
    return out
}

/**
 * Reactive 25x25 dot display. Static by default; each [mode] plays one finite burst
 * (event driven, at most 30 fps, zero work while idle) and then stops.
 */
@Composable
fun GlyphMatrix(
    mode: String,
    color: Color,
    accent: Color,
    modifier: Modifier,
    burstKey: Int = 0,
    batteryPct: Int = 100,
    charging: Boolean = false
) {
    val now = rememberNow()
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val motion = LocalMotionScale.current
    var t by remember { mutableFloatStateOf(0f) }
    val duration = GlyphModes.durationMs(mode)

    // Finite, event-driven burst. Nothing schedules frames while idle.
    LaunchedEffect(mode, burstKey, motion) {
        if (duration <= 0 || motion <= 0.05f) return@LaunchedEffect
        val start = withFrameNanos { it }
        var lastDraw = start
        t = 0f
        while (true) {
            val frame = withFrameNanos { it }
            val elapsed = (frame - start) / 1e6f
            // Cap the update rate at ~30 fps by only advancing on 33 ms steps.
            if (frame - lastDraw >= 33_000_000L || elapsed >= duration) {
                lastDraw = frame
                t = (elapsed / duration).coerceIn(0f, 1f)
            }
            if (elapsed >= duration) break
        }
    }

    val clockLit = remember(mode, now.hour, now.minute, is24) {
        if (mode == GlyphModes.CLOCK) {
            timeCells(if (is24) now.hour else ((now.hour + 11) % 12) + 1, now.minute)
        } else {
            emptySet()
        }
    }
    val ctx = GlyphReaction(clockLit, batteryPct, charging)

    Canvas(modifier) {
        val grid = GlyphMatrixSpec.GRID
        val cell = size.minDimension / grid
        val dot = cell * 0.74f
        val ox = (size.width - cell * grid) / 2f
        val oy = (size.height - cell * grid) / 2f
        val dim = ArrayList<Offset>(GlyphMatrixSpec.count)
        val lit = ArrayList<Offset>(GlyphMatrixSpec.count)
        val acc = ArrayList<Offset>(GlyphMatrixSpec.count)
        val cells = GlyphMatrixSpec.cells
        var i = 0
        while (i < cells.size) {
            val x = cells[i]
            val y = cells[i + 1]
            i += 2
            val o = Offset(ox + (x + 0.5f) * cell, oy + (y + 0.5f) * cell)
            val level = reactionLevel(mode, x, y, t, ctx)
            dim.add(o)
            if (level >= 0.5f) {
                if (reactionAccent(mode, x, y, t, ctx)) acc.add(o) else lit.add(o)
            }
        }
        // At most four draw calls per frame.
        drawPoints(dim, PointMode.Points, color.copy(alpha = 0.10f), dot, StrokeCap.Round)
        if (lit.isNotEmpty()) drawPoints(lit, PointMode.Points, color, dot, StrokeCap.Round)
        if (acc.isNotEmpty()) drawPoints(acc, PointMode.Points, accent, dot, StrokeCap.Round)
    }
}
