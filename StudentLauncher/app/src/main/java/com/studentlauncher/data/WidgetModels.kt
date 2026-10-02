package com.studentlauncher.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class WidgetBg { Glass, Solid, None }

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

data class WidgetItem(
    val id: String,
    val type: String,
    val bg: WidgetBg = WidgetBg.Glass,
    val half: Boolean = false,
    val size: Int = 1,
    val height: Int = 2,
    val tone: String = "auto",
    val cfg: Map<String, String> = emptyMap()
) {
    /** Explicit width flag — independent of height (small+full and tall+half both valid). */
    val effectiveHalf: Boolean get() = half
    fun get(key: String, def: String = ""): String = cfg[key] ?: def
    fun put(key: String, value: String): WidgetItem = copy(cfg = cfg + (key to value))
}

fun newWidget(type: String): WidgetItem {
    val h = when (type) {
        WT.DATE, WT.BATTERY, WT.NEXT, WT.COUNTDOWN, WT.TIMER, WT.WATER, WT.POMODORO, WT.EVENTS -> 1
        WT.TASKS, WT.QUICK, WT.SPHERE, WT.GLYPH, WT.TODAY, WT.PLANNER, WT.FLASHCARDS, WT.FORMULAS, WT.ASSIGNMENTS, WT.REMINDERS -> 2
        else -> 2
    }
    return WidgetItem(
        id = UUID.randomUUID().toString(),
        type = type,
        height = h,
        half = h <= 1 || type in setOf(WT.WATER, WT.POMODORO),
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
                .put("half", w.half).put("size", w.size).put("height", w.height)
                .put("tone", w.tone).put("cfg", cfg)
        )
    }
    return arr.toString()
}

fun parseWidgets(s: String?): List<WidgetItem>? {
    if (s == null) return null
    return runCatching {
        val arr = JSONArray(s)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val c = o.optJSONObject("cfg") ?: JSONObject()
            val cfg = mutableMapOf<String, String>()
            c.keys().forEach { k -> cfg[k] = c.optString(k) }
            WidgetItem(
                id = o.getString("id"),
                type = o.getString("type"),
                bg = runCatching { WidgetBg.valueOf(o.optString("bg", "Glass")) }.getOrDefault(WidgetBg.Glass),
                half = o.optBoolean("half", false),
                size = o.optInt("size", 1),
                height = o.optInt("height", if (o.optBoolean("half", false)) 1 else 2),
                tone = o.optString("tone", "auto"),
                cfg = cfg
            )
        }
    }.getOrNull()
}
