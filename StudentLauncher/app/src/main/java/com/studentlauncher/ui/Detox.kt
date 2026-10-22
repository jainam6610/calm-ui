package com.studentlauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.detox.DetoxPage
import com.studentlauncher.detox.DetoxPolicy
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * Detoxify as a full Calm port: the start flow is Calm's multi-step onboarding (typed title,
 * dim body, pill choices, lowercase app rows), a running session is Calm's home (big countdown,
 * lowercase rows, phone/camera pills), and "settings" is a Calm-style full page rather than a
 * section buried in the launcher's long settings list.
 *
 * Palette note: Calm ships three palettes. Here the mode follows the launcher's own dark/paper
 * choice but uses Calm's paper tone and type scale, so the two apps feel like one.
 */

private class CalmColors(val base: Color, val ink: Color, val dim: Color, val line: Color)

@Composable
private fun calmColors(): CalmColors {
    val c = LocalColors.current
    return CalmColors(
        base = if (c.dark) Color.Black else Color(0xFFF2F0E9),
        ink = c.fg,
        dim = c.fg2,
        line = c.track
    )
}

// ---------- Calm primitives ----------

@Composable
private fun CalmPage(
    title: String = "",
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val k = calmColors()
    Column(
        Modifier
            .fillMaxSize()
            .background(k.base)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, top = 30.dp, bottom = 28.dp)
    ) {
        if (onBack != null) {
            Txt("←  $title", 15f, k.dim, modifier = Modifier.tap(onBack).padding(bottom = 12.dp))
        }
        content()
    }
}

/** Calm's row: light 25 sp label, a dim right-hand value, press dims and shrinks. */
@Composable
private fun CalmRow(
    label: String,
    trailing: String = "",
    size: Float = 25f,
    onClick: (() -> Unit)? = null
) {
    val k = calmColors()
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.45f else 1f, tween(90), label = "calmA")
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, tween(90), label = "calmS")
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = a; scaleX = sc; scaleY = sc; transformOrigin = TransformOrigin(0f, 0.5f) }
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = src, indication = null, role = Role.Button
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                } else Modifier
            )
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(label, size, k.ink, FontWeight.Light, modifier = Modifier.weight(1f))
        if (trailing.isNotEmpty()) Txt(trailing, 13f, k.dim)
    }
}

@Composable
private fun CalmSettingRow(label: String, value: String, onClick: () -> Unit) {
    CalmRow(label, value, 20f, onClick)
}

@Composable
private fun CalmInfoRow(label: String, value: String) {
    val k = calmColors()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, 17f, k.ink, FontWeight.Light, modifier = Modifier.weight(1f))
        Txt(value, 14f, k.dim)
    }
}

@Composable
private fun CalmButton(
    label: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val k = calmColors()
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val a by animateFloatAsState(if (pressed) 0.45f else 1f, tween(90), label = "calmBtn")
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(40.dp))
            .then(if (filled) Modifier.background(k.ink) else Modifier.border(1.dp, k.ink, RoundedCornerShape(40.dp)))
            .graphicsLayer { alpha = a }
            .clickable(interactionSource = src, indication = null, role = Role.Button) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) { Txt(label, 16f, if (filled) k.base else k.ink) }
}

/** Calm's onboarding pills: filled when selected, hairline otherwise. Wraps two per row. */
@Composable
private fun <T> CalmPillRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val k = calmColors()
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { (value, label) ->
                    val on = value == selected
                    val src = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .clickable(interactionSource = src, indication = null, role = Role.RadioButton) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(value)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .then(
                                    if (on) Modifier.background(k.ink)
                                    else Modifier.border(1.dp, k.ink, RoundedCornerShape(40.dp))
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Txt(label, 14f, if (on) k.base else k.ink)
                        }
                    }
                }
            }
        }
    }
}

/** Calm types the onboarding title a character at a time; skipped when motion is off. */
@Composable
private fun CalmTitle(text: String, animate: Boolean) {
    val k = calmColors()
    var shown by remember(text) { mutableStateOf(if (animate) "" else text) }
    LaunchedEffect(text) {
        if (!animate) { shown = text; return@LaunchedEffect }
        shown = ""
        for (i in 1..text.length) {
            delay(38L)
            shown = text.substring(0, i)
        }
    }
    Txt(shown, 32f, k.ink, FontWeight.Light)
}

@Composable
private fun CalmBody(text: String, animate: Boolean, delayMs: Long = 0L) {
    val k = calmColors()
    val a = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(text) {
        if (!animate) { a.snapTo(1f); return@LaunchedEffect }
        a.snapTo(0f)
        delay(delayMs)
        a.animateTo(1f, tween(400))
    }
    Txt(
        text, 15f, k.dim, maxLines = 4,
        modifier = Modifier.padding(top = 10.dp).graphicsLayer { alpha = a.value }
    )
}

// ---------- start flow (Calm onboarding) ----------

@Composable
fun DetoxSetup(vm: LauncherViewModel) {
    val k = calmColors()
    val animate = vm.motionIntensity > 0f
    val step = vm.detoxSetupStep
    val last = DetoxPolicy.SETUP_STEPS - 1
    BackHandler { if (step > 0) vm.detoxStep(step - 1) else vm.closeDetoxSetup() }
    CalmPage {
        Txt(
            "←  detoxify", 15f, k.dim,
            modifier = Modifier.tap { vm.closeDetoxSetup() }.padding(bottom = 16.dp)
        )
        Txt("${step + 1} / ${DetoxPolicy.SETUP_STEPS}", 12f, k.dim)
        Spacer(Modifier.height(18.dp))
        val title = when (step) {
            0 -> "detoxify"
            1 -> "your apps"
            2 -> "the way out"
            else -> "you're set"
        }
        CalmTitle(title, animate)
        val body = when (step) {
            0 -> "a quieter phone for a while. first, how long?"
            1 -> "tap an app to allow it. phone, camera and clock are always there."
            2 -> "a pin, then a ten minute wait before the session ends early."
            else -> "everything else disappears from the home screen until the timer runs out."
        }
        CalmBody(body, animate, if (animate) 38L * title.length + 100L else 0L)
        when (step) {
            0 -> {
                Spacer(Modifier.height(26.dp))
                Txt("length", 12f, k.dim)
                Spacer(Modifier.height(8.dp))
                CalmPillRow(DetoxPolicy.durations, vm.detoxDraftLen, vm::detoxDraftDuration)
                Spacer(Modifier.height(18.dp))
                Txt("counts down on the home screen. one month is thirty days.", 12f, k.dim, maxLines = 2)
                Spacer(Modifier.weight(1f))
            }
            1 -> {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Txt("always", 12f, k.dim, modifier = Modifier.padding(top = 6.dp))
                    listOf("phone", "camera", "clock").forEach { name ->
                        CalmRow(name, "always", 20f)
                    }
                    Spacer(Modifier.height(16.dp))
                    Txt("allow", 12f, k.dim)
                    vm.apps.sortedBy { it.label.lowercase() }
                        .filter { it.pkg !in vm.detoxAlways }
                        .forEach { app ->
                            CalmRow(
                                app.label.lowercase(),
                                if (app.pkg in vm.detoxDraftAllowed) "on" else "",
                                20f
                            ) { vm.detoxDraftToggle(app.pkg) }
                        }
                    Spacer(Modifier.height(8.dp))
                }
            }
            2 -> {
                Spacer(Modifier.height(18.dp))
                CalmRow("pin", if (vm.pinSet) "set" else "set one", 20f) {
                    if (!vm.pinSet) vm.requestCreatePin()
                }
                Spacer(Modifier.height(12.dp))
                Txt(
                    "you will need it to end the session early. forgetting it can be reset from settings after twenty-four hours.",
                    12f, k.dim, maxLines = 4
                )
                Spacer(Modifier.weight(1f))
            }
            else -> {
                Spacer(Modifier.height(18.dp))
                CalmInfoRow("length", durationLabel(vm.detoxDraftLen))
                CalmInfoRow("apps", "${vm.detoxDraftAllowed.size} allowed")
                CalmInfoRow("always", "phone · camera · clock")
                Spacer(Modifier.height(14.dp))
                Txt(
                    "no apps are allowed unless you tick them. the timer, your pin and the ten minute wait are the only way out.",
                    12f, k.dim, maxLines = 4
                )
                Spacer(Modifier.weight(1f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(
                if (step > 0) "back" else "cancel", 15f, k.dim,
                modifier = Modifier
                    .tap { if (step > 0) vm.detoxStep(step - 1) else vm.closeDetoxSetup() }
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            )
            Spacer(Modifier.weight(1f))
            CalmButton(if (step == last) "start" else "next", filled = true) {
                if (step == last) vm.detoxBegin() else vm.detoxStep(step + 1)
            }
        }
    }
}

// ---------- a running session ----------

@Composable
fun DetoxMode(vm: LauncherViewModel) {
    // Back always stays inside the session: from a page it returns home, on home it does nothing.
    BackHandler(enabled = true) {
        if (vm.detoxPage != DetoxPage.Home) vm.detoxSetPage(DetoxPage.Home)
    }
    AnimatedContent(
        targetState = vm.detoxPage,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState == DetoxPage.Home) {
                (slideInHorizontally(tween(300)) { -it / 6 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(240)))
            } else {
                (slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(240)) { -it / 6 } + fadeOut(tween(240)))
            }
        },
        label = "detoxPage"
    ) { page ->
        when (page) {
            DetoxPage.Home -> DetoxHome(vm)
            DetoxPage.Settings -> DetoxSettings(vm)
            DetoxPage.Apps -> DetoxApps(vm)
        }
    }
}

@Composable
private fun DetoxHome(vm: LauncherViewModel) {
    val k = calmColors()
    val session = vm.detox ?: return
    val now = rememberNow()
    CalmPage {
        Txt(DetoxPolicy.label(vm.detoxLeft), 64f, k.ink, FontWeight.Light, modifier = Modifier.padding(top = 10.dp))
        Txt(
            now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).lowercase(),
            15f, k.dim, modifier = Modifier.padding(top = 6.dp)
        )
        if (vm.detoxReleaseAt != 0L) {
            val left = DetoxPolicy.releaseLeftMs(vm.detoxReleaseAt, System.currentTimeMillis())
            Txt("ends early in ${DetoxPolicy.label(left)}", 13f, k.dim, modifier = Modifier.padding(top = 10.dp))
        }
        val rows = userApps(vm, session)
        Column(Modifier.weight(1f).padding(top = 28.dp).verticalScroll(rememberScrollState())) {
            if (rows.isEmpty()) {
                Txt("phone, camera and clock. that's all.", 15f, k.dim, maxLines = 2)
            }
            rows.forEach { a -> CalmRow(a.label.lowercase()) { vm.startApp(a.pkg) } }
            CalmRow("clock") { vm.openSystemIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)) }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt(
                "plan", 13f, k.dim,
                modifier = Modifier.tap { vm.destination = InternalDestination.Plan }.padding(vertical = 8.dp)
            )
            Txt("·", 13f, k.dim, modifier = Modifier.padding(horizontal = 8.dp))
            Txt(
                "settings", 13f, k.dim,
                modifier = Modifier.tap { vm.detoxSetPage(DetoxPage.Settings) }.padding(vertical = 8.dp)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CalmButton("phone", filled = false) { vm.openSystemIntent(Intent(Intent.ACTION_DIAL)) }
            Spacer(Modifier.weight(1f))
            CalmButton("camera", filled = false) {
                vm.openSystemIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
        }
    }
}

@Composable
private fun DetoxSettings(vm: LauncherViewModel) {
    val k = calmColors()
    val session = vm.detox ?: return
    CalmPage(title = "settings", onBack = { vm.detoxSetPage(DetoxPage.Home) }) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(10.dp))
            CalmInfoRow("time left", DetoxPolicy.label(vm.detoxLeft))
            CalmInfoRow("length", durationLabel(session.lengthMs))
            Spacer(Modifier.height(10.dp))
            CalmSettingRow("apps", "${userApps(vm, session).size} allowed") {
                vm.detoxSetPage(DetoxPage.Apps)
            }
            if (vm.detoxReleaseAt != 0L) {
                val left = DetoxPolicy.releaseLeftMs(vm.detoxReleaseAt, System.currentTimeMillis())
                CalmSettingRow("ending early", "in ${DetoxPolicy.label(left)}") { vm.cancelDetoxRelease() }
                Txt(
                    "tap that row to withdraw and stay in the session.",
                    12f, k.dim, maxLines = 2, modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                CalmSettingRow("end early", "pin + 10 min") { vm.requestDetoxEnd() }
            }
            CalmSettingRow("launcher settings", "") { vm.destination = InternalDestination.Settings }
            Spacer(Modifier.height(20.dp))
        }
        Txt("the timer is the way out. everything else is a wait.", 12f, k.dim, maxLines = 2)
    }
}

@Composable
private fun DetoxApps(vm: LauncherViewModel) {
    val k = calmColors()
    val session = vm.detox ?: return
    CalmPage(title = "apps", onBack = { vm.detoxSetPage(DetoxPage.Settings) }) {
        Spacer(Modifier.height(14.dp))
        Txt("always", 12f, k.dim)
        Spacer(Modifier.height(6.dp))
        listOf("phone", "camera", "clock").forEach { name ->
            Txt(name, 20f, k.ink, FontWeight.Light, modifier = Modifier.padding(vertical = 9.dp))
        }
        Spacer(Modifier.height(18.dp))
        Txt("allowed", 12f, k.dim)
        Spacer(Modifier.height(6.dp))
        val rows = userApps(vm, session)
        if (rows.isEmpty()) {
            Txt("none — just the three above.", 15f, k.dim, modifier = Modifier.padding(top = 6.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            rows.forEach { a ->
                Txt(a.label.lowercase(), 20f, k.ink, FontWeight.Light, modifier = Modifier.padding(vertical = 9.dp))
            }
        }
        Txt(
            "change these when the session ends.", 12f, k.dim, maxLines = 2,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// ---------- sheets that can appear over any screen ----------

@Composable
internal fun DetoxBlockSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val pkg = vm.detoxBlocked ?: return
    BottomSheet({ vm.clearDetoxBlock() }) {
        Txt("not now", 26f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(6.dp))
        Txt("${vm.appFor(pkg)?.label ?: "this app"} is off during detoxify.", 13f, c.fg2, maxLines = 2)
        Txt(
            "${DetoxPolicy.label(vm.detoxLeft)} left", 13f, c.accent,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(10.dp))
        PillButton("ok", { vm.clearDetoxBlock() }, filled = true)
    }
}

@Composable
internal fun DetoxSummarySheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val summary = vm.detoxSummary ?: return
    BottomSheet({ vm.dismissDetoxSummary() }) {
        Txt(
            if (summary.endedByTimer) "session complete" else "session ended",
            26f, c.fg, FontWeight.Light
        )
        Spacer(Modifier.height(6.dp))
        Txt("${durationLabel(summary.lengthMs)} · ${summary.allowedCount} apps allowed", 13f, c.fg2, maxLines = 2)
        Spacer(Modifier.height(4.dp))
        Txt("your phone is back to normal.", 13f, c.fg2, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        PillButton("ok", { vm.dismissDetoxSummary() }, filled = true)
    }
}

// ---------- helpers ----------

private fun durationLabel(ms: Long): String =
    DetoxPolicy.durations.firstOrNull { it.first == ms }?.second ?: DetoxPolicy.label(ms)

/** The apps the user ticked, minus the always-allowed set (those live in the bottom pills). */
private fun userApps(vm: LauncherViewModel, session: DetoxPolicy.Session) =
    session.allowed
        .filter { it !in vm.detoxAlways }
        .mapNotNull { vm.appFor(it) }
        .sortedBy { it.label.lowercase() }
