package com.studentlauncher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class WidgetBg { Glass, Solid, None, Frosted }

/** Widget type ids and the catalog shown in "Add a widget". */
object WT {
    const val CLOCK = "clock"
    const val DATE = "date"
    const val WEEK = "week"
    const val BATTERY = "battery"
    const val NEXT = "next"
    const val COUNTDOWN = "countdown"
    const val TIMER = "timer"
    const val TASKS = "tasks"
    const val QUICK = "quick"
    const val SPHERE = "sphere"
    const val GLYPH = "glyph"
    const val ANDROID = "android"
    const val TODAY = "today"
    const val MINDFUL_TODAY = "mindful_today"
    const val MINDFUL_WEEK = "mindful_week"
    const val EXPENSES = "expenses"
    const val PLANNER = "planner"
    const val REMINDERS = "reminders"
    const val POMODORO = "pomodoro"
    const val FLASHCARDS = "flashcards"
    const val HABITS = "habits"
    const val GPA = "gpa"
    const val WATER = "water"
    const val FORMULAS = "formulas"
    const val ASSIGNMENTS = "assignments"
    const val MONITOR = "monitor"
    const val QUOTES = "quotes"
    const val EVENTS = "events"
    const val PHOTOS = "photos"

    val catalog: List<Triple<String, String, String>> = listOf(
        Triple(CLOCK, "Clock", "Dot, digital, words or analog"),
        Triple(DATE, "Date", "Big day number"),
        Triple(WEEK, "Week", "This week at a glance"),
        Triple(BATTERY, "Battery", "Ring with the charge level"),
        Triple(NEXT, "Next up", "Your next class or task"),
        Triple(COUNTDOWN, "Exam countdown", "Days left, in dots"),
        Triple(TIMER, "Study timer", "15 to 60 minute focus timer"),
        Triple(TASKS, "Tasks", "A tiny checklist"),
        Triple(QUICK, "Quick settings", "Shortcuts to Wi-Fi, Bluetooth and more"),
        Triple(SPHERE, "3D sphere", "Drag to spin it"),
        Triple(GLYPH, "Glyph matrix", "25 by 25 dot display, tap to change"),
        Triple(TODAY, "Today", "Screen time vs daily budget"),
        Triple(MINDFUL_TODAY, "Mindful today", "Minutes on distracting apps against your budget"),
        Triple(MINDFUL_WEEK, "Mindful week", "Seven days and what saying no saved"),
        Triple(EXPENSES, "Expenses", "Track spending against a monthly budget"),
        Triple(PLANNER, "Planner", "Your class schedule at a glance"),
        Triple(REMINDERS, "Reminders", "Quick timed reminders"),
        Triple(POMODORO, "Pomodoro", "Focus timer with short breaks"),
        Triple(FLASHCARDS, "Flashcards", "Study with quick flip cards"),
        Triple(HABITS, "Habits", "Tick off daily habits"),
        Triple(GPA, "GPA", "Track your GPA by course"),
        Triple(WATER, "Water", "Log glasses of water"),
        Triple(FORMULAS, "Formulas", "Pin a formula for quick reference"),
        Triple(ASSIGNMENTS, "Assignments", "Track assignment due dates"),
        Triple(MONITOR, "Monitor", "RAM, storage, swap and CPU"),
        Triple(QUOTES, "Quotes & facts", "A random line, tap for another"),
        Triple(EVENTS, "Events", "Upcoming holidays with days left"),
        Triple(PHOTOS, "Photos", "Rotates through your wallpaper photos")
    )

    fun title(type: String): String =
        catalog.firstOrNull { it.first == type }?.second ?: if (type == ANDROID) "Android widget" else type
}

internal const val WIDGET_COLUMNS = 4

/** Grid floor. A two-row tile is the smallest that still has room for a label and a value. */
internal const val MIN_WIDGET_ROW_SPAN = 2
internal const val MAX_WIDGET_ROW_SPAN = 6
internal const val MIN_WIDGET_COL_SPAN = 2

/** Pre-grid builds stored a tall Android widget as `size == 2`, which meant four rows. */
internal const val LEGACY_TALL_ROW_SPAN = 4

/** Width/height in grid spans. Order is position; there is no absolute row/column. */
data class WidgetItem(
    val id: String,
    val type: String,
    val bg: WidgetBg = WidgetBg.Glass,
    val colSpan: Int = WIDGET_COLUMNS,
    val rowSpan: Int = 2,
    /** Legacy hosted-widget size, read only when old JSON has no rowSpan. */
    val size: Int = 1,
    val tone: String = "auto",
    val cfg: Map<String, String> = emptyMap()
) {
    fun get(key: String, def: String = ""): String = cfg[key] ?: def
    fun put(key: String, value: String): WidgetItem = copy(cfg = cfg + (key to value))
}

/** One legacy record before spans are resolved. Explicit spans always win. */
internal data class SpanSeed(
    val explicitColSpan: Int?,
    val explicitRowSpan: Int?,
    val legacyHalf: Boolean,
    val legacyHeight: Int
)

internal data class WidgetSpan(val colSpan: Int, val rowSpan: Int)

internal fun clampWidgetColSpan(v: Int) = v.coerceIn(MIN_WIDGET_COL_SPAN, WIDGET_COLUMNS)
internal fun clampWidgetRowSpan(v: Int) = v.coerceIn(MIN_WIDGET_ROW_SPAN, MAX_WIDGET_ROW_SPAN)

/**
 * Legacy `half` was a flag, not a width: a lone half rendered full width because its
 * row child used weight(1f). Preserve what users saw:
 * - two consecutive legacy halves become 2 + 2 columns;
 * - an unpaired legacy half becomes 4 columns;
 * - a full widget stays 4 columns.
 */
internal fun migrateWidgetSpans(seeds: List<SpanSeed>): List<WidgetSpan> {
    val out = ArrayList<WidgetSpan>(seeds.size)
    var i = 0
    while (i < seeds.size) {
        val s = seeds[i]
        val row = clampWidgetRowSpan(s.explicitRowSpan ?: s.legacyHeight)
        if (s.explicitColSpan != null) {
            out += WidgetSpan(clampWidgetColSpan(s.explicitColSpan), row)
            i++
            continue
        }
        val next = seeds.getOrNull(i + 1)
        if (s.legacyHalf && next != null && next.explicitColSpan == null && next.legacyHalf) {
            out += WidgetSpan(2, row)
            out += WidgetSpan(2, clampWidgetRowSpan(next.explicitRowSpan ?: next.legacyHeight))
            i += 2
            continue
        }
        out += WidgetSpan(WIDGET_COLUMNS, row)
        i++
    }
    return out
}

/** Row packing: consume spans left-to-right; a span that does not fit starts the next row. */
internal fun flowRowsOfSpans(spans: List<Int>, columns: Int = WIDGET_COLUMNS): List<List<Int>> {
    if (columns <= 0) return spans.indices.map { listOf(it) }
    val rows = mutableListOf<List<Int>>()
    var row = mutableListOf<Int>()
    var used = 0
    spans.forEachIndexed { index, raw ->
        val span = raw.coerceIn(1, columns)
        if (row.isNotEmpty() && used + span > columns) {
            rows += row
            row = mutableListOf()
            used = 0
        }
        row += index
        used += span
    }
    if (row.isNotEmpty()) rows += row
    return rows
}

internal fun flowRows(items: List<WidgetItem>, columns: Int = WIDGET_COLUMNS): List<List<WidgetItem>> =
    flowRowsOfSpans(items.map { it.colSpan }, columns).map { indices -> indices.map(items::get) }

internal fun rowUsedColumns(row: List<WidgetItem>, columns: Int = WIDGET_COLUMNS): Int =
    row.sumOf { it.colSpan.coerceIn(1, columns) }.coerceAtMost(columns)

/** One widget's resolved top-left cell on the Home grid. */
internal data class WidgetGridSlot(
    val index: Int,
    val row: Int,
    val col: Int,
    val colSpan: Int,
    val rowSpan: Int
)

/**
 * Control-Center-style packing: each widget takes the first cell, scanning rows top-down
 * and columns left-to-right, where its rectangle fits. This keeps tiles flush instead of
 * leaving a hole whenever the previous row happens to be wider.
 *
 * When [pinnedIndex] is set (a tile being resized), it is placed first at
 * [pinnedRow]/[pinnedCol] and everything else flows around it. Without that, growing a
 * tile could make it leap to a later row mid-drag.
 */
internal fun packWidgetGrid(
    spans: List<WidgetSpan>,
    columns: Int = WIDGET_COLUMNS,
    pinnedIndex: Int? = null,
    pinnedRow: Int = 0,
    pinnedCol: Int = 0
): List<WidgetGridSlot> {
    if (columns <= 0) return spans.mapIndexed { i, s -> WidgetGridSlot(i, i, 0, 1, 1) }
    val occupied = HashSet<Int>()
    val placed = arrayOfNulls<WidgetGridSlot>(spans.size)
    val limit = spans.size * (MAX_WIDGET_ROW_SPAN + 1) + 1

    fun put(index: Int, row: Int, col: Int, cs: Int, rs: Int) {
        for (r in row until row + rs) {
            for (c in col until col + cs) occupied += r * columns + c
        }
        placed[index] = WidgetGridSlot(index, row, col, cs, rs)
    }

    val pin = pinnedIndex?.takeIf { it in spans.indices }
    if (pin != null) {
        val span = spans[pin]
        val cs = span.colSpan.coerceIn(1, columns)
        val rs = span.rowSpan.coerceAtLeast(1)
        val row = pinnedRow.coerceAtLeast(0)
        // Keep the old column when the new width still fits, otherwise slide left just
        // enough. A height-only drag therefore never moves the tile sideways.
        val col = pinnedCol.coerceIn(0, (columns - cs).coerceAtLeast(0))
        put(pin, row, col, cs, rs)
    }

    spans.forEachIndexed { index, span ->
        if (placed[index] != null) return@forEachIndexed
        val cs = span.colSpan.coerceIn(1, columns)
        val rs = span.rowSpan.coerceAtLeast(1)
        var row = 0
        while (row <= limit) {
            val col = (0..columns - cs).firstOrNull { c -> gridFits(occupied, row, c, cs, rs, columns) }
            if (col != null) {
                put(index, row, col, cs, rs)
                return@forEachIndexed
            }
            row++
        }
        // Unreachable in practice (the row limit grows faster than any legal tile).
        put(index, row, 0, cs, rs)
    }
    return placed.mapIndexed { i, s -> s ?: WidgetGridSlot(i, i, 0, 1, 1) }
}

private fun gridFits(
    occupied: Set<Int>,
    row: Int,
    col: Int,
    colSpan: Int,
    rowSpan: Int,
    columns: Int
): Boolean {
    for (r in row until row + rowSpan) {
        for (c in col until col + colSpan) {
            if ((r * columns + c) in occupied) return false
        }
    }
    return true
}

fun newWidget(type: String): WidgetItem {
    // Every tile starts on the same uniform two-row unit; users grow it from the corner.
    val h = MIN_WIDGET_ROW_SPAN
    return WidgetItem(
        id = UUID.randomUUID().toString(),
        type = type,
        rowSpan = h,
        colSpan = if (type in setOf(WT.WATER, WT.POMODORO)) 2 else WIDGET_COLUMNS,
        size = if (type in setOf(WT.PLANNER, WT.FLASHCARDS, WT.FORMULAS, WT.ASSIGNMENTS, WT.REMINDERS)) 1 else 0,
        cfg = when (type) {
            WT.CLOCK -> mapOf("style" to "Dot", "fmt" to "sys", "date" to "1", "quote" to "1")
            WT.NEXT -> mapOf("title" to "Physics", "time" to "10:30")
            WT.COUNTDOWN -> mapOf("title" to "Exam", "date" to "")
            WT.TIMER -> mapOf("min" to "25", "start" to "0")
            WT.GLYPH -> emptyMap()
            WT.WEEK -> mapOf("mon" to "1")
            WT.TODAY -> emptyMap()
            WT.EXPENSES -> mapOf("budget" to "2000", "month" to java.time.YearMonth.now().toString())
            WT.POMODORO -> mapOf("focus" to "25", "brk" to "5", "phase" to "focus", "start" to "0", "rounds" to "0")
            WT.HABITS -> mapOf("names" to "Read|Exercise|Sleep by 11")
            WT.GPA -> mapOf("rows" to "")
            WT.WATER -> mapOf("goal" to "8", "date" to "", "count" to "0")
            WT.FORMULAS -> mapOf("title" to "Formula", "body" to "")
            WT.QUOTES -> mapOf("mode" to "both")
            else -> emptyMap()
        }
    )
}

fun defaultWidgets(): List<WidgetItem> = listOf(newWidget(WT.CLOCK), newWidget(WT.NEXT), newWidget(WT.TIMER))

fun List<WidgetItem>.toJson(): String {
    val arr = JSONArray()
    forEach { w ->
        val cfg = JSONObject()
        w.cfg.forEach { (k, v) -> cfg.put(k, v) }
        arr.put(
            JSONObject()
                .put("id", w.id).put("type", w.type).put("bg", w.bg.name)
                .put("colSpan", w.colSpan).put("rowSpan", w.rowSpan)
                // One-release compatibility for older builds reading this JSON.
                .put("half", w.colSpan <= 2).put("size", w.size)
                .put("height", w.rowSpan.coerceIn(1, LEGACY_TALL_ROW_SPAN))
                .put("tone", w.tone).put("cfg", cfg)
        )
    }
    return arr.toString()
}

fun parseWidgets(s: String?): List<WidgetItem>? {
    if (s == null) return null
    return runCatching {
        val arr = JSONArray(s)
        val objects = (0 until arr.length()).map { arr.getJSONObject(it) }
        val seeds = objects.map { o ->
            val half = o.optBoolean("half", false)
            val type = o.optString("type")
            // Old hosted widgets stored height in `size`, not in `height`.
            val legacyHeight = if (type == WT.ANDROID) {
                when (o.optInt("size", 1)) {
                    0 -> MIN_WIDGET_ROW_SPAN
                    2 -> LEGACY_TALL_ROW_SPAN
                    else -> MIN_WIDGET_ROW_SPAN
                }
            } else {
                o.optInt("height", MIN_WIDGET_ROW_SPAN)
            }
            SpanSeed(
                explicitColSpan = if (o.has("colSpan")) o.optInt("colSpan") else null,
                explicitRowSpan = if (o.has("rowSpan")) o.optInt("rowSpan") else null,
                legacyHalf = half,
                legacyHeight = legacyHeight
            )
        }
        val spans = migrateWidgetSpans(seeds)
        objects.mapIndexed { i, o ->
            val c = o.optJSONObject("cfg") ?: JSONObject()
            val cfg = mutableMapOf<String, String>()
            c.keys().forEach { k -> cfg[k] = c.optString(k) }
            WidgetItem(
                id = o.getString("id"),
                type = o.getString("type"),
                bg = runCatching { WidgetBg.valueOf(o.optString("bg", "Glass")) }.getOrDefault(WidgetBg.Glass),
                colSpan = spans[i].colSpan,
                rowSpan = spans[i].rowSpan,
                size = o.optInt("size", 1),
                tone = o.optString("tone", "auto"),
                cfg = cfg
            )
        }
    }.getOrNull()
}
