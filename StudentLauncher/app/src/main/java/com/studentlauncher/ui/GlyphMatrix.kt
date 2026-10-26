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
import com.studentlauncher.reactions.Flash
import com.studentlauncher.reactions.TorchPatterns
import kotlin.math.PI
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

/**
 * Every behaviour the matrix can show, with the personality it belongs to.
 * Durations of 0 are static: they never schedule a frame.
 */
object GlyphModes {
    const val CLOCK = "Clock"
    const val QUIET = "Quiet"
    const val FOCUS = "Focus"
    const val TASKS = "Tasks"
    const val PULSE = "Pulse"
    const val ORBIT = "Orbit"
    const val SPIRAL = "Spiral"
    const val WAVE = "Wave"
    const val DRIFT = "Drift"
    const val CHARGING = "Charging"
    const val BREATHE = "Breathe"
    const val DONE = "Done"
    const val TAP = "Tap"
    const val SPARK = "Spark"
    const val CUSTOM = "Custom"
    const val LOW = "Low"
    const val OFF = "Off"

    const val CAT_QUIET = "Quiet"
    const val CAT_PRODUCTIVE = "Productive"
    const val CAT_CREATIVE = "Creative"
    const val CAT_COOL = "Cool"

    val categories = listOf(CAT_QUIET, CAT_PRODUCTIVE, CAT_CREATIVE, CAT_COOL)

    data class Meta(
        val id: String,
        val label: String,
        val category: String,
        val summary: String,
        val durationMs: Int
    )

    /** Stable order: the tap-cycle, the studio list and the tests all read this. */
    val catalog: List<Meta> = listOf(
        Meta(CLOCK, "Clock", CAT_QUIET, "The time as four dot digits", 0),
        Meta(QUIET, "Quiet", CAT_QUIET, "One dim centre dot", 0),
        Meta(OFF, "Off", CAT_QUIET, "No light at rest", 0),
        Meta(BREATHE, "Breathe", CAT_QUIET, "A slow ring, once", 3600),
        Meta(FOCUS, "Focus ring", CAT_PRODUCTIVE, "Study timer as a ring", 0),
        Meta(TASKS, "Task arc", CAT_PRODUCTIVE, "Completed tasks as an arc", 0),
        Meta(DONE, "Done", CAT_PRODUCTIVE, "Draw a check mark", 900),
        Meta(LOW, "Low battery", CAT_PRODUCTIVE, "A small centre dot", 0),
        Meta(SPIRAL, "Spiral", CAT_CREATIVE, "A slow two-arm spiral", 1800),
        Meta(WAVE, "Wave", CAT_CREATIVE, "Ripples across the grid", 1600),
        Meta(DRIFT, "Drift", CAT_CREATIVE, "Three wandering points", 3600),
        Meta(SPARK, "Spark", CAT_CREATIVE, "A bright scatter", 1200),
        Meta(CUSTOM, "Custom art", CAT_CREATIVE, "Your own dots, drawn here", 0),
        Meta(PULSE, "Pulse", CAT_COOL, "A ring sweeping outward", 1400),
        Meta(ORBIT, "Orbit", CAT_COOL, "Two dots circling", 4200),
        Meta(TAP, "Tap", CAT_COOL, "A quick touch ripple", 700),
        Meta(CHARGING, "Charging", CAT_COOL, "Battery ring and bolt", 1600)
    )

    /** Every mode, in catalog order. */
    val ALL: List<String> = catalog.map { it.id }

    /** Modes that can be left on screen; all render without a frame loop. */
    val ambient = listOf(CLOCK, QUIET, FOCUS, TASKS, CUSTOM, OFF)

    fun meta(mode: String): Meta? = catalog.firstOrNull { it.id == mode }

    fun byCategory(category: String): List<Meta> = catalog.filter { it.category == category }

    fun next(mode: String): String =
        ALL[(ALL.indexOf(mode).let { if (it < 0) 0 else it } + 1).mod(ALL.size)]

    /** Burst length in milliseconds; 0 means the frame loop never starts. */
    fun durationMs(mode: String): Int = meta(mode)?.durationMs ?: 0
}

/**
 * Custom art is a 625-character row-major bitmap (25 x 25). Only cells inside the circular
 * mask are kept, so a saved design always fits the physical layout.
 */
fun encodeGlyphArt(cells: Set<Int>): String = buildString(GlyphMatrixSpec.GRID * GlyphMatrixSpec.GRID) {
    for (i in 0 until GlyphMatrixSpec.GRID * GlyphMatrixSpec.GRID) {
        val x = i % GlyphMatrixSpec.GRID
        val y = i / GlyphMatrixSpec.GRID
        append(if (i in cells && GlyphMatrixSpec.contains(x, y)) '1' else '0')
    }
}

fun decodeGlyphArt(raw: String?): Set<Int> {
    val size = GlyphMatrixSpec.GRID * GlyphMatrixSpec.GRID
    if (raw == null || raw.length != size) return emptySet()
    val out = HashSet<Int>(256)
    for (i in 0 until size) {
        if (raw[i] == '1') out.add(i)
    }
    return out
}

/**
 * Which torch pattern, if any, belongs to a matrix reaction. Kept pure so the mapping can
 * be tested without the camera light. Touch, breathing, charging and low battery never map.
 */
object GlyphTorch {
    fun pattern(mode: String): List<Flash>? = when (mode) {
        GlyphModes.DONE -> TorchPatterns.DONE
        GlyphModes.PULSE -> TorchPatterns.ALERT
        else -> null
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

/** 0 at the top, increasing clockwise to 1. */
private fun angle01(x: Int, y: Int): Float {
    val a = atan2((y - GlyphMatrixSpec.CENTER).toFloat(), (x - GlyphMatrixSpec.CENTER).toFloat())
    var f = (a + PI.toFloat() / 2f) / (2f * PI.toFloat())
    if (f < 0f) f += 1f
    if (f >= 1f) f -= 1f
    return f
}

/** Context needed to evaluate a reaction at a point. */
class GlyphReaction(
    val clockLit: Set<Int>,
    val batteryPct: Int,
    val charging: Boolean,
    /** 0..1 progress for the focus timer or completed-task arc. */
    val progress: Float = 0f,
    /** Saved dot-art cells for [GlyphModes.CUSTOM]. */
    val custom: Set<Int> = emptySet()
)

/**
 * Brightness of a cell for a reaction. Pure, so the drawing code stays a thin loop and the
 * behaviour can be reasoned about (and tested) without a frame clock.
 */
fun reactionLevel(mode: String, x: Int, y: Int, t: Float, ctx: GlyphReaction): Float {
    val idx = y * GlyphMatrixSpec.GRID + x
    val r = GlyphMatrixSpec.radius(x, y)
    return when (mode) {
        GlyphModes.OFF -> 0f
        GlyphModes.CUSTOM -> if (idx in ctx.custom) 1f else 0.04f
        GlyphModes.QUIET -> if (r <= 1.8f) 0.55f else 0.04f
        GlyphModes.CLOCK -> if (idx in ctx.clockLit) 1f else 0.06f
        GlyphModes.FOCUS -> {
            val p = ctx.progress.coerceIn(0f, 1f)
            val ringR = 3.2f + 8.4f * p
            val ring = (1f - abs(r - ringR) / 1.15f).coerceIn(0f, 1f)
            maxOf(ring, if (r <= 1.3f) 0.6f else 0f)
        }
        GlyphModes.TASKS -> {
            val p = ctx.progress.coerceIn(0f, 1f)
            when {
                abs(r - 10.2f) > 0.95f -> 0.05f
                angle01(x, y) <= p -> 1f
                else -> 0.10f
            }
        }
        GlyphModes.PULSE -> {
            val front = t * 15.5f
            (1f - abs(r - front) / 2.4f).coerceIn(0f, 1f)
        }
        GlyphModes.TAP -> {
            val front = t * 14.5f
            (1f - abs(r - front) / 1.7f).coerceIn(0f, 1f)
        }
        GlyphModes.ORBIT -> {
            val a = t * 2f * PI.toFloat()
            val ax = 12f + cos(a) * 8.2f
            val ay = 12f + sin(a) * 8.2f
            val bx = 12f + cos(a + PI.toFloat()) * 8.2f
            val by = 12f + sin(a + PI.toFloat()) * 8.2f
            val d1 = hypot(x + 0.5f - ax, y + 0.5f - ay)
            val d2 = hypot(x + 0.5f - bx, y + 0.5f - by)
            when {
                d1 <= 1.0f -> 1f
                d2 <= 0.85f -> 0.8f
                d1 <= 2.1f -> 0.28f
                d2 <= 1.9f -> 0.18f
                else -> 0.04f
            }
        }
        GlyphModes.DRIFT -> {
            var best = 0f
            for (k in 0 until 3) {
                val ph = k * 2.094f
                val px = 12f + 7.2f * sin(t * 2f * PI.toFloat() + ph)
                val py = 12f + 7.2f * cos(t * 3f * PI.toFloat() + ph * 1.7f)
                val d = hypot(x + 0.5f - px, y + 0.5f - py)
                best = maxOf(best, (1f - d / 1.6f).coerceIn(0f, 1f))
            }
            best
        }
        GlyphModes.SPARK -> {
            val front = 2.5f + t * 11f
            val ring = (1f - abs(r - front) / 1.3f).coerceIn(0f, 1f)
            val core = if (r <= 2.2f) (1f - t * 1.2f).coerceAtLeast(0.25f) else 0f
            maxOf(ring, core)
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
        GlyphModes.FOCUS -> {
            val ringR = 3.2f + 8.4f * ctx.progress.coerceIn(0f, 1f)
            abs(r - ringR) < 1.0f
        }
        GlyphModes.TASKS -> {
            val p = ctx.progress.coerceIn(0f, 1f)
            abs(r - 10.2f) <= 0.95f && abs(angle01(x, y) - p) < 0.035f
        }
        GlyphModes.ORBIT -> {
            val a = t * 2f * PI.toFloat()
            val ax = 12f + cos(a) * 8.2f
            val ay = 12f + sin(a) * 8.2f
            hypot(x + 0.5f - ax, y + 0.5f - ay) <= 1.0f
        }
        GlyphModes.DRIFT -> {
            val px = 12f + 7.2f * sin(t * 2f * PI.toFloat())
            val py = 12f + 7.2f * cos(t * 3f * PI.toFloat())
            hypot(x + 0.5f - px, y + 0.5f - py) <= 1.3f
        }
        GlyphModes.SPARK -> abs(r - (2.5f + t * 11f)) < 1.1f
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
    charging: Boolean = false,
    progress: Float = 0f,
    dotScale: Float = 1f,
    glow: Boolean = false,
    custom: Set<Int> = emptySet()
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
    val ctx = GlyphReaction(clockLit, batteryPct, charging, progress, custom)

    Canvas(modifier) {
        val grid = GlyphMatrixSpec.GRID
        val cell = size.minDimension / grid
        val dot = cell * 0.74f * dotScale.coerceIn(0.5f, 1.4f)
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
        // At most six draw calls per frame, and glow only when the option is on.
        if (glow && lit.isNotEmpty()) {
            drawPoints(lit, PointMode.Points, color.copy(alpha = 0.12f), dot * 2.2f, StrokeCap.Round)
        }
        if (glow && acc.isNotEmpty()) {
            drawPoints(acc, PointMode.Points, accent.copy(alpha = 0.22f), dot * 2.4f, StrokeCap.Round)
        }
        if (mode != GlyphModes.OFF) {
            drawPoints(dim, PointMode.Points, color.copy(alpha = 0.10f), dot, StrokeCap.Round)
        }
        if (lit.isNotEmpty()) drawPoints(lit, PointMode.Points, color, dot, StrokeCap.Round)
        if (acc.isNotEmpty()) drawPoints(acc, PointMode.Points, accent, dot, StrokeCap.Round)
    }
}
