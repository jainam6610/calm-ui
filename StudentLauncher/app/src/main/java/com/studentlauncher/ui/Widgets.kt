package com.studentlauncher.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetBg
import com.studentlauncher.data.WidgetItem
import com.studentlauncher.mindful.ui.MindfulTodayContent
import com.studentlauncher.mindful.ui.MindfulWeekContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------------------------
// Dot-matrix helpers (Nothing style, drawn on canvas so no font is needed)
// ---------------------------------------------------------------------------------------------

private val GLYPHS: Map<Char, List<String>> = mapOf(
    '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    '3' to listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    ':' to listOf("0", "0", "1", "0", "1", "0", "0")
)

fun dotCols(text: String): Int {
    var n = 0
    text.forEachIndexed { i, ch -> n += (GLYPHS[ch]?.first()?.length ?: 0) + if (i < text.length - 1) 1 else 0 }
    return max(1, n)
}

@Composable
fun DotText(text: String, modifier: Modifier, color: Color, accent: Color) {
    Canvas(modifier) {
        val cols = dotCols(text)
        val cell = size.width / cols
        val r = cell * 0.36f
        var cx = 0
        for (ch in text) {
            val g = GLYPHS[ch] ?: continue
            for (row in g.indices) for (col in g[row].indices) {
                val lit = g[row][col] == '1'
                val base = if (ch == ':') accent else color
                drawCircle(
                    color = if (lit) base else color.copy(alpha = 0.10f),
                    radius = r,
                    center = Offset((cx + col + 0.5f) * cell, (row + 0.5f) * cell)
                )
            }
            cx += g[0].length + 1
        }
    }
}

@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - (System.currentTimeMillis() % 60_000L) + 50L)
        }
    }
    return now
}

private val MESSAGES = listOf(
    "Start with ten minutes.",
    "Small sessions add up.",
    "You have done harder things.",
    "Phone down. Page open.",
    "One chapter, then a break.",
    "Future you will be glad."
)

private val ONES = listOf("twelve", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven")
private val TEENS = listOf("ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
private val UNITS = listOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty")

private fun hourWord(h: Int) = ONES[h % 12]
private fun minuteWords(m: Int): String = when {
    m == 0 -> "o'clock"
    m < 10 -> "oh ${UNITS[m]}"
    m < 20 -> TEENS[m - 10]
    else -> TENS[m / 10] + if (m % 10 == 0) "" else " ${UNITS[m % 10]}"
}

fun parseTasks(s: String): List<Pair<Boolean, String>> =
    s.split("\n").filter { it.length > 2 }.map { (it[0] == '1') to it.drop(2) }

fun encodeTasks(l: List<Pair<Boolean, String>>): String =
    l.joinToString("\n") { (if (it.first) "1" else "0") + "|" + it.second }

// ---------------------------------------------------------------------------------------------
// Widget area
// ---------------------------------------------------------------------------------------------

internal fun buildRows(items: List<WidgetItem>): List<List<WidgetItem>> {
    val rows = mutableListOf<List<WidgetItem>>()
    var i = 0
    while (i < items.size) {
        val a = items[i]
        val b = items.getOrNull(i + 1)
        if (a.effectiveHalf && b != null && b.effectiveHalf) { rows.add(listOf(a, b)); i += 2 } else { rows.add(listOf(a)); i += 1 }
    }
    return rows
}

@Composable
private fun Appear(content: @Composable () -> Unit) {
    val motion = LocalMotionScale.current
    val s = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = s,
        enter = if (motion < .05f) fadeIn(tween(0)) else fadeIn(tween((180 * motion).toInt())) + scaleIn(Motion.Macro, initialScale = 0.96f) + expandVertically(tween((220 * motion).toInt())),
        exit = fadeOut() + scaleOut(Motion.Macro, targetScale = 0.96f) + shrinkVertically(tween((180 * motion).toInt()))
    ) { content() }
}

@Composable
fun Widgets(vm: LauncherViewModel, padStart: Dp, padEnd: Dp, compact: Boolean, maxH: Dp, modifier: Modifier) {
    val c = LocalColors.current
    val rows = buildRows(vm.homeWidgets.toList())
    val scroll = rememberScrollState()
    val arrangeHome = vm.selectedWidgetId != null && vm.homeWidgets.any { it.id == vm.selectedWidgetId }
    var viewBottom by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    // Selection first reveals: a frame corner under the window clip scrolls into view,
    // then the scroll-away exit arms from the settled position (so the reveal itself
    // never counts as the user browsing away).
    LaunchedEffect(vm.selectedWidgetId, scroll) {
        val sel = vm.selectedWidgetId ?: return@LaunchedEffect
        val over = vm.frameBottomPx - viewBottom + 20f
        if (over > 0f && viewBottom < Float.MAX_VALUE) scroll.animateScrollBy(over)
        val anchor = scroll.value
        snapshotFlow { scroll.value }.first { it != anchor }
        if (vm.selectedWidgetId == sel) vm.selectedWidgetId = null
    }
    Box(
        modifier.fillMaxWidth().heightIn(max = maxH)
            .onGloballyPositioned { viewBottom = it.boundsInRoot().bottom }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(start = padStart, end = padEnd),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                key(row.joinToString { it.id }) {
                    Appear {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            row.forEach { w -> Box(Modifier.weight(1f)) { WidgetBody(w, vm, compact) } }
                        }
                    }
                }
            }
            if (rows.isEmpty()) {
                Box(Modifier.fillMaxWidth().glass(24.dp).tap { vm.widgetCenter = true }.padding(18.dp), contentAlignment = Alignment.Center) {
                    Txt("No widgets yet. Tap to add one.", 13f, c.fg2)
                }
            }
        }
        ArrangeBar(vm, arrangeHome, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
    }
}

@Composable
private fun TodayContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val usage = vm.getTodayUsage()
    val totalMin = usage.sumOf { it.minutes }
    val budget = vm.dailyBudgetMin
    val fraction = if (budget > 0) (totalMin.toFloat() / budget).coerceIn(0f, 1f) else 0f
    Txt("Today", 11f, fg2)
    Spacer(Modifier.height(8.dp))
    // Budget bar
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(fg.copy(alpha = 0.1f))) {
        Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(if (fraction > 0.8f) Color(0xFFFF6B6B) else accent))
    }
    Spacer(Modifier.height(4.dp))
    Txt("$totalMin / $budget min", 11f, fg2)
    if (usage.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        usage.sortedByDescending { it.minutes }.take(4).forEach { u ->
            val label = vm.appFor(u.pkg)?.label ?: u.pkg.substringAfterLast('.')
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Txt(label, 12f, fg, modifier = Modifier.weight(1f))
                Txt("${u.minutes} min", 12f, fg2)
            }
        }
    }
    if (vm.mindfulEnabled) {
        val pauses = vm.getPauseCount()
        if (pauses > 0) {
            Spacer(Modifier.height(6.dp))
            Txt("Paused $pauses times", 11f, fg2)
        }
    }
}

@Composable
private fun WidgetFrame(w: WidgetItem, vm: LauncherViewModel, content: @Composable ColumnScope.(Color, Color) -> Unit) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val fg = toneColor(w.tone, c)
    val fg2 = fg.copy(alpha = 0.6f)
    val selected = vm.selectedWidgetId == w.id
    // Single border source: this frame draws both mode and accent borders for Home and
    // Board alike, so BoardWidget must not add its own accent border on top (W5).
    val onBoard = vm.boardWidgets.any { it.id == w.id }
    val arranging = vm.selectedWidgetId != null &&
        (if (onBoard) vm.boardWidgets else vm.homeWidgets).any { it.id == vm.selectedWidgetId }
    val stroke = when {
        selected -> c.accent.copy(alpha = 0.6f)
        arranging -> c.fg2.copy(alpha = 0.45f)
        else -> c.fg2.copy(alpha = 0.22f)
    }

    Column(Modifier.fillMaxWidth().then(if (selected) Modifier.animateContentSize(Motion.Size) else Modifier)) {
        // Widget content with long-press to select
        Box(
            Modifier
                .fillMaxWidth()
                // Reveal math: Widgets() reads this to scroll a clipped corner into view.
                .onGloballyPositioned { vm.frameBottomPx = it.boundsInRoot().bottom }
                .then(
                    when (w.bg) {
                        WidgetBg.Glass -> Modifier.glass(12.dp).border(1.dp, stroke, RoundedCornerShape(12.dp))
                        WidgetBg.Solid -> Modifier.clip(RoundedCornerShape(12.dp)).background(if (c.dark) Color(0xFF1C1C1E) else Color.White).border(1.dp, stroke, RoundedCornerShape(12.dp))
                        WidgetBg.None -> if (selected || arranging) Modifier.border(1.dp, stroke, RoundedCornerShape(12.dp)) else Modifier
                    }
                )
                .pointerInput(w.id) {
                    detectTapGestures(
                        // In Arrange mode a plain tap re-targets (content taps win where they consume).
                        onTap = { if (vm.selectedWidgetId != null) vm.selectedWidgetId = w.id },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.selectedWidgetId = w.id
                            if (!vm.resizeHintShown) {
                                vm.resizeHintShown = true
                                vm.toast = "Drag corner to resize"
                            }
                        }
                    )
                }
                .padding(if (w.bg == WidgetBg.None) 6.dp else 12.dp)
        ) {
            Column { content(fg, fg2) }
            if (selected) {
                // Resize handle: one gesture, both axes. Claims on the first slop-crossing of
                // either axis; leaf runs before parent scroll / Board drag / HorizontalPager.
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(44.dp)
                        .pointerInput(w.id) { // w.id ONLY — a height/half key would cancel mid-drag
                            awaitEachGesture {
                                var accumV = 0f
                                var accumH = 0f
                                var markV = 0
                                var claimed = false
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val slop = viewConfiguration.touchSlop
                                val startX = down.position.x
                                val startY = down.position.y
                                val stepPx = 36.dp.toPx()
                                val flipPx = 40.dp.toPx()
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val ch = ev.changes.first()
                                    if (!ch.pressed) break
                                    if (!claimed) {
                                        if (ch.isConsumed) continue // another gesture took it
                                        val dx = ch.position.x - startX
                                        val dy = ch.position.y - startY
                                        if (abs(dx) <= slop && abs(dy) <= slop) continue
                                        accumV = if (dy > 0f) dy - slop else dy + slop
                                        accumH = if (dx > 0f) dx - slop else dx + slop
                                        claimed = true
                                        ch.consume()
                                    } else {
                                        accumV += ch.position.y - ch.previousPosition.y
                                        accumH += ch.position.x - ch.previousPosition.x
                                        ch.consume()
                                    }

                                    // Live commit: one per step, so each step haptics and the
                                    // running gesture must survive the recomposition it causes.
                                    val cur = vm.findWidget(w.id) ?: break
                                    val steps = (accumV / stepPx).toInt()
                                    if (steps != markV) {
                                        val newH = (cur.height + steps - markV).coerceIn(1, 3)
                                        if (newH != cur.height) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            vm.setWidgetHeight(w.id, newH)
                                        }
                                        markV = steps // advance even at the clamp, else drag-back never applies
                                    }
                                    if (abs(accumH) > flipPx) {
                                        val want = accumH < 0f // drag left = half, right = full
                                        if (want != cur.half) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            vm.updateWidget(w.id) { it.copy(half = want) }
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Corner bracket — fuses with the accent border, legible at any widget height.
                    Box(Modifier.size(18.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(c.accent)
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(c.accent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelChip(label: String, modifier: Modifier, danger: Boolean = false, filled: Boolean = false, onClick: () -> Unit) {
    val c = LocalColors.current
    val bg = if (filled) c.fg else c.track
    val fg = when {
        filled -> c.knobOn
        danger -> c.accent
        else -> c.fg
    }
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg).tap(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Txt(label, 13f, fg) }
}

/** Shared ↑/↓ move for the Arrange bar: Board-aware (skips half-pair partners), else home order. */
private fun moveVert(vm: LauncherViewModel, w: WidgetItem, delta: Int, haptic: HapticFeedback) {
    val i = vm.boardWidgets.indexOfFirst { it.id == w.id }
    if (i >= 0) { // Board → row-aware, skips half-pair partners
        val (up, down) = boardVerticalNeighbors(vm.boardWidgets.toList(), i)
        val t = if (delta < 0) up else down
        if (t == null) return // boundary no-op — no haptic
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        vm.reorderBoardWidget(i, t)
        return
    }
    val hi = vm.homeWidgets.indexOfFirst { it.id == w.id }
    if (hi < 0 || hi + delta !in vm.homeWidgets.indices) return // mirrors moveWidget bounds check
    vm.moveWidget(w.id, delta)
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/** Arrange-mode toolbar — fixed at the page bottom, shared by Home widgets and the Board.
 *  Staggered: borders/handles land at t=0, controls fade in a beat later. */
@Composable
fun ArrangeBar(vm: LauncherViewModel, onPage: Boolean, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(onPage) {
        if (onPage) {
            shown = false
            delay(130) // let mode borders land first (Niagara-style reveal)
            shown = true
        } else shown = false
    }
    AnimatedVisibility(visible = onPage && shown, enter = fadeIn(Motion.Micro), modifier = modifier) {
        val sel = vm.selectedWidgetId
        if (sel != null) {
            Row(
                Modifier.glass(20.dp).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val w = vm.findWidget(sel)
                SelChip("↑", Modifier.width(48.dp)) { if (w != null) moveVert(vm, w, -1, haptic) }
                SelChip("↓", Modifier.width(48.dp)) { if (w != null) moveVert(vm, w, 1, haptic) }
                SelChip("Edit", Modifier.width(72.dp)) { vm.editWidgetId = sel }
                SelChip("Remove", Modifier.width(88.dp), danger = true) { vm.removeWidget(sel) }
                SelChip("Done", Modifier.width(72.dp), filled = true) { vm.selectedWidgetId = null }
            }
        }
    }
}

@Composable
fun WidgetBody(w: WidgetItem, vm: LauncherViewModel, compact: Boolean) {
    if (w.type == WT.ANDROID) {
        HostedWidget(vm, w, Modifier.fillMaxWidth())
        return
    }
    WidgetFrame(w, vm) { fg, fg2 -> WidgetContent(w, vm, fg, fg2, compact) }
}

/** Frame-free widget render — the frame adds gestures; the settings-sheet preview doesn't want them. */
@Composable
fun WidgetContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, compact: Boolean) {
    val accent = LocalColors.current.accent
    // Height 1=80dp, 2=120dp, 3=180dp — controls max vertical space
    val maxContentH = when (w.height) {
        1 -> if (compact) 60.dp else 80.dp
        3 -> if (compact) 140.dp else 180.dp
        else -> if (compact) 90.dp else 120.dp
    }
    Column(Modifier.heightIn(max = maxContentH)) {
        when (w.type) {
            WT.CLOCK -> ClockContent(w, fg, fg2, accent)
            WT.DATE -> DateContent(fg, fg2)
            WT.WEEK -> WeekContent(w, fg, fg2, accent)
            WT.BATTERY -> BatteryContent(fg, accent)
            WT.NEXT -> NextContent(w, fg, fg2)
            WT.COUNTDOWN -> CountdownContent(w, fg, fg2, accent)
            WT.TIMER -> TimerContent(w, vm, fg, fg2, accent)
            WT.TASKS -> TasksContent(w, vm, fg, fg2, accent)
            WT.QUICK -> QuickContent(fg)
            WT.SPHERE -> Box(Modifier.fillMaxWidth().height(if (compact) 60.dp else 80.dp)) {
                DotSphere(Modifier.fillMaxSize(), fg, accent)
            }
            WT.GLYPH -> GlyphContent(w, vm, fg, accent, compact)
            WT.TODAY -> TodayContent(w, vm, fg, fg2, accent)
            WT.EXPENSES -> ExpensesContent(w, vm, fg, fg2, accent)
            WT.PLANNER -> PlannerContent(w, vm, fg, fg2, accent)
            WT.REMINDERS -> RemindersContent(w, vm, fg, fg2)
            WT.POMODORO -> PomodoroContent(w, vm, fg, fg2, accent)
            WT.FLASHCARDS -> FlashcardsContent(w, vm, fg, fg2, accent)
            WT.HABITS -> HabitsContent(w, vm, fg, fg2, accent)
            WT.GPA -> GpaContent(w, vm, fg, fg2, accent)
            WT.WATER -> WaterContent(w, vm, fg, fg2, accent)
            WT.FORMULAS -> FormulasContent(w, vm, fg, fg2)
            WT.ASSIGNMENTS -> AssignmentsContent(w, vm, fg, fg2, accent)
            WT.MINDFUL_TODAY -> MindfulTodayContent(vm, fg, fg2, accent)
            WT.MINDFUL_WEEK -> MindfulWeekContent(vm, fg, fg2, accent)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Widgets
// ---------------------------------------------------------------------------------------------

@Composable
private fun ClockContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val now = rememberNow()
    val ctx = LocalContext.current
    val sys24 = remember { DateFormat.is24HourFormat(ctx) }
    val is24 = when (w.get("fmt", "sys")) { "24" -> true; "12" -> false; else -> sys24 }
    val time = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "hh:mm"))
    var q by remember { mutableIntStateOf(0) }
    when (w.get("style", "Dot")) {
        "Digital" -> Txt(time, 54f, fg, FontWeight.Light)
        "Words" -> {
            Txt(hourWord(now.hour), 40f, fg, FontWeight.Bold)
            Txt(minuteWords(now.minute), 40f, fg, FontWeight.Light, maxLines = 2)
        }
        "Analog" -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            AnalogClock(now, Modifier.size(112.dp), fg, accent)
        }
        "Flip" -> FlipClock(now, is24, fg, accent, flap = false)
        "Split-Flap" -> FlipClock(now, is24, fg, accent, flap = true)
        "Retro Pixel" -> MonoText(time, 46f, fg, FontWeight.Bold)
        "Thin" -> Txt(time, 56f, fg, FontWeight.Thin)
        "Bold" -> Txt(time, 52f, fg, FontWeight.Black)
        "Neon" -> NeonText(time, accent)
        "Binary" -> MonoText(binaryTime(now, is24), 22f, accent, FontWeight.Medium)
        "Roman" -> {
            val h = ((now.hour + 11) % 12) + 1
            Txt("${roman(h)}:${roman(now.minute).ifBlank { "—" }}", 42f, fg, FontWeight.Light, maxLines = 1)
        }
        "Gradient" -> GradientText(time, accent, fg)
        else -> DotText(time, Modifier.fillMaxWidth().aspectRatio(dotCols(time) / 7f), fg, accent)
    }
    if (w.get("date", "1") == "1") {
        val fmt = w.get("dateFmt", "EEE, d MMM")
        Txt(now.format(DateTimeFormatter.ofPattern(fmt, Locale.getDefault())), 12f, fg2, modifier = Modifier.padding(top = 8.dp))
    }
    if (w.get("quote", "1") == "1") {
        AnimatedContent(
            targetState = q,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(140)) },
            label = "quote"
        ) { i ->
            Txt(MESSAGES[i], 13f, fg, maxLines = 2, modifier = Modifier.tap { q = (q + 1) % MESSAGES.size }.padding(top = 8.dp))
        }
    }
}

private val CLOCK_STYLES = listOf(
    "Dot", "Digital", "Words", "Analog", "Flip", "Split-Flap", "Retro Pixel",
    "Thin", "Bold", "Neon", "Binary", "Roman", "Gradient"
)

private val DATE_FORMATS = listOf(
    "EEE, d MMM" to "Tue, 24 Sep",
    "dd MMM" to "24 Sep",
    "d MMMM" to "24 September",
    "EEEE" to "Tuesday"
)

@Composable
private fun MonoText(text: String, size: Float, color: Color, weight: FontWeight) {
    androidx.compose.foundation.text.BasicText(
        text,
        style = TextStyle(color = color, fontSize = size.sp, fontWeight = weight, fontFamily = FontFamily.Monospace)
    )
}

@Composable
private fun NeonText(text: String, accent: Color) {
    androidx.compose.foundation.text.BasicText(
        text,
        style = TextStyle(
            color = accent,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold,
            shadow = androidx.compose.ui.graphics.Shadow(accent.copy(alpha = 0.9f), offset = androidx.compose.ui.geometry.Offset(3f, 3f), blurRadius = 14f)
        )
    )
}

@Composable
private fun GradientText(text: String, accent: Color, fg: Color) {
    // ponytail: Brush-on-text needs a newer TextStyle API; accent + white stack reads as a cheap gradient
    Box {
        androidx.compose.foundation.text.BasicText(
            text,
            style = TextStyle(color = accent.copy(alpha = 0.55f), fontSize = 54.sp, fontWeight = FontWeight.Bold)
        )
        androidx.compose.foundation.text.BasicText(
            text,
            style = TextStyle(color = fg, fontSize = 54.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.graphicsLayer { translationY = -3f * density }
        )
    }
}

@Composable
private fun FlipClock(now: LocalDateTime, is24: Boolean, fg: Color, accent: Color, flap: Boolean) {
    val h = if (is24) String.format("%02d", now.hour) else String.format("%02d", ((now.hour + 11) % 12) + 1)
    val m = String.format("%02d", now.minute)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        FlipCard(h, fg, accent, flap)
        Txt(":", 40f, fg, FontWeight.Bold)
        FlipCard(m, fg, accent, flap)
    }
}

@Composable
private fun FlipCard(text: String, fg: Color, accent: Color, flap: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(fg.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (flap) {
            Box(Modifier.matchParentSize().padding(vertical = 18.dp).align(Alignment.Center)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(fg.copy(alpha = 0.35f)))
            }
        }
        Txt(text, 36f, if (flap) accent else fg, FontWeight.Bold)
    }
}

private fun binaryTime(now: LocalDateTime, is24: Boolean): String {
    val h = if (is24) now.hour else ((now.hour + 11) % 12) + 1
    fun d(n: Int) = Integer.toBinaryString(n).padStart(4, '0')
    return "${d(h / 10)} ${d(h % 10)}:${d(now.minute / 10)} ${d(now.minute % 10)}"
}

private fun roman(n: Int): String {
    if (n <= 0) return ""
    val vals = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
    val syms = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
    val sb = StringBuilder()
    var v = n
    for (i in vals.indices) while (v >= vals[i]) { sb.append(syms[i]); v -= vals[i] }
    return sb.toString()
}

private fun polar(c: Offset, r: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(c.x + r * sin(a).toFloat(), c.y - r * cos(a).toFloat())
}

@Composable
private fun AnalogClock(now: LocalDateTime, modifier: Modifier, fg: Color, accent: Color) {
    Canvas(modifier) {
        val r = min(size.width, size.height) / 2f
        val c = center
        for (i in 0 until 60) {
            val big = i % 5 == 0
            drawCircle(
                color = fg.copy(alpha = if (big) 0.9f else 0.35f),
                radius = if (big) r * 0.035f else r * 0.018f,
                center = polar(c, r * 0.92f, i * 6f)
            )
        }
        val hDeg = (now.hour % 12 + now.minute / 60f) * 30f
        val mDeg = now.minute * 6f
        drawLine(fg, c, polar(c, r * 0.5f, hDeg), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(accent, c, polar(c, r * 0.78f, mDeg), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(accent, 4.dp.toPx(), c)
    }
}

@Composable
private fun DateContent(fg: Color, fg2: Color) {
    val now = rememberNow()
    Txt(now.dayOfMonth.toString(), 60f, fg, FontWeight.Light)
    Txt(now.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault()), 13f, fg)
    Txt(now.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()), 12f, fg2)
}

@Composable
private fun WeekContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val today = rememberNow().toLocalDate()
    val mon = w.get("mon", "1") == "1"
    val offset = if (mon) today.dayOfWeek.value - 1 else today.dayOfWeek.value % 7
    val start = today.minusDays(offset.toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..6) {
            val d = start.plusDays(i.toLong())
            val isToday = d == today
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(d.dayOfWeek.getDisplayName(JTextStyle.NARROW, Locale.getDefault()), 11f, fg2)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(if (isToday) accent else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) { Txt(d.dayOfMonth.toString(), 13f, if (isToday) Color.White else fg) }
            }
        }
    }
}

@Composable
private fun BatteryContent(fg: Color, accent: Color) {
    val ctx = LocalContext.current
    var pct by remember { mutableIntStateOf(100) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0 && s > 0) pct = l * 100 / s
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
            }
            delay(30_000)
        }
    }
    Box(Modifier.fillMaxWidth(0.8f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.dp.toPx()
            val tl = Offset(sw / 2f, sw / 2f)
            val sz = Size(size.width - sw, size.height - sw)
            drawArc(fg.copy(alpha = 0.12f), -90f, 360f, false, tl, sz, style = Stroke(sw))
            drawArc(if (charging) accent else fg, -90f, 360f * pct / 100f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Txt("$pct%", 20f, fg, FontWeight.Medium)
    }
}

@Composable
private fun NextContent(w: WidgetItem, fg: Color, fg2: Color) {
    Txt("Next", 11f, fg2)
    Spacer(Modifier.height(18.dp))
    Txt(w.get("title").ifBlank { "Nothing yet" }, 18f, fg, FontWeight.Medium, maxLines = 2)
    Txt(w.get("time").ifBlank { "Long-press to set" }, 12f, fg2, modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun CountdownContent(w: WidgetItem, fg: Color, fg2: Color, accent: Color) {
    val d = runCatching { LocalDate.parse(w.get("date")) }.getOrNull()
    val days = d?.let { ChronoUnit.DAYS.between(LocalDate.now(), it).toInt().coerceAtLeast(0) }
    Txt(w.get("title").ifBlank { "Exam" }, 13f, fg2)
    Spacer(Modifier.height(12.dp))
    if (days != null) {
        val s = days.toString()
        DotText(s, Modifier.height(30.dp).aspectRatio(dotCols(s) / 7f), fg, accent)
        Txt(if (days == 1) "day left" else "days left", 12f, fg2, modifier = Modifier.padding(top = 8.dp))
    } else {
        Txt("Long-press to set a date", 12f, fg2, maxLines = 2)
    }
}

@Composable
private fun TimerContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val minutes = w.get("min", "25").toIntOrNull() ?: 25
    val start = w.get("start", "0").toLongOrNull() ?: 0L
    val total = minutes * 60_000L
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val haptic = LocalHapticFeedback.current
    val left = if (start > 0L) (total - (now - start)).coerceAtLeast(0L) else total
    val done = start > 0L && left == 0L

    LaunchedEffect(start) {
        while (start > 0L) {
            now = System.currentTimeMillis()
            if (now - start >= total) break
            delay(1000)
        }
    }
    LaunchedEffect(done) { if (done) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    val secs = (left / 1000).toInt()
    val text = "%02d:%02d".format(secs / 60, secs % 60)
    Txt("Study timer", 11f, fg2)
    Spacer(Modifier.height(10.dp))
    DotText(text, Modifier.fillMaxWidth().aspectRatio(dotCols(text) / 7f), if (done) accent else fg, accent)
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        WPill(if (start > 0L) "Reset" else "Start", fg) {
            vm.setCfg(w.id, "start", if (start > 0L) "0" else System.currentTimeMillis().toString())
        }
        if (done) Txt("Done. Take a break.", 12f, accent, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun WPill(label: String, fg: Color, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.clip(CircleShape).background(fg.copy(alpha = 0.14f)).tap {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }.padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Txt(label, 12f, fg) }
}

@Composable
private fun TasksContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, fg2: Color, accent: Color) {
    val tasks = parseTasks(w.get("items"))
    var draft by remember { mutableStateOf("") }
    val add: () -> Unit = {
        val t = draft.trim()
        if (t.isNotEmpty()) {
            vm.setCfg(w.id, "items", encodeTasks(tasks + (false to t)))
            draft = ""
        }
    }
    Txt("Tasks", 11f, fg2)
    tasks.forEachIndexed { idx, t ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(20.dp).clip(CircleShape)
                    .background(if (t.first) accent else Color.Transparent)
                    .border(1.5.dp, if (t.first) accent else fg2, CircleShape)
                    .tap {
                        vm.setCfg(w.id, "items", encodeTasks(tasks.mapIndexed { i, x -> if (i == idx) (!x.first) to x.second else x }))
                    }
            )
            Spacer(Modifier.width(10.dp))
            Txt(t.second, 14f, if (t.first) fg2 else fg, modifier = Modifier.weight(1f), strike = t.first)
            Txt(
                "x", 14f, fg2,
                modifier = Modifier.tap { vm.setCfg(w.id, "items", encodeTasks(tasks.filterIndexed { i, _ -> i != idx })) }.padding(6.dp)
            )
        }
    }
    if (tasks.size < 6) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (draft.isEmpty()) Txt("Add a task", 14f, fg2)
                BasicTextField(
                    draft, { draft = it }, singleLine = true,
                    textStyle = TextStyle(color = fg, fontSize = 14.sp),
                    cursorBrush = SolidColor(fg),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Txt("Add", 13f, accent, modifier = Modifier.tap { add() }.padding(6.dp))
        }
    }
}

private val QUICK = listOf(
    "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
    "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
    "Sound" to Settings.ACTION_SOUND_SETTINGS,
    "Display" to Settings.ACTION_DISPLAY_SETTINGS,
    "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
    "Airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS
)

@Composable
private fun QuickContent(fg: Color) {
    val ctx = LocalContext.current
    QUICK.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (label, action) ->
                Box(
                    Modifier.weight(1f).height(38.dp).clip(CircleShape).background(fg.copy(alpha = 0.12f))
                        .tap { runCatching { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                    contentAlignment = Alignment.Center
                ) { Txt(label, 12f, fg) }
            }
        }
    }
}

private val GLYPH_MODES = listOf("Pulse", "Spiral", "Wave", "Time")

@Composable
private fun GlyphContent(w: WidgetItem, vm: LauncherViewModel, fg: Color, accent: Color, compact: Boolean) {
    val mode = w.get("mode", "Pulse")
    Box(
        Modifier.fillMaxWidth().tap {
            vm.setCfg(w.id, "mode", GLYPH_MODES[(GLYPH_MODES.indexOf(mode) + 1).mod(GLYPH_MODES.size)])
        },
        contentAlignment = Alignment.Center
    ) {
        GlyphMatrix(mode, fg, accent, Modifier.fillMaxWidth(if (compact) 0.55f else 0.62f).aspectRatio(1f))
    }
}

/** On-screen 25 x 25 dot display inspired by Nothing's Glyph Matrix. */
@Composable
fun GlyphMatrix(mode: String, color: Color, accent: Color, modifier: Modifier) {
    val now = rememberNow()
    var t by remember { mutableFloatStateOf(0f) }
    val motion = LocalMotionScale.current
    // ponytail: Time mode never reads t — skip the frame loop entirely
    LaunchedEffect(motion, mode) {
        if (mode == "Time" || motion <= .05f) return@LaunchedEffect
        var last = -1L
        while (true) {
            val n = withFrameNanos { it }
            if (last >= 0) t += (n - last) / 1e9f * motion
            last = n
        }
    }
    val lit = remember(mode, now.hour, now.minute) { if (mode == "Time") timeCells(now.hour, now.minute) else emptySet() }
    Canvas(modifier) {
        val n = 25
        val cell = size.width / n
        val r = cell * 0.36f
        for (y in 0 until n) for (x in 0 until n) {
            val dx = x - 12f
            val dy = y - 12f
            val d = sqrt(dx * dx + dy * dy)
            if (d > 12.6f) continue
            val b = when (mode) {
                "Spiral" -> (sin(atan2(dy, dx) * 2f + d * 0.7f - t * 3f) + 1f) / 2f
                "Wave" -> (sin(x * 0.55f + t * 2.4f) + cos(y * 0.45f - t * 1.7f) + 2f) / 4f
                "Time" -> if ((y * n + x) in lit) 1f else 0.08f
                else -> { val v = (sin(d * 0.9f - t * 3.2f) + 1f) / 2f; v * v }
            }.coerceIn(0f, 1f)
            val centre = d < 1.2f
            drawCircle(
                color = if (centre) accent else color.copy(alpha = 0.10f + 0.9f * b),
                radius = r,
                center = Offset((x + 0.5f) * cell, (y + 0.5f) * cell)
            )
        }
    }
}

private fun timeCells(h: Int, m: Int): Set<Int> {
    val out = mutableSetOf<Int>()
    val digits = listOf(h / 10, h % 10, m / 10, m % 10)
    digits.forEachIndexed { i, dgt ->
        val g = GLYPHS[('0' + dgt)] ?: return@forEachIndexed
        val row0 = if (i < 2) 4 else 14
        val col0 = 7 + (i % 2) * 6
        for (r in g.indices) for (c in g[r].indices) if (g[r][c] == '1') out.add((row0 + r) * 25 + col0 + c)
    }
    return out
}

// ---------------------------------------------------------------------------------------------
// 3D dot sphere
// ---------------------------------------------------------------------------------------------

@Composable
fun DotSphere(modifier: Modifier, color: Color, accent: Color) {
    val n = 260
    val pts = remember {
        FloatArray(n * 3).also { a ->
            for (i in 0 until n) {
                val y = 1f - 2f * (i + 0.5f) / n
                val r = sqrt(1f - y * y)
                val th = i * 2.39996f
                a[i * 3] = cos(th) * r; a[i * 3 + 1] = y; a[i * 3 + 2] = sin(th) * r
            }
        }
    }
    val xs = remember { FloatArray(n) }
    val ys = remember { FloatArray(n) }
    val zs = remember { FloatArray(n) }
    var yaw by remember { mutableFloatStateOf(0f) }
    var tilt by remember { mutableFloatStateOf(0.35f) }
    var dragging by remember { mutableStateOf(false) }
    val motion = LocalMotionScale.current

    LaunchedEffect(motion, dragging) {
        if (motion <= .05f) return@LaunchedEffect
        var last = -1L
        while (true) {
            val n = withFrameNanos { it }
            if (last >= 0 && !dragging) {
                yaw += (n - last) / 1e9f * 0.5f * motion
                tilt += (0.35f - tilt) * 0.05f
            }
            last = n
        }
    }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = { dragging = false },
                onDragCancel = { dragging = false }
            ) { change, drag ->
                change.consume()
                yaw += drag.x * 0.012f
                tilt = (tilt + drag.y * 0.008f).coerceIn(-0.6f, 1.2f)
            }
        }
    ) {
        val radius = min(size.width, size.height) / 2f * 0.9f
        val cy = cos(yaw); val sy = sin(yaw); val ct = cos(tilt); val st = sin(tilt)
        for (i in 0 until n) {
            val px = pts[i * 3]; val py = pts[i * 3 + 1]; val pz = pts[i * 3 + 2]
            val x1 = px * cy + pz * sy
            val z1 = -px * sy + pz * cy
            xs[i] = x1; ys[i] = py * ct - z1 * st; zs[i] = py * st + z1 * ct
        }
        val order = (0 until n).sortedBy { zs[it] }
        val c = Offset(size.width / 2f, size.height / 2f)
        for (i in order) {
            val d = (zs[i] + 1f) / 2f
            val s = 1.9f / (1.9f - zs[i] * 0.5f)
            val ring = kotlin.math.abs(ys[i]) < 0.05f
            drawCircle(
                color = if (ring) accent.copy(alpha = 0.4f + 0.6f * d) else color.copy(alpha = 0.2f + 0.8f * d),
                radius = (0.9f + 1.5f * d) * s * (radius / 100f),
                center = Offset(c.x + xs[i] * radius * s, c.y - ys[i] * radius * s)
            )
        }
    }
}
