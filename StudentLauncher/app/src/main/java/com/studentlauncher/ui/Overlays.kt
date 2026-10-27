package com.studentlauncher.ui

import android.content.Intent
import androidx.core.net.toUri
import com.studentlauncher.data.WT
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AppInfo
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class LaunchAnim(val app: AppInfo, val rect: Rect)

/** Transparent tap-catcher (menus, context menu). */
@Composable
fun Scrim(onTap: () -> Unit) {
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onTap() } })
}

/** Dimming tap-catcher whose alpha follows an enter animation without recomposing. */
@Composable
fun FadeScrim(onTap: () -> Unit, progress: Float, max: Float = 0.22f) {
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = progress.coerceIn(0f, 1f) * max }
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onTap() } }
    )
}

/** Bottom sheet used by widgets, about and pause sheets. Springs up, dims the home screen. */
@Composable
fun BottomSheet(
    onDismiss: () -> Unit,
    maxHeightFrac: Float = 0.82f,
    dim: Float = 0.22f,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val visible = remember { MutableTransitionState(true) }
    LaunchedEffect(visible.targetState) { if (!visible.targetState) onDismiss() }
    val e by animateFloatAsState(if (visible.targetState) 1f else 0f, Motion.Macro, label = "bs")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxH = maxHeight * maxHeightFrac
        FadeScrim({ visible.targetState = false }, e, dim)
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(14.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .graphicsLayer { translationY = (1f - e) * size.height * 1.15f }
                    .glass(32.dp, GlassLevel.Chrome)
                    .padding(20.dp)
            ) {
                // Pinned above the scroll area: the live widget preview rides the sheet top.
                header?.invoke(this)
                Column(Modifier.verticalScroll(rememberScrollState()), content = content)
            }
        }
    }
}

@Composable
fun ContextMenu(vm: LauncherViewModel, app: AppInfo, anchor: Rect, screenW: Dp, topLimit: Float) {
    val visible = remember { MutableTransitionState(true) }
    LaunchedEffect(visible.targetState) { if (!visible.targetState) vm.ctxApp = null }
    val e by animateFloatAsState(if (visible.targetState) 1f else 0f, Motion.Macro, label = "ctx")
    val density = LocalDensity.current
    var h by remember { mutableIntStateOf(0) }
    val wPx = with(density) { 216.dp.toPx() }
    val margin = with(density) { 12.dp.toPx() }
    val gap = with(density) { 8.dp.toPx() }
    val x = anchor.left.coerceIn(margin, with(density) { screenW.toPx() } - wPx - margin)
    val above = anchor.top - h - gap
    val y = if (above >= topLimit) above else anchor.bottom + gap
    val originY = if (above >= topLimit) 1f else 0f

    Column(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(216.dp)
            .graphicsLayer {
                val s = 0.82f + 0.18f * e
                scaleX = s; scaleY = s
                alpha = e.coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0.2f, originY)
            }
            .onSizeChanged { h = it.height }
            .glass(22.dp, GlassLevel.Chrome)
            .padding(6.dp)
    ) {
        val pinned = app.pkg in vm.pinned
        val docked = app.pkg in vm.dock
        val context = LocalContext.current
        CtxItem(if (pinned) "Remove from home" else "Pin to home") { visible.targetState = false; vm.togglePin(app.pkg) }
        if (pinned || docked) CtxItem(if (pinned) "Rearrange apps" else "Rearrange dock") {
            visible.targetState = false
            vm.selectedWidgetId = null
            vm.arrangingApps = true
            vm.toast = if (pinned) "Hold an app, then drag to rearrange" else "Hold a dock app, then drag to reorder"
        }
        CtxItem(if (docked) "Remove from dock" else "Add to dock") { visible.targetState = false; vm.toggleDock(app.pkg) }
        CtxItem(if (app.pkg in vm.flagged) "Not distracting" else "Mark as distracting") { visible.targetState = false; vm.toggleFlag(app.pkg) }
        CtxItem(if (app.pkg in vm.studyApps) "Not a study app" else "Mark as study app") { visible.targetState = false; vm.toggleStudyApp(app.pkg) }
        CtxItem("App info") {
            visible.targetState = false
            if (!openSystemPage(context, app.pkg, android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)) {
                vm.toast = "Couldn't open app info"
            }
        }
        CtxItem("Uninstall") {
            visible.targetState = false
            if (!openSystemPage(context, app.pkg, Intent.ACTION_DELETE)) {
                vm.toast = "Couldn't start uninstall"
            }
        }
    }
}

/**
 * Sends a `package:` intent to the system, used for App info and Uninstall. The system owns both
 * screens. Uninstall additionally needs REQUEST_DELETE_PACKAGES (a normal permission declared in
 * the manifest); without it the system uninstaller closes itself as soon as it opens.
 * Returns false when the platform has nothing to handle the intent.
 */
private fun openSystemPage(context: android.content.Context, pkg: String, action: String): Boolean {
    val intent = Intent(action, "package:$pkg".toUri())
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(intent) }.isSuccess
}

@Composable
private fun CtxItem(label: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).tap {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) { Txt(label, 14f) }
}

@Composable
fun PauseSheet(vm: LauncherViewModel, app: AppInfo, onCancel: () -> Unit, onOpen: () -> Unit) {
    val c = LocalColors.current
    val delay = vm.effectivePauseSec()
    var left by remember { mutableIntStateOf(delay) }
    var chosen by remember { mutableIntStateOf(0) } // 0=none, 1=quick, 5=five, 15=fifteen
    LaunchedEffect(Unit) {
        vm.incrementPauseCount()
        while (left > 0) { delay(1000); left-- }
    }
    BottomSheet(onCancel, 0.6f) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.dp, c.fg2.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Txt("$left", 18f, c.fg, FontWeight.Light)
            }
            Spacer(Modifier.height(14.dp))
            Txt("Opening ${app.label}", 17f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Txt("Is this what you want to do right now?", 13f, c.fg2)

            if (vm.mindfulEnabled) {
                Spacer(Modifier.height(14.dp))
                Txt("What for?", 12f, c.fg2)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Quick check", 5 to "5 min", 15 to "15 min").forEach { (mins, label) ->
                        val sel = chosen == mins
                        Box(
                            Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (sel) c.fg else c.fg.copy(alpha = 0.1f))
                                .tap { chosen = mins; vm.unlockApp(app.pkg, mins); onOpen() }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) { Txt(label, 13f, if (sel) c.knobOn else c.fg) }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Not now", onCancel)
                val ready = left == 0
                PillButton(if (ready) "Open" else "Open in $left", {
                    if (ready) { vm.incrementPauseCount(); onOpen() }
                }, filled = ready)
            }

            if (vm.mindfulEnabled && vm.earnScrollEnabled) {
                Spacer(Modifier.height(12.dp))
                Txt("Or do something quick first:", 11f, c.fg2)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Study timer" to "timer", "Flashcard" to "flashcard", "Breathe" to "breathe").forEach { (label, task) ->
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(c.fg.copy(alpha = 0.08f))
                                .tap {
                                    if (task == "timer") {
                                        vm.pauseApp = null
                                        // Start the timer widget
                                        val timer = vm.homeWidgets.firstOrNull { it.type == WT.TIMER }
                                            ?: vm.boardWidgets.firstOrNull { it.type == WT.TIMER }
                                        if (timer != null) vm.setCfg(timer.id, "start", System.currentTimeMillis().toString())
                                        else vm.toast = "Add a Study timer widget first"
                                    } else {
                                        vm.activeBrainTask = task
                                        vm.pauseApp = null
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) { Txt(label, 11f, c.fg2) }
                    }
                }
            }
        }
    }
}

@Composable
fun ToastPill(text: String) {
    val c = LocalColors.current
    Box(
        Modifier.glass(20.dp, GlassLevel.Chrome).semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) { Txt(text, 13f, c.fg) }
}

/** The tapped icon grows into a full-screen sheet (iPhone-style zoom), then collapses on return. */
@Composable
fun LaunchOverlay(anim: LaunchAnim?, dark: Boolean) {
    val p = animateFloatAsState(if (anim != null) 1f else 0f, Motion.Macro, label = "launch")
    var last by remember { mutableStateOf<LaunchAnim?>(null) }
    LaunchedEffect(anim) { if (anim != null) last = anim }
    val cur = anim ?: last
    if (cur != null) {
        val tint = if (dark) Color(0xFF0B0B0C) else Color(0xFFF5F5F7)
        Canvas(Modifier.fillMaxSize()) {
            val f = p.value
            if (f > 0.002f) {
                val r = cur.rect
                val l = lerp(r.left, 0f, f)
                val t = lerp(r.top, 0f, f)
                val rr = lerp(r.right, size.width, f)
                val b = lerp(r.bottom, size.height, f)
                val cr = lerp(r.width * 0.3f, 44.dp.toPx(), f)
                drawRoundRect(
                    color = tint.copy(alpha = 0.55f + 0.45f * f),
                    topLeft = Offset(l, t),
                    size = Size(rr - l, b - t),
                    cornerRadius = CornerRadius(cr)
                )
            }
        }
    }
}
