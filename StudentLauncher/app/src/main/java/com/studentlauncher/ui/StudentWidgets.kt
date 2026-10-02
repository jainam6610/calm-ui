package com.studentlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WidgetItem
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Ten widgets built for student life. Each is a thin view over strings encoded in [WidgetItem.cfg]
 * (see the `encode`/`decode` helpers below) so the whole system stays inside the existing
 * add / resize / reorder / remove flow with no extra storage plumbing.
 */

// ---------------------------------------------------------------------------------------------
// tiny record codecs - one line per record, fields separated by a separator that cannot appear
// in user text (student input is plain words, so "|" is safe; we still strip it defensively).
// ---------------------------------------------------------------------------------------------

private fun String.esc() = replace("|", "/").replace("\n", " ")

private fun encodeRows(rows: List<List<String>>): String = rows.joinToString("\n") { r -> r.joinToString("|") { it.esc() } }
private fun decodeRows(s: String): List<List<String>> = s.split("\n").filter { it.isNotBlank() }.map { it.split("|") }

@Composable
private fun MiniField(value: String, hint: String, fg: Color, modifier: Modifier = Modifier, numeric: Boolean = false, onGo: () -> Unit = {}, onChange: (String) -> Unit) {
    Box(modifier) {
        if (value.isEmpty()) Txt(hint, 13f, fg.copy(alpha = 0.5f))
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = TextStyle(color = fg, fontSize = 14.sp),
            cursorBrush = SolidColor(fg),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onGo() }),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowActions(onAdd: () -> Unit, addLabel: String = "Add") {
    val c = LocalColors.current
    Txt(addLabel, 13f, c.accent, modifier = Modifier.tap(onAdd).padding(vertical = 4.dp))
}

// ---------------------------------------------------------------------------------------------
// 1. Expense tracker
// ---------------------------------------------------------------------------------------------

@Composable
fun ExpensesContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val month = remember { YearMonth.now().toString() }
    LaunchedEffect(month, w.id) {
        if (w.get("month") != month) {
            vm.setCfg(w.id, "month", month)
            vm.setCfg(w.id, "log", "")
        }
    }
    val budget = w.get("budget", "2000").toIntOrNull() ?: 2000
    val rows = decodeRows(w.get("log")).mapNotNull { r -> r.getOrNull(0)?.toIntOrNull()?.let { it to (r.getOrNull(1) ?: "") } }
    val spent = rows.sumOf { it.first }
    var amt by remember(w.id) { mutableStateOf("") }
    var note by remember(w.id) { mutableStateOf("") }
    val over = spent > budget
    val frac by animateFloatAsState((spent.toFloat() / max(budget, 1)).coerceIn(0f, 1f), spring(dampingRatio = 0.85f, stiffness = 260f), label = "exp")

    Txt("Expenses  -  ${monthLabel()}", 11f, fg2)
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
        Txt("$spent", 32f, if (over) accent else fg, FontWeight.Light)
        Txt("  / $budget", 13f, fg2, modifier = Modifier.padding(bottom = 6.dp))
    }
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(fg.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(if (over) accent else fg))
    }
    Spacer(Modifier.height(10.dp))
    rows.asReversed().take(3).forEach { (v, n) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Txt(n.ifBlank { "Expense" }, 13f, fg, modifier = Modifier.weight(1f))
            Txt(v.toString(), 12f, fg2)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniField(amt, "Amount", fg, Modifier.width(70.dp), numeric = true) { amt = it.filter(Char::isDigit) }
        Spacer(Modifier.width(8.dp))
        MiniField(note, "For what", fg, Modifier.weight(1f)) { note = it }
        val add: () -> Unit = {
            val v = amt.toIntOrNull()
            if (v != null && v > 0) {
                vm.setCfg(w.id, "log", encodeRows(decodeRows(w.get("log")) + listOf(listOf(v.toString(), note))))
                amt = ""; note = ""
            }
        }
        Txt("Add", 13f, accent, modifier = Modifier.tap(add).padding(start = 8.dp))
    }
}

private fun monthLabel(): String = LocalDate.now().month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())

// ---------------------------------------------------------------------------------------------
// 2. Planner - one line per weekday
// ---------------------------------------------------------------------------------------------

@Composable
fun PlannerContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val today = rememberNow().toLocalDate()
    val lines = w.get("lines").split("\u0001").let { if (it.size == 7) it else List(7) { "" } }
    var editing by remember(w.id) { mutableStateOf(-1) }
    var draft by remember(w.id) { mutableStateOf("") }

    Txt("Planner", 11f, fg2)
    Spacer(Modifier.height(6.dp))
    for (i in 0..6) {
        val day = today.with(java.time.DayOfWeek.MONDAY).plusDays(i.toLong())
        val isToday = day == today
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
            Txt(
                day.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.getDefault()), 12f,
                if (isToday) accent else fg2, if (isToday) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.width(34.dp)
            )
            if (editing == i) {
                MiniField(draft, "Plan for the day", fg, Modifier.weight(1f), onGo = {
                    vm.setCfg(w.id, "lines", lines.toMutableList().apply { this[i] = draft }.joinToString("\u0001"))
                    editing = -1
                }) { draft = it }
            } else {
                Txt(
                    lines[i].ifBlank { "-" }, 13f, if (lines[i].isBlank()) fg2 else fg, maxLines = 2,
                    modifier = Modifier.weight(1f).tap { editing = i; draft = lines[i] }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 3. Reminders - short label + time, sorted, no OS alarm (visual only, tap to dismiss)
// ---------------------------------------------------------------------------------------------

@Composable
fun RemindersContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color) {
    val rows = decodeRows(w.get("items")).sortedBy { it.getOrNull(0) ?: "" }
    var time by remember(w.id) { mutableStateOf("") }
    var label by remember(w.id) { mutableStateOf("") }

    Txt("Reminders", 11f, fg2)
    Spacer(Modifier.height(6.dp))
    if (rows.isEmpty()) Txt("Nothing set", 13f, fg2, modifier = Modifier.padding(vertical = 4.dp))
    rows.forEach { r ->
        val t = r.getOrNull(0).orEmpty()
        val l = r.getOrNull(1).orEmpty()
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(t, 14f, fg, FontWeight.Medium, modifier = Modifier.width(56.dp))
            Txt(l.ifBlank { "Reminder" }, 13f, fg, modifier = Modifier.weight(1f))
            Txt("x", 13f, fg2, modifier = Modifier.tap {
                vm.setCfg(w.id, "items", encodeRows(decodeRows(w.get("items")).filterNot { it == r }))
            }.padding(6.dp))
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniField(time, "18:30", fg, Modifier.width(56.dp), numeric = false) { v -> time = v.filter { it.isDigit() || it == ':' }.take(5) }
        Spacer(Modifier.width(8.dp))
        MiniField(label, "Label", fg, Modifier.weight(1f)) { label = it }
        val add: () -> Unit = {
            if (time.matches(Regex("\\d{1,2}:\\d{2}"))) {
                vm.setCfg(w.id, "items", encodeRows(decodeRows(w.get("items")) + listOf(listOf(time, label))))
                time = ""; label = ""
            }
        }
        Txt("Add", 13f, LocalColors.current.accent, modifier = Modifier.tap(add).padding(start = 8.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// 4. Pomodoro - 25/5 (configurable) auto-switching cycle
// ---------------------------------------------------------------------------------------------

@Composable
fun PomodoroContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val focusMin = w.get("focus", "25").toIntOrNull() ?: 25
    val breakMin = w.get("brk", "5").toIntOrNull() ?: 5
    val phase = w.get("phase", "focus")
    val start = w.get("start", "0").toLongOrNull() ?: 0L
    val rounds = w.get("rounds", "0").toIntOrNull() ?: 0
    val totalMs = (if (phase == "focus") focusMin else breakMin) * 60_000L
    var now by remember(w.id) { mutableLongStateOf(System.currentTimeMillis()) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(start, phase, w.id) {
        while (start > 0L) {
            now = System.currentTimeMillis()
            val left = totalMs - (now - start)
            if (left <= 0L) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val nextPhase = if (phase == "focus") "break" else "focus"
                val nextRounds = if (phase == "focus") rounds + 1 else rounds
                vm.updateWidget(w.id) { it.put("phase", nextPhase).put("rounds", nextRounds.toString()).put("start", System.currentTimeMillis().toString()) }
                break
            }
            delay(500)
        }
    }

    val left = if (start > 0L) (totalMs - (now - start)).coerceAtLeast(0L) else totalMs
    val secs = (left / 1000).toInt()
    val text = "%02d:%02d".format(secs / 60, secs % 60)
    val running = start > 0L

    Txt(if (phase == "focus") "Pomodoro  -  Focus" else "Pomodoro  -  Break", 11f, fg2)
    Spacer(Modifier.height(8.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        DotText(text, Modifier.fillMaxWidth(0.7f).aspectRatio(dotCols(text) / 7f), if (phase == "break") accent else fg, accent)
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Txt(if (running) "Pause" else "Start", 13f, fg, modifier = Modifier.tap {
            vm.updateWidget(w.id) { it.put("start", if (running) "0" else System.currentTimeMillis().toString()) }
        }.padding(end = 14.dp))
        Txt("Skip", 13f, fg2, modifier = Modifier.tap {
            val nextPhase = if (phase == "focus") "break" else "focus"
            vm.updateWidget(w.id) { it.put("phase", nextPhase).put("start", "0") }
        }.padding(end = 14.dp))
        Txt("$rounds done", 12f, fg2)
    }
}

// ---------------------------------------------------------------------------------------------
// 5. Flashcards - tap to flip, swipe-free (tap arrows) navigation
// ---------------------------------------------------------------------------------------------

@Composable
fun FlashcardsContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val cards = decodeRows(w.get("cards"))
    var idx by remember(w.id) { mutableStateOf(0) }
    var showBack by remember(w.id) { mutableStateOf(false) }
    var front by remember(w.id) { mutableStateOf("") }
    var back by remember(w.id) { mutableStateOf("") }

    Row(Modifier.fillMaxWidth()) {
        Txt("Flashcards", 11f, fg2, modifier = Modifier.weight(1f))
        if (cards.isNotEmpty()) Txt("${idx + 1}/${cards.size}", 11f, fg2)
    }
    Spacer(Modifier.height(8.dp))
    if (cards.isEmpty()) {
        Txt("Add a card below to start.", 13f, fg2, maxLines = 2)
    } else {
        val safeIdx = idx.coerceIn(0, cards.lastIndex)
        val card = cards[safeIdx]
        AnimatedContent(
            targetState = showBack,
            transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
            label = "flip"
        ) { back2 ->
            Box(
                Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(18.dp))
                    .background(fg.copy(alpha = 0.08f)).tap { showBack = !showBack }.padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Txt(
                    (if (back2) card.getOrNull(1) else card.getOrNull(0)).orEmpty().ifBlank { "-" },
                    16f, fg, FontWeight.Medium, maxLines = 3, align = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Txt("Prev", 13f, fg2, modifier = Modifier.tap { idx = (safeIdx - 1 + cards.size) % cards.size; showBack = false })
            Spacer(Modifier.weight(1f))
            Txt("Remove", 13f, accent, modifier = Modifier.tap {
                vm.setCfg(w.id, "cards", encodeRows(cards.filterIndexed { i, _ -> i != safeIdx }))
                idx = 0; showBack = false
            })
            Spacer(Modifier.weight(1f))
            Txt("Next", 13f, fg2, modifier = Modifier.tap { idx = (safeIdx + 1) % cards.size; showBack = false })
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniField(front, "Front", fg, Modifier.weight(1f)) { front = it }
        Spacer(Modifier.width(8.dp))
        MiniField(back, "Back", fg, Modifier.weight(1f)) { back = it }
        val add: () -> Unit = {
            if (front.isNotBlank()) {
                vm.setCfg(w.id, "cards", encodeRows(cards + listOf(listOf(front, back))))
                front = ""; back = ""
            }
        }
        Txt("Add", 13f, accent, modifier = Modifier.tap(add).padding(start = 8.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// 6. Habit tracker - up to 4 habits, 7 day dots, tap today to toggle
// ---------------------------------------------------------------------------------------------

@Composable
fun HabitsContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val names = w.get("names", "").split("|").filter { it.isNotBlank() }
    val today = rememberNow().toLocalDate()
    Txt("Habits", 11f, fg2)
    Spacer(Modifier.height(8.dp))
    if (names.isEmpty()) {
        Txt("Set up to 4 habits in Edit.", 13f, fg2, maxLines = 2)
        return
    }
    names.take(4).forEachIndexed { hi, name ->
        val doneDates = w.get("log_$hi").split(",").filter { it.isNotBlank() }.toSet()
        var streak = 0
        var d = today
        while (d.toString() in doneDates) { streak++; d = d.minusDays(1) }
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(name, 13f, fg, modifier = Modifier.weight(1f), maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (back in 6 downTo 0) {
                    val day = today.minusDays(back.toLong())
                    val done = day.toString() in doneDates
                    val isToday = back == 0
                    Box(
                        Modifier.size(11.dp).clip(CircleShape)
                            .background(if (done) accent else fg.copy(alpha = 0.14f))
                            .then(if (isToday) Modifier.border(1.dp, fg2, CircleShape) else Modifier)
                            .then(
                                if (isToday) Modifier.tap {
                                    val key = today.toString()
                                    val next = if (key in doneDates) doneDates - key else doneDates + key
                                    vm.setCfg(w.id, "log_$hi", next.joinToString(","))
                                } else Modifier
                            )
                    )
                }
            }
            if (streak > 0) Txt(" $streak", 11f, fg2, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// 7. GPA calculator - grade + credit rows, running weighted average
// ---------------------------------------------------------------------------------------------

@Composable
fun GpaContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val rows = decodeRows(w.get("rows")) // [course, gradePoint(0-10), credits]
    var course by remember(w.id) { mutableStateOf("") }
    var grade by remember(w.id) { mutableStateOf("") }
    var credits by remember(w.id) { mutableStateOf("") }

    val totalCredits = rows.sumOf { it.getOrNull(2)?.toDoubleOrNull() ?: 0.0 }
    val weighted = rows.sumOf { (it.getOrNull(1)?.toDoubleOrNull() ?: 0.0) * (it.getOrNull(2)?.toDoubleOrNull() ?: 0.0) }
    val gpa = if (totalCredits > 0) weighted / totalCredits else 0.0

    Txt("GPA calculator", 11f, fg2)
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
        Txt("%.2f".format(gpa), 32f, fg, FontWeight.Light)
        Txt("  ${rows.size} courses", 12f, fg2, modifier = Modifier.padding(bottom = 6.dp))
    }
    Spacer(Modifier.height(8.dp))
    rows.asReversed().take(3).forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Txt(r.getOrNull(0).orEmpty().ifBlank { "Course" }, 13f, fg, modifier = Modifier.weight(1f))
            Txt("${r.getOrNull(1)} pt  x  ${r.getOrNull(2)} cr", 12f, fg2)
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniField(course, "Course", fg, Modifier.weight(1f)) { course = it }
        Spacer(Modifier.width(6.dp))
        MiniField(grade, "Pt", fg, Modifier.width(40.dp), numeric = true) { grade = it.filter { c -> c.isDigit() || c == '.' } }
        Spacer(Modifier.width(6.dp))
        MiniField(credits, "Cr", fg, Modifier.width(40.dp), numeric = true) { credits = it.filter { c -> c.isDigit() } }
        val add: () -> Unit = {
            val g = grade.toDoubleOrNull(); val c2 = credits.toDoubleOrNull()
            if (course.isNotBlank() && g != null && c2 != null && c2 > 0) {
                vm.setCfg(w.id, "rows", encodeRows(rows + listOf(listOf(course, g.toString(), c2.toString()))))
                course = ""; grade = ""; credits = ""
            }
        }
        Txt("Add", 13f, accent, modifier = Modifier.tap(add).padding(start = 6.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// 8. Water tracker - glasses today, resets automatically on a new day
// ---------------------------------------------------------------------------------------------

@Composable
fun WaterContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val today = rememberNow().toLocalDate().toString()
    LaunchedEffect(today, w.id) {
        if (w.get("date") != today) vm.updateWidget(w.id) { it.put("date", today).put("count", "0") }
    }
    val goal = w.get("goal", "8").toIntOrNull() ?: 8
    val count = w.get("count", "0").toIntOrNull() ?: 0

    Txt("Water", 11f, fg2)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until goal) {
            val filled = i < count
            Box(
                Modifier.size(18.dp).padding(1.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (filled) accent else fg.copy(alpha = 0.14f))
                    .tap { vm.setCfg(w.id, "count", (i + 1).coerceIn(0, goal).toString()) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Txt("$count of $goal glasses", 13f, fg)
        Spacer(Modifier.weight(1f))
        Txt("Reset", 12f, fg2, modifier = Modifier.tap { vm.setCfg(w.id, "count", "0") })
    }
}

// ---------------------------------------------------------------------------------------------
// 9. Formula card - one pinned block of text, tap to edit
// ---------------------------------------------------------------------------------------------

@Composable
fun FormulasContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color) {
    var editing by remember(w.id) { mutableStateOf(false) }
    var title by remember(w.id) { mutableStateOf(w.get("title", "Formula")) }
    var body by remember(w.id) { mutableStateOf(w.get("body")) }

    if (editing) {
        MiniField(title, "Title", fg) { title = it }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            if (body.isEmpty()) Txt("Write your formula or note", 13f, fg.copy(alpha = 0.5f))
            BasicTextField(
                body, { body = it },
                textStyle = TextStyle(color = fg, fontSize = 14.sp),
                cursorBrush = SolidColor(fg),
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
        }
        Spacer(Modifier.height(8.dp))
        Txt("Save", 13f, LocalColors.current.accent, modifier = Modifier.tap {
            vm.updateWidget(w.id) { it.put("title", title).put("body", body) }
            editing = false
        })
    } else {
        Txt(w.get("title", "Formula"), 13f, fg2)
        Spacer(Modifier.height(6.dp))
        Txt(
            w.get("body").ifBlank { "Tap to add a formula or note" }, 16f, fg, maxLines = 8,
            modifier = Modifier.tap { title = w.get("title", "Formula"); body = w.get("body"); editing = true }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// 10. Assignments - title + due date, nearest first, colour warns when close
// ---------------------------------------------------------------------------------------------

@Composable
fun AssignmentsContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val rows = decodeRows(w.get("items")) // [title, date]
        .mapNotNull { r -> val d = r.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }; if (d != null) Triple(r[0], d, r) else null }
        .sortedBy { it.second }
    var title by remember(w.id) { mutableStateOf("") }
    var date by remember(w.id) { mutableStateOf("") }

    Txt("Assignments", 11f, fg2)
    Spacer(Modifier.height(6.dp))
    if (rows.isEmpty()) Txt("Nothing due. Add one below.", 13f, fg2, maxLines = 2)
    rows.take(4).forEach { (name, due, raw) ->
        val days = ChronoUnit.DAYS.between(LocalDate.now(), due).toInt()
        val soon = days <= 2
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(name, 13f, fg, modifier = Modifier.weight(1f), maxLines = 1)
            Txt(
                when { days < 0 -> "overdue"; days == 0 -> "today"; days == 1 -> "tomorrow"; else -> "${days}d" },
                12f, if (soon) accent else fg2
            )
            Txt("x", 13f, fg2, modifier = Modifier.tap {
                vm.setCfg(w.id, "items", encodeRows(decodeRows(w.get("items")).filterNot { it == raw }))
            }.padding(start = 8.dp))
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniField(title, "Assignment", fg, Modifier.weight(1f)) { title = it }
        Spacer(Modifier.width(8.dp))
        MiniField(date, "YYYY-MM-DD", fg, Modifier.width(96.dp)) { date = it }
        val add: () -> Unit = {
            if (title.isNotBlank() && runCatching { LocalDate.parse(date) }.isSuccess) {
                vm.setCfg(w.id, "items", encodeRows(decodeRows(w.get("items")) + listOf(listOf(title, date))))
                title = ""; date = ""
            }
        }
        Txt("Add", 13f, accent, modifier = Modifier.tap(add).padding(start = 8.dp))
    }
}
