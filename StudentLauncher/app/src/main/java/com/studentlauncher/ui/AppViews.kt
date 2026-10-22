package com.studentlauncher.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.AppInfo
import com.studentlauncher.data.Chip
import com.studentlauncher.data.IconShape
import kotlin.math.abs
import kotlin.math.max

enum class EntryMode { List, Grid, Panel, Dock }

private val GRAY = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(
    app: AppInfo,
    vm: LauncherViewModel,
    mode: EntryMode,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, Motion.Micro, label = "press")
    var rect by remember { mutableStateOf(Rect.Zero) }

    // Arrange mode owns the gestures: hold-lift-drag on the wrapper, no launch/menu races.
    val arrangingHome = vm.arrangingApps && (mode == EntryMode.List || mode == EntryMode.Grid)
    val gestures = if (arrangingHome) Modifier else Modifier.combinedClickable(
        interactionSource = src,
        indication = null,
        onClick = { onLaunch(app, rect) },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onMenu(app, rect)
        }
    )
    val press = Modifier.graphicsLayer { scaleX = s; scaleY = s }
    val iconPos = Modifier.onGloballyPositioned { rect = it.boundsInRoot() }
    val flagged = app.pkg in vm.flagged

    when (mode) {
        EntryMode.List, EntryMode.Panel -> Row(
            modifier.fillMaxWidth().then(press).then(gestures).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(app, vm, 42.dp, iconPos)
            if (vm.showNames || mode == EntryMode.Panel) {
                Spacer(Modifier.width(14.dp))
                Txt(app.label, 20f, c.fg, modifier = Modifier.weight(1f, fill = false))
                if (flagged) Box(Modifier.padding(start = 8.dp).size(6.dp).background(c.accent, CircleShape))
                if (mode == EntryMode.Panel && app.pkg in vm.studyApps) {
                    Txt("study", 11f, c.fg2, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        EntryMode.Grid -> Column(
            modifier.then(press).then(gestures).padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIconView(app, vm, 56.dp, iconPos)
            if (vm.showNames) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(app.label, 11.5f, c.fg, modifier = Modifier.weight(1f, fill = false))
                    if (flagged) Box(Modifier.padding(start = 4.dp).size(5.dp).background(c.accent, CircleShape))
                }
            }
        }
        EntryMode.Dock -> Box(modifier.then(press).then(gestures)) { AppIconView(app, vm, 48.dp, iconPos) }
    }
}

// ---------------------------------------------------------------------------------------------
// Home favourites
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeApps(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val grid = vm.layout == com.studentlauncher.data.HomeLayout.Grid
    val arranging = vm.arrangingApps
    fun visibleList() = vm.pinned.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
    val list = visibleList()
    var dragPkg by remember { mutableStateOf<String?>(null) }
    var startIdx by remember { mutableIntStateOf(0) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var cellW by remember { mutableFloatStateOf(1f) }
    var cellH by remember { mutableFloatStateOf(1f) }
    val launchGate: (AppInfo, Rect) -> Unit = { a, r -> if (!arranging) onLaunch(a, r) }

    fun onDrag(drag: Offset) {
        val pkg = dragPkg ?: return
        dx += drag.x
        dy += drag.y
        val cur = visibleList()
        val i = cur.indexOfFirst { it.pkg == pkg }
        if (i < 0) return
        val rowPx = (cellH + if (grid) with(density) { 10.dp.toPx() } else 0f).coerceAtLeast(1f)
        val rowStep = Math.round(dy / rowPx)
        val colStep = if (grid) Math.round(dx / cellW) else 0
        val target = (startIdx + rowStep * if (grid) vm.homeColumns else 1 + colStep).coerceIn(0, cur.size - 1)
        if (target != i) {
            vm.movePinned(pkg, cur[target].pkg, after = target > i)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // In rearrange mode a hold-then-drag lifts the app; plain swipes still scroll.
    fun cellMod(pkg: String) = Modifier
        .onSizeChanged {
            cellW = it.width.toFloat().coerceAtLeast(1f)
            cellH = it.height.toFloat().coerceAtLeast(1f)
        }
        .graphicsLayer {
            if (dragPkg == pkg) {
                translationX = dx
                translationY = dy
                scaleX = 1.06f
                scaleY = 1.06f
                shadowElevation = 10.dp.toPx()
            }
        }
        .pointerInput(pkg, arranging, grid) {
            if (!arranging) return@pointerInput
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragPkg = pkg
                    startIdx = visibleList().indexOfFirst { it.pkg == pkg }
                    dx = 0f
                    dy = 0f
                },
                onDragEnd = { dragPkg = null },
                onDragCancel = { dragPkg = null },
                onDrag = { _, drag -> onDrag(drag) }
            )
        }

    if (grid) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.chunked(vm.homeColumns).forEach { rowApps ->
                Row(Modifier.fillMaxWidth()) {
                    rowApps.forEach { a ->
                        Box(Modifier.weight(1f).then(cellMod(a.pkg)), contentAlignment = Alignment.Center) {
                            AppEntry(a, vm, EntryMode.Grid, launchGate, onMenu)
                        }
                    }
                    repeat(vm.homeColumns - rowApps.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Column(modifier) {
            vm.pinned.toList().forEach { pkg ->
                val a = vm.appFor(pkg)
                if (a != null) {
                    androidx.compose.runtime.key(pkg) {
                        AnimatedVisibility(
                            visible = vm.isVisible(a),
                            enter = fadeIn(Motion.Macro) + expandVertically(Motion.Size),
                            exit = fadeOut() + shrinkVertically(Motion.Size)
                        ) {
                            Box(cellMod(pkg)) { AppEntry(a, vm, EntryMode.List, launchGate, onMenu) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Alphabet scrubber (star = home, A-Z = jump, circle = settings)
// ---------------------------------------------------------------------------------------------

private val LETTERS: List<String> = listOf("*") + ('A'..'Z').map { it.toString() } + listOf("o")

@Composable
fun AlphaScrubber(vm: LauncherViewModel, letterH: Dp, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableIntStateOf(-1) }
    var heightPx by remember { mutableIntStateOf(1) }
    val available = vm.availableLetters()
    val right = vm.alphaSide == AlphaSide.Right
    val fontSize = (letterH.value * 0.78f).coerceIn(9f, 12f)
    val appAlphabet = stringResource(R.string.app_alphabet)
    val idle = stringResource(R.string.idle)

    fun indexAt(y: Float) = (y / max(1, heightPx) * 28f).toInt().coerceIn(0, 27)

    Column(
        modifier
            .width(48.dp)
            .semantics {
                contentDescription = appAlphabet
                stateDescription = if (active >= 0) LETTERS[active] else idle
            }
            .onSizeChanged { heightPx = it.height }
            .pointerInput(Unit) {
                while (true) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var i = indexAt(down.position.y)
                        active = i
                        vm.onScrub(i)
                        do {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.first()
                            val ni = indexAt(ch.position.y)
                            if (ni != i) {
                                i = ni
                                active = i
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.onScrub(i)
                            }
                            ch.consume()
                        } while (ev.changes.any { it.pressed })
                        vm.onScrubRelease(i)
                        active = -1
                    }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LETTERS.forEachIndexed { idx, label ->
            val d = if (active < 0) 99 else abs(idx - active)
            val off = animateFloatAsState(
                if (active < 0) 0f else max(0f, 26f - d * 7f),
                Motion.Micro, label = "wave"
            )
            val sc = animateFloatAsState(
                if (active < 0) 1f else 1f + max(0f, 0.55f - d * 0.16f),
                Motion.Micro, label = "waveScale"
            )
            val dim = idx in 1..26 && ('A' + (idx - 1)) !in available
            Box(
                Modifier.height(letterH).width(48.dp).graphicsLayer {
                    translationX = (if (right) -off.value else off.value) * density
                    scaleX = sc.value; scaleY = sc.value
                },
                contentAlignment = Alignment.Center
            ) {
                when (idx) {
                    0 -> Txt("\u2605", fontSize, if (d == 0) c.fg else c.fg2)
                    27 -> Box(Modifier.size(7.dp).border(1.2.dp, if (d == 0) c.fg else c.fg2, CircleShape))
                    else -> Txt(label, fontSize, (if (d == 0) c.fg else c.fg2).copy(alpha = if (dim) 0.3f else 1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// All-apps panel (apps that are not on home)
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AllAppsPanel(
    vm: LauncherViewModel,
    modifier: Modifier,
    startPad: Dp,
    endPad: Dp,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit
) {
    val c = LocalColors.current
    val searching = vm.query.isNotBlank()
    val key = if (searching) "q" else vm.letter.toString()

    Column(
        modifier.fillMaxWidth().imePadding().padding(start = startPad, end = endPad, bottom = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Plan", { vm.destination = com.studentlauncher.data.InternalDestination.Plan }, Modifier.weight(1f))
            PillButton(stringResource(R.string.settings_gear), { vm.destination = com.studentlauncher.data.InternalDestination.Settings }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Segmented(listOf(Chip.All to "All", Chip.Study to "Study"), vm.chip, { vm.chip = it }, Modifier.width(170.dp))
        Spacer(Modifier.height(12.dp))
        if (vm.normalScroll && key != "q") {
            NormalList(vm, Modifier.weight(1f).fillMaxWidth(), onLaunch, onMenu)
        } else {
            AnimatedContent(
                targetState = key,
                transitionSpec = {
                    val d = vm.letterDir
                    (slideInVertically(Motion.Offset) { it * d / 4 } + fadeIn(Motion.Macro)) togetherWith
                        (slideOutVertically(Motion.Offset) { -it * d / 4 } + fadeOut(Motion.Macro))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "panel"
            ) { k ->
                val results = vm.appsForKey(k)
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(64.dp)) {
                        if (k != "q") Txt(k, 56f, c.fg, FontWeight.Light)
                    }
                    if (results.isEmpty()) {
                        Txt(
                            if (k == "q") "No matches" else "No apps under $k",
                            14f, c.fg2, modifier = Modifier.padding(top = 18.dp)
                        )
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(results, key = { it.pkg }) { a ->
                                AppEntry(a, vm, EntryMode.Panel, onLaunch, onMenu)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SearchField(vm) {
            vm.appsForKey("q").firstOrNull()?.let { onLaunch(it, Rect(Offset.Zero, Offset.Zero)) }
        }
    }
}

/** Flattened LazyColumn index of group [groupIdx]'s sticky header (each group = 1 header + N items), else -1. */
internal fun groupScrollIndex(groupSizes: List<Int>, groupIdx: Int): Int =
    if (groupIdx in groupSizes.indices) groupSizes.take(groupIdx).sumOf { it + 1 } else -1

/** Group for a scrubbed letter: exact match, else nearest above, else nearest below; -1 when the list is empty. */
internal fun targetGroupIndex(letters: List<Char>, scrub: Char): Int =
    letters.indexOf(scrub).takeIf { it >= 0 }
        ?: letters.indexOfFirst { it > scrub }.takeIf { it >= 0 }
        ?: letters.indexOfLast { it < scrub }.takeIf { it >= 0 }
        ?: -1

/** Normal phone-style A-Z list with sticky letter headers (Settings > Advanced > Normal scroll). */
@Composable
private fun NormalList(
    vm: LauncherViewModel,
    modifier: Modifier,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit
) {
    val c = LocalColors.current
    val groups = vm.groupedApps()
    val state = rememberLazyListState()
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(vm.scrubTick) {
        if (first) first = false
        else {
            val gi = targetGroupIndex(groups.map { it.first }, vm.letter)
            val li = groupScrollIndex(groups.map { it.second.size }, gi)
            if (li >= 0) state.animateScrollToItem(li)
        }
    }
    if (groups.isEmpty()) {
        Txt("No apps", 14f, c.fg2, modifier = modifier.padding(top = 18.dp))
        return
    }
    LazyColumn(state = state, modifier = modifier) {
        groups.forEach { (letter, apps) ->
            stickyHeader(key = "hdr_$letter") {
                Box(Modifier.fillMaxWidth().background(c.tile).padding(vertical = 5.dp)) {
                    Txt(letter.toString(), 22f, c.fg, FontWeight.Light)
                }
            }
            items(apps, key = { it.pkg }) { a ->
                AppEntry(a, vm, EntryMode.Panel, onLaunch, onMenu)
            }
        }
    }
}

@Composable
private fun SearchField(vm: LauncherViewModel, onGo: () -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier.fillMaxWidth().height(46.dp).glass(23.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(16.dp)) {
            val s = Stroke(1.6.dp.toPx())
            drawCircle(c.fg2, radius = size.width * 0.34f, center = Offset(size.width * 0.42f, size.height * 0.42f), style = s)
            drawLine(c.fg2, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.95f, size.height * 0.95f), strokeWidth = 1.6.dp.toPx())
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (vm.query.isEmpty()) Txt("Search apps", 15f, c.fg2)
            BasicTextField(
                value = vm.query,
                onValueChange = { vm.query = it; if (it.isNotEmpty()) vm.view = com.studentlauncher.data.ViewMode.All },
                singleLine = true,
                textStyle = TextStyle(color = c.fg, fontSize = 15.sp),
                cursorBrush = SolidColor(c.fg),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onGo() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dock
// ---------------------------------------------------------------------------------------------

@Composable
fun Dock(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val list = vm.dock.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
    Row(
        modifier.fillMaxWidth().height(68.dp).glass(34.dp).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        list.forEach { AppEntry(it, vm, EntryMode.Dock, onLaunch, onMenu) }
    }
}

@Composable
fun DockRail(
    vm: LauncherViewModel,
    onLaunch: (AppInfo, Rect) -> Unit,
    onMenu: (AppInfo, Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val list = vm.dock.mapNotNull { vm.appFor(it) }.filter { vm.isVisible(it) }
    if (list.isEmpty()) return
    Column(
        modifier.width(58.dp).glass(29.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(5.dp).background(if (i == 0) c.accent else c.fg2.copy(alpha = 0.5f), CircleShape))
            }
        }
        list.forEach { AppEntry(it, vm, EntryMode.Dock, onLaunch, onMenu) }
    }
}
