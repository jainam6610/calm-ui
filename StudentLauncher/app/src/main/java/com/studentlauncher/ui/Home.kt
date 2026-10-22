package com.studentlauncher.ui

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.AppInfo
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.SettingsPage
import com.studentlauncher.data.ThemeMode
import com.studentlauncher.data.ViewMode
import com.studentlauncher.data.InternalDestination
import kotlin.math.abs
import com.studentlauncher.mindful.ui.AppLimitsSheet
import com.studentlauncher.mindful.ui.MindfulDoor
import com.studentlauncher.mindful.ui.MindfulSettingsSheet
import com.studentlauncher.mindful.ui.MindfulTicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LauncherRoot(
    vm: LauncherViewModel,
    pickPhoto: () -> Unit,
    pickShufflePhotos: () -> Unit,
    addWidget: (AppWidgetProviderInfo) -> Unit,
    askNotifications: () -> Unit
) {
    val dark = when (vm.themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    val ctx = LocalContext.current

    SideEffect {
        val win = (view.context as? Activity)?.window
        if (win != null) {
            val ic = WindowCompat.getInsetsController(win, view)
            ic.isAppearanceLightStatusBars = !dark
            ic.isAppearanceLightNavigationBars = !dark
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(if (dark) Color.Black else Color(0xFFF2F2F4))) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        var set by remember { mutableStateOf<WallpaperSet?>(null) }

        LaunchedEffect(vm.wallpaper, vm.wallpaperVersion, dark, wPx, hPx) {
            val id = vm.wallpaper
            val adj = vm.currentWpAdjust()
            val slot = vm.shuffleSlot
            set = withContext(Dispatchers.Default) { Wallpapers.build(ctx, id, dark, wPx, hPx, adj, slot) }
        }
        val env = remember(set, dark) { GlassEnv(set?.blurred, dark, wPx) }
        val accent = if (vm.wpAccent && set != null) {
            remember(set, vm.wpAccent) {
                Wallpapers.dominantColor(set!!.blurred.asAndroidBitmap())
            }
        } else accentColor(vm.accentId)
        val colors = remember(dark, accent) { launcherColors(dark, accent) }

        CompositionLocalProvider(
            LocalColors provides colors,
            LocalGlass provides env,
            LocalMotionScale provides vm.effectiveMotion(),
            LocalFontFamily provides fontFamilyOf(vm.uiFont)
        ) {
            set?.let { s ->
                val motion = LocalMotionScale.current
                Crossfade(s, animationSpec = tween((700 * motion).coerceAtLeast(1f).toInt()), label = "wallpaper") { cur ->
                    Image(cur.full, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                }
            }
            when {
                !vm.onboardingComplete -> OnboardingFlow(vm)
                vm.destination == InternalDestination.Plan -> PlanApp(vm)
                vm.destination == InternalDestination.Settings -> SettingsApp(vm, pickPhoto, pickShufflePhotos, askNotifications)
                else -> HomeLayer(vm, pickPhoto, addWidget, askNotifications, maxWidth, maxHeight)
            }
            SharedSheets(vm, addWidget)
            MindfulTicker(vm.mindful)
        }
    }
}

// ponytail: shared overlays live here so Settings/Plan get PIN, wallpaper, widget sheets too
@Composable
private fun SharedSheets(vm: LauncherViewModel, addWidget: (AppWidgetProviderInfo) -> Unit) {
    if (vm.widgetCenter) WidgetCenter(vm, addWidget)
    val ew = vm.editWidgetId
    if (ew != null) key(ew) { WidgetSettings(vm, ew) }
    if (vm.aboutOpen) AboutSheet(vm)
    if (vm.wallpaperEditor) WallpaperEditor(vm)
    val pinReq = vm.pinRequest
    if (pinReq != null) key(pinReq) { PinSheet(vm, pinReq) }
    if (vm.mindful.settingsOpen) MindfulSettingsSheet(vm.mindful)
    if (vm.mindful.appLimitsOpen) AppLimitsSheet(vm)
    if (vm.pickHomeApps) HomeAppPicker(vm)
}

// ponytail: one sheet, reuses togglePin + BottomSheet — no new abstractions
@Composable
private fun HomeAppPicker(vm: LauncherViewModel) {
    val c = LocalColors.current
    BottomSheet({ vm.pickHomeApps = false }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt("Pick home apps", 17f, c.fg, FontWeight.Medium, modifier = Modifier.weight(1f))
            Txt("${vm.pinned.size}/${vm.homeAppLimit}", 12f, c.fg2)
            Spacer(Modifier.width(10.dp))
            PillButton("Done", { vm.pickHomeApps = false }, filled = true)
        }
        Spacer(Modifier.height(8.dp))
        Txt("Tap to pin or unpin. Home holds up to your limit.", 12f, c.fg2, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        vm.apps.forEach { app ->
            val on = app.pkg in vm.pinned
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp).tap {
                    if (on || vm.pinned.size < vm.homeAppLimit) vm.togglePin(app.pkg)
                    else vm.toast = "Home holds ${vm.homeAppLimit} apps"
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(app, vm, 36.dp)
                Spacer(Modifier.width(12.dp))
                Txt(app.label, 15f, c.fg, modifier = Modifier.weight(1f))
                if (on) Txt("on home", 11f, c.accent)
            }
        }
    }
}

private fun GraphicsLayerScope.depthBlur(p: Float) {
    if (Build.VERSION.SDK_INT >= 31 && p > 0.02f) renderEffect = BlurEffect(20f * p, 20f * p)
}

@Composable
fun HomeLayer(
    vm: LauncherViewModel,
    pickPhoto: () -> Unit,
    addWidget: (AppWidgetProviderInfo) -> Unit,
    askNotifications: () -> Unit,
    screenW: Dp,
    screenH: Dp
) {
    val c = LocalColors.current
    val dens = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var launching by remember { mutableStateOf<LaunchAnim?>(null) }
    var lastToast by remember { mutableStateOf("") }
    var homeMenu by remember { mutableStateOf(false) }

    // Phase 1: 0 = home, 1 = widget board. Swipe anywhere; dock stays put until Phase 2.
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    // 0 = home, 1 = all-apps panel. Read only in graphicsLayer blocks so animating it never
    // recomposes the screen (that is what keeps the transition smooth).
    val prog = remember { Animatable(0f) }
    LaunchedEffect(vm.view) {
        val target = if (vm.view == ViewMode.All) 1f else 0f
        if (vm.effectiveMotion() < 0.05f) prog.snapTo(target)
        else prog.animateTo(target, spring(dampingRatio = 0.88f, stiffness = 240f / vm.effectiveMotion().coerceAtLeast(.2f)))
    }
    val showHome by remember { derivedStateOf { prog.value < 0.99f } }
    val showPanel by remember { derivedStateOf { prog.value > 0.001f } }

    // Compact density is a user preference now, not only a small-screen fallback.
    val compact = isCompactHome(vm.compactDensity, screenH.value)
    val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topLimitPx = with(dens) { (topPad + 72.dp).toPx() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { launching = null }
    BackHandler {
        when {
            homeMenu -> homeMenu = false
            pagerState.currentPage == 1 -> scope.launch { pagerState.animateScrollToPage(0) }
            else -> vm.back()
        }
    }

    val launchApp: (AppInfo, Rect) -> Unit = { app, rect ->
        vm.ctxApp = null
        if (app.pkg in vm.flagged && !vm.isUnlocked(app.pkg)) {
            if (vm.mindfulEnabled) vm.mindful.beginDoor(app, rect)
            else if (vm.effectivePauseSec() > 0) {
                vm.pauseApp = app
                vm.pauseRect = rect
            } else {
                launching = LaunchAnim(app, rect)
            }
        } else {
            launching = LaunchAnim(app, rect)
        }
    }
    val openMenu: (AppInfo, Rect) -> Unit = { app, rect ->
        vm.ctxApp = app
        vm.ctxRect = rect
    }

    LaunchedEffect(launching) {
        val l = launching
        if (l != null) {
            delay(230)
            vm.startApp(l.app.pkg)
        }
    }
    LaunchedEffect(vm.toast) {
        val t = vm.toast
        if (t != null) {
            lastToast = t
            delay(1600)
            vm.toast = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.statusBarsPadding().height(72.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 0
            ) { page ->
                if (page == 0) {
                    HomePageContent(
                        vm, launchApp, openMenu, prog, showHome, showPanel, compact,
                        onLongPressEmpty = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeMenu = true
                        }
                    )
                } else {
                    WidgetBoard(vm, Modifier.fillMaxSize())
                }
            }

            AppsHandle(vm, prog)

            if (vm.dockStyle == DockStyle.Bottom && vm.dock.isNotEmpty()) {
                Dock(vm, launchApp, openMenu, Modifier.navigationBarsPadding().padding(horizontal = 28.dp, vertical = 10.dp))
            } else {
                Spacer(Modifier.navigationBarsPadding().height(12.dp))
            }
        }

        QuietHomeHeader(vm, pagerState, Modifier.align(Alignment.TopCenter).statusBarsPadding())

        val ca = vm.ctxApp
        if (ca != null) {
            Scrim { vm.ctxApp = null }
            key(ca.pkg) { ContextMenu(vm, ca, vm.ctxRect, screenW, topLimitPx) }
        }

        val door = vm.mindful.door
        if (door != null) {
            key(door.app.pkg) {
                MindfulDoor(
                    vm, door,
                    onProceed = { planned ->
                        vm.mindful.proceed(planned)
                        launching = LaunchAnim(door.app, door.rect)
                    },
                    onCancel = { vm.mindful.cancelDoor(true) }
                )
            }
        }
        if (vm.mindful.settingsOpen) MindfulSettingsSheet(vm.mindful)
        if (vm.mindful.appLimitsOpen) AppLimitsSheet(vm)

        val pa = vm.pauseApp
        if (pa != null) {
            key(pa.pkg) {
                PauseSheet(
                    vm, pa,
                    onCancel = { vm.pauseApp = null },
                    onOpen = { vm.pauseApp = null; launching = LaunchAnim(pa, vm.pauseRect) }
                )
            }
        }

        val bt = vm.activeBrainTask
        if (bt != null) {
            when (bt) {
                "flashcard" -> FlashcardSheet(onComplete = { vm.completeBrainTask() }, onDismiss = { vm.activeBrainTask = null })
                "math" -> MentalMathSheet(onComplete = { vm.completeBrainTask() }, onDismiss = { vm.activeBrainTask = null })
                "breathe" -> BreatheSheet(onComplete = { vm.completeBrainTask() }, onDismiss = { vm.activeBrainTask = null })
            }
        }

        AnimatedVisibility(
            vm.toast != null,
            Modifier.align(Alignment.TopCenter).padding(top = topPad + 84.dp),
            enter = slideInVertically(Motion.Offset) { -it } + fadeIn(),
            exit = slideOutVertically(Motion.Offset) { -it } + fadeOut()
        ) { ToastPill(lastToast) }

        if (homeMenu) {
            HomeMenuSheet(vm, onDismiss = { homeMenu = false })
        }

        LaunchOverlay(launching, c.dark)
    }
}

@Composable
private fun QuietHomeHeader(vm: LauncherViewModel, pagerState: PagerState, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val now = rememberNow()
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Txt(now.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")).uppercase(), 11f, c.fg2, FontWeight.Bold)
            Txt(now.format(java.time.format.DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a")), 25f, c.fg, FontWeight.SemiBold)
        }
        PagerDots(pagerState)
        Spacer(Modifier.width(10.dp))
        PillButton("Plan", { vm.destination = InternalDestination.Plan })
    }
}

/** Two quiet dots; the second one tells people the Widget Board exists. */
@Composable
private fun PagerDots(pagerState: PagerState) {
    val c = LocalColors.current
    val page = pagerState.currentPage
    Row(
        Modifier.semantics {
            contentDescription = "Page ${page + 1} of 2"
        },
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { i ->
            Box(
                Modifier
                    .size(if (i == page) 16.dp else 6.dp, 6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (i == page) c.accent else c.fg2.copy(alpha = 0.35f))
            )
        }
    }
}

/**
 * Swipe-up handle and visible All-Apps affordance. It lives above the dock, outside the widget
 * scroll, so it never competes with widget scrolling, the Home pager, or system gestures.
 */
@Composable
private fun AppsHandle(vm: LauncherViewModel, prog: Animatable<Float, *>) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val motion = LocalMotionScale.current
    val dens = LocalDensity.current
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = "Open all apps"
                role = Role.Button
                onClick("Open all apps") { vm.view = ViewMode.All; true }
            }
            .pointerInput(prog, motion) {
                val distance = with(dens) { 180.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                    var claimed = false
                    var lastDy = 0f
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        tracker.addPosition(ch.uptimeMillis, ch.position)
                        val dx = ch.position.x - down.position.x
                        val dy = ch.position.y - down.position.y
                        if (!claimed) {
                            if (abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) {
                                return@awaitEachGesture // horizontal: the Home pager owns it
                            }
                            if (drawerGestureOwns(dx, dy, viewConfiguration.touchSlop)) {
                                claimed = true
                                ch.consume()
                            } else if (dy > viewConfiguration.touchSlop) {
                                return@awaitEachGesture // downward: leave it to the content
                            }
                        }
                        if (claimed) {
                            lastDy = dy
                            if (motion >= 0.05f) {
                                val target = drawerProgress(dy, distance)
                                // The pointer scope is restricted; move the shared panel
                                // Animatable on the composition scope instead.
                                scope.launch { prog.snapTo(target) }
                            }
                            ch.consume()
                        }
                    }
                    if (claimed) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val velocity = tracker.calculateVelocity().y
                        if (drawerShouldOpen(drawerProgress(lastDy, distance), velocity)) {
                            vm.view = ViewMode.All
                        } else {
                            scope.launch {
                                if (motion < 0.05f) prog.snapTo(0f)
                                else prog.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 320f))
                            }
                        }
                    } else if (abs(lastDy) < viewConfiguration.touchSlop) {
                        // A plain tap on the affordance also opens the panel.
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.view = ViewMode.All
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                .background(c.track.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 5.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("Apps", 10f, c.fg2, FontWeight.Medium)
            Txt("\u2191", 11f, c.fg2, FontWeight.Medium)
        }
    }
}

@Composable
private fun HomeMenuSheet(vm: LauncherViewModel, onDismiss: () -> Unit) {
    val c = LocalColors.current
    BottomSheet(onDismiss, 0.56f) {
        Txt("HOME", 11f, c.fg2, FontWeight.Bold)
        Txt("Make it yours", 20f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(14.dp))
        PillButton("Settings", {
            onDismiss()
            vm.settingsPage = SettingsPage.Root
            vm.destination = InternalDestination.Settings
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PillButton("Widgets", {
            onDismiss()
            vm.widgetCenter = true
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PillButton("Wallpaper", {
            onDismiss()
            vm.settingsPage = SettingsPage.Wallpaper
            vm.destination = InternalDestination.Settings
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Txt("Long-press an app for app actions. Long-press a widget to select and resize it.", 11f, c.fg2, maxLines = 2)
    }
}

// Phase 1: page 0 of the Home <-> Board pager. Extracted verbatim so the
// Home <-> All panel transition (prog) behaves exactly as before.

/** Max height for the home app strip: 8 visible apps (2×4 grid or 8 list rows). */
internal fun homeAppsWindowMax(showNames: Boolean, isGrid: Boolean): Dp =
    if (isGrid) {
        // row = 4+56+(6+~14 label if names)+4 ≈ 84/64; +10dp between rows (AppViews.kt)
        val row = if (showNames) 84.dp else 64.dp
        row * 2 + 10.dp
    } else {
        50.dp * 8
    }

private fun homeAppsWindowMax(vm: LauncherViewModel): Dp =
    homeAppsWindowMax(vm.showNames, vm.layout == com.studentlauncher.data.HomeLayout.Grid)

@Composable
private fun HomePageContent(
    vm: LauncherViewModel,
    launchApp: (AppInfo, Rect) -> Unit,
    openMenu: (AppInfo, Rect) -> Unit,
    prog: Animatable<Float, *>,
    showHome: Boolean,
    showPanel: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onLongPressEmpty: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPressEmpty() }) }
    ) {
        val areaH = maxHeight
        val letterH = (areaH * 0.6f / 28f).coerceIn(10.dp, 17.dp)
        val side = vm.alphaSide
        val rail = vm.dockStyle == DockStyle.Rail
        val railLeft = rail && side != AlphaSide.Left
        val railRight = rail && side == AlphaSide.Left
        val startPad = 22.dp + (if (side == AlphaSide.Left) 38.dp else 0.dp) + (if (railLeft) 66.dp else 0.dp)
        val endPad = 22.dp + (if (side == AlphaSide.Right) 38.dp else 0.dp) + (if (railRight) 66.dp else 0.dp)
        val wStart = 16.dp + (if (side == AlphaSide.Left) 42.dp else 0.dp) + (if (railLeft) 66.dp else 0.dp)
        val wEnd = 16.dp + (if (side == AlphaSide.Right) 42.dp else 0.dp) + (if (railRight) 66.dp else 0.dp)

        if (showHome) {
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = prog.value
                        alpha = 1f - 0.9f * p
                        scaleX = 1f - 0.06f * p
                        scaleY = 1f - 0.06f * p
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        depthBlur(p)
                    }
            ) {
                Widgets(
                    vm, wStart, wEnd, compact, areaH * 0.48f,
                    Modifier.fillMaxWidth().weight(1f)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val p = prog.value
                            alpha = 1f - p
                            translationX = -48f * p * density
                            scaleX = 1f - 0.05f * p
                            scaleY = 1f - 0.05f * p
                            transformOrigin = TransformOrigin(0f, 1f)
                            depthBlur(p)
                        }
                ) {
                    HomeApps(
                        vm, launchApp, openMenu,
                        Modifier
                            .fillMaxWidth()
                            // Cap at 8 visible apps (2 grid rows / 8 list rows) so widgets/clock keep space;
                            // overflow scrolls inside this window.
                            .heightIn(max = homeAppsWindowMax(vm))
                            .verticalScroll(rememberScrollState())
                            .padding(start = startPad, end = endPad)
                    )
                    if (vm.arrangingApps) {
                        PillButton(
                            "Done",
                            { vm.arrangingApps = false },
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                            filled = true
                        )
                    }
                }
            }
        }

        if (showPanel) {
            AllAppsPanel(
                vm,
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(if (compact) 0.92f else 0.74f)
                    .graphicsLayer {
                        val p = prog.value
                        alpha = p
                        val s = 0.94f + 0.06f * p
                        scaleX = s
                        scaleY = s
                        translationX = (1f - p) * 60f * density
                        transformOrigin = TransformOrigin(1f, 1f)
                    },
                startPad, endPad, launchApp, openMenu
            )
        }

        if (rail) {
            DockRail(
                vm, launchApp, openMenu,
                Modifier
                    .align(if (railLeft) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }

        if (side != AlphaSide.Off) {
            AlphaScrubber(
                vm, letterH,
                Modifier
                    .align(if (side == AlphaSide.Right) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(bottom = 6.dp)
            )
        }
    }
}
