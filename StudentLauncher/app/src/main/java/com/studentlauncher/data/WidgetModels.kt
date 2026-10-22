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
internal const val MAX_WIDGET_ROW_SPAN = 4

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

internal fun clampWidgetColSpan(v: Int) = v.coerceIn(1, WIDGET_COLUMNS)
internal fun clampWidgetRowSpan(v: Int) = v.coerceIn(1, MAX_WIDGET_ROW_SPAN)

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

fun newWidget(type: String): WidgetItem {
    val h = when (type) {
        WT.DATE, WT.BATTERY, WT.NEXT, WT.COUNTDOWN, WT.TIMER, WT.WATER, WT.POMODORO, WT.EVENTS -> 1
        WT.TASKS, WT.QUICK, WT.SPHERE, WT.GLYPH, WT.TODAY, WT.PLANNER, WT.FLASHCARDS, WT.FORMULAS, WT.ASSIGNMENTS, WT.REMINDERS -> 2
        else -> 2
    }
    return WidgetItem(
        id = UUID.randomUUID().toString(),
        type = type,
        rowSpan = h,
        colSpan = if (h <= 1 || type in setOf(WT.WATER, WT.POMODORO)) 2 else WIDGET_COLUMNS,
        size = if (type in setOf(WT.PLANNER, WT.FLASHCARDS, WT.FORMULAS, WT.ASSIGNMENTS, WT.REMINDERS)) 1 else 0,
        cfg = when (type) {
            WT.CLOCK -> mapOf("style" to "Dot", "fmt" to "sys", "date" to "1", "quote" to "1")
            WT.NEXT -> mapOf("title" to "Physics", "time" to "10:30")
            WT.COUNTDOWN -> mapOf("title" to "Exam", "date" to "")
            WT.TIMER -> mapOf("min" to "25", "start" to "0")
            WT.GLYPH -> mapOf("mode" to "Pulse")
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
                .put("height", w.rowSpan.coerceIn(1, 3))
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
                when (o.optInt("size", 1)) { 0 -> 1; 2 -> MAX_WIDGET_ROW_SPAN; else -> 2 }
            } else {
                o.optInt("height", if (half) 1 else 2)
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
