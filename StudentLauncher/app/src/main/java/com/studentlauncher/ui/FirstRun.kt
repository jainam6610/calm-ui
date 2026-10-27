package com.studentlauncher.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.data.SettingsPage

@Composable
fun SettingsApp(
    vm: LauncherViewModel,
    pickPhoto: () -> Unit,
    pickShufflePhotos: () -> Unit,
    requestNotifications: () -> Unit
) {
    val c = LocalColors.current
    androidx.activity.compose.BackHandler { vm.back() }
    Box(Modifier.fillMaxSize().background(if (c.dark) Ink.Canvas else Color(0xFFF7F7F9))) {
        when (vm.settingsPage) {
            SettingsPage.Root -> SettingsRoot(vm)
            SettingsPage.Personalization -> PersonalizationHub(vm)
            SettingsPage.Icons -> IconsPage(vm)
            SettingsPage.Theme -> ThemePage(vm)
            SettingsPage.Wallpaper -> WallpaperPage(vm, pickPhoto, pickShufflePhotos)
            SettingsPage.Home -> HomeSettingsPage(vm)
            SettingsPage.Clock -> ClockPage(vm)
            SettingsPage.Glyph -> GlyphStudioPage(vm)
            SettingsPage.Calm -> CalmSettingsPage(vm)
            SettingsPage.Focus -> FocusPage(vm, requestNotifications)
            SettingsPage.Signals -> SignalsPage(vm, requestNotifications)
            SettingsPage.Advanced -> AdvancedPage(vm)
            SettingsPage.Privacy -> PrivacyPage(vm)
        }
    }
}

@Composable
private fun SettingsRoot(vm: LauncherViewModel) {
    LaunchedEffect(Unit) { vm.reloadCalmConfig() }
    val ctx = LocalContext.current
    val version = remember(ctx) { appVersionLabel(ctx) }
    val layout = when (vm.layout) {
        HomeLayout.Grid -> "Grid · ${vm.homeColumns} columns"
        HomeLayout.List -> "List"
    }
    val dock = when (vm.dockStyle) {
        DockStyle.Bottom -> "Bottom"
        DockStyle.Rail -> "Rail"
        DockStyle.Off -> "Off"
    }
    SettingsScaffold("Settings", onBack = { vm.destination = InternalDestination.Home }) {
        SettingsGroup {
            SettingsLink(
                "Personalisation",
                "Icons, theme, wallpaper, clock",
                "personal"
            ) { vm.settingsPage = SettingsPage.Personalization }
        }
        SettingsGroup("Home screen") {
            SettingsLink("Layout", layout, "layout") { vm.settingsPage = SettingsPage.Home }
            SettingsLink("Home apps", "${vm.pinned.size} of ${vm.homeAppLimit}", "apps") { vm.pickHomeApps = true }
            SettingsLink("Dock", dock, "dock") { vm.settingsPage = SettingsPage.Home }
            SettingsLink("Widgets", "Add or arrange", "widgets") { vm.widgetCenter = true }
        }
        SettingsGroup("Focus") {
            SettingsToggle("Study mode", "Hide apps marked distracting", "focus", vm.studyMode, vm::applyStudyMode)
            SettingsToggle("Mindful Mode", "Pauses and session tracking", "shield", vm.mindfulEnabled, vm::applyMindful)
            SettingsLink("Pauses & reminders", "${vm.pauseSec}s pause · every ${vm.reminderMin} min", "bell") {
                vm.settingsPage = SettingsPage.Focus
            }
            SettingsLink("App limits", "Daily time and per-app limits", "detox") { vm.mindful.appLimitsOpen = true }
            SettingsLink("PIN", if (vm.pinSet) "Set" else "Not set", "privacy") { vm.settingsPage = SettingsPage.Focus }
        }
        SettingsGroup("Detoxify") {
            SettingsLink("Detox apps", "${vm.calmCount} on Detox now", "detox") { vm.openCalmConfig() }
        }
        SettingsGroup("Signals & reactions") {
            SettingsLink("Reactions", if (vm.torchEnabled) "Torch on" else "Off", "bolt") {
                vm.settingsPage = SettingsPage.Signals
            }
            SettingsLink("Notification signals", if (vm.notifSignalsEnabled) "On" else "Off", "bell") {
                vm.settingsPage = SettingsPage.Signals
            }
        }
        SettingsGroup("Privacy & about") {
            SettingsLink("Privacy policy", "Opens in your browser", "privacy") { vm.settingsPage = SettingsPage.Privacy }
            SettingsLink("About", "Calm UI $version", "info") { vm.aboutOpen = true }
        }
        SettingsGroup("Advanced") {
            SettingsLink("Appearance & motion", "Font, density, scroll, motion", "advanced") {
                vm.settingsPage = SettingsPage.Advanced
            }
            SettingsLink("Restart onboarding", "Walk through the launcher again", "restart") { vm.resetOnboarding() }
        }
    }
}

@Composable
private fun FocusPage(vm: LauncherViewModel, requestNotifications: () -> Unit) {
    val c = LocalColors.current
    SettingsScaffold("Focus", onBack = { vm.settingsPage = SettingsPage.Root }) {
        SettingsGroup("Focus") {
            SettingsToggle("Study mode", "Hide apps marked distracting", "focus", vm.studyMode, vm::applyStudyMode)
            SettingsToggle("Mindful Mode", "Escalating pauses + session tracking", "shield", vm.mindfulEnabled, vm::applyMindful)
        }
        if (vm.mindfulEnabled) {
            SettingsGroup("Mindful") {
                SettingsToggle("Escalating pause", "Delay grows each time you open a distracting app", "focus", vm.escalatingEnabled, vm::applyEscalating)
                SettingsToggle("Session tracking", "Track time in distracting apps", "detox", vm.trackingEnabled, vm::applyTracking)
                SettingsToggle("Earn your scroll", "Do a quick task to unlock for 10 min", "bolt", vm.earnScrollEnabled, vm::applyEarnScroll)
                Spacer(Modifier.height(12.dp))
                Txt("Daily budget: ${vm.dailyBudgetMin} min", 12f, c.fg2)
                Spacer(Modifier.height(6.dp))
                Segmented(
                    listOf(30 to "30 min", 60 to "60 min", 90 to "90 min", 120 to "2h"),
                    vm.dailyBudgetMin,
                    vm::applyDailyBudget
                )
                Spacer(Modifier.height(10.dp))
                SettingsAction("Mindful Mode settings") { vm.mindful.settingsOpen = true }
                Spacer(Modifier.height(8.dp))
                SettingsAction("App limits") { vm.mindful.appLimitsOpen = true }
            }
        }
        SettingsGroup("Pauses & reminders") {
            Txt("Pause before opening a distracting app", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf(0 to "No pause", 5 to "5 sec", 10 to "10 sec"),
                vm.pauseSec,
                vm::applyPauseSec
            )
            Spacer(Modifier.height(12.dp))
            Txt("Reminder interval", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf(5 to "5 min", 10 to "10 min", 15 to "15 min"),
                vm.reminderMin,
                { v -> vm.applyReminderMin(v); if (v > 0) requestNotifications() }
            )
            Spacer(Modifier.height(10.dp))
            SettingsAction("Allow reminders") { requestNotifications() }
        }
        SettingsGroup("PIN") {
            if (!vm.pinSet) {
                SettingsAction("Set a PIN") { vm.requestCreatePin() }
            } else {
                SettingsToggle("Protect Study mode with PIN", null, "privacy", vm.protectStudy, vm::applyProtectStudy)
                Spacer(Modifier.height(8.dp))
                SettingsAction("Change PIN") { vm.requestChangePin() }
                Spacer(Modifier.height(8.dp))
                SettingsAction("Remove PIN") { vm.requestRemovePin() }
            }
        }
    }
}

@Composable
private fun SignalsPage(vm: LauncherViewModel, requestNotifications: () -> Unit) {
    val c = LocalColors.current
    SettingsScaffold("Signals & reactions", onBack = { vm.settingsPage = SettingsPage.Root }) {
        SettingsGroup("Torch reactions") {
            Txt(
                "Flash the camera light when a focus session ends. Off by default. Never used for touch, breathing or low battery. Capped at 3 flashes per second, 3 seconds total, 12 per hour, with a 10 second cool-down; skipped in battery saver, Do Not Disturb, when warm, when battery is under 15% and charging is off, or with reduced motion.",
                12f, c.fg2, maxLines = 8
            )
            Spacer(Modifier.height(10.dp))
            SettingsToggle("Torch reactions", "Camera light flashes for finished sessions", "bolt", vm.torchEnabled, vm::applyTorchEnabled)
            if (!vm.torch.hasFlash) {
                Spacer(Modifier.height(6.dp))
                Txt("This device reports no flash unit, so torch reactions stay off.", 12f, c.fg2, maxLines = 2)
            }
            if (vm.torchEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { SettingsAction("Test torch") { vm.testTorch() } }
                    Box(Modifier.weight(1f)) { SettingsAction("Preview Done") { vm.reactionDone() } }
                }
                vm.torchNotice?.let {
                    Spacer(Modifier.height(6.dp))
                    Txt(it, 12f, c.fg2, maxLines = 2)
                }
            }
        }
        SettingsGroup("Signals") {
            Txt(
                "The glyph matrix follows your phone. It shows the clock, switches to the charging ring while plugged in, and reacts when a signal arrives.",
                12f, c.fg2, maxLines = 4
            )
            Spacer(Modifier.height(10.dp))
            if (vm.hasGlyphWidget()) {
                Txt(
                    "A glyph matrix is on your home screen or the widget board, so reactions and the live state have somewhere to show.",
                    12f, c.fg2, maxLines = 2
                )
            } else {
                Txt(
                    "No glyph matrix on your home screen or the widget board, so reactions have nowhere to show.",
                    12f, c.fg2, maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                SettingsAction("Add a glyph matrix widget") { vm.addGlyphWidget() }
            }
            Spacer(Modifier.height(10.dp))
            if (vm.signalsAvailable) {
                SettingsToggle("Call signals", "React when a call arrives", "signals", vm.signalsEnabled, vm::applySignalsEnabled)
            } else {
                Txt("Call signals need Android 12 or newer.", 12f, c.fg2, maxLines = 2)
            }
            Spacer(Modifier.height(10.dp))
            SettingsToggle(
                "Notification signals",
                "Counts notifications only, never their content",
                "bell",
                vm.notifSignalsEnabled,
                vm::applyNotifSignals
            )
            LaunchedEffect(vm.notifSignalsEnabled) { vm.refreshNotifAccess() }
            if (vm.notifSignalsEnabled) {
                Spacer(Modifier.height(6.dp))
                Txt(
                    if (vm.notifAccess) {
                        "Notification access is on, so new notifications reach the matrix."
                    } else {
                        "Notification access is off, so real notifications cannot reach the matrix yet."
                    },
                    12f, c.fg2, maxLines = 2
                )
                if (!vm.notifAccess) {
                    Spacer(Modifier.height(10.dp))
                    SettingsAction("Grant notification access") { vm.openNotificationAccess() }
                    Spacer(Modifier.height(8.dp))
                    SettingsAction("Check again") { vm.refreshNotifAccess() }
                } else {
                    val counts = vm.signalCounts()
                    Spacer(Modifier.height(6.dp))
                    Txt(
                        "Counting signals: ${counts.second} in the last hour, ${counts.first} in total. Held in memory only.",
                        12f, c.fg2, maxLines = 2
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            SettingsAction("Test reaction") { vm.testNotificationReaction() }
            vm.signalNotice?.let {
                Spacer(Modifier.height(6.dp))
                Txt(it, 12f, c.fg2, maxLines = 2)
            }
        }
    }
}

@Composable
private fun AdvancedPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Advanced", onBack = { vm.settingsPage = SettingsPage.Root }) {
        SettingsGroup("Type & density") {
            Txt("Font", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf("system" to "System", "serif" to "Serif", "mono" to "Mono", "poppins" to "Poppins", "geist" to "Geist"),
                vm.uiFont,
                vm::applyUiFont
            )
            Spacer(Modifier.height(10.dp))
            SettingsToggle("Compact density", "Fit more without changing text size", "layout", vm.compactDensity, vm::applyCompactDensity)
        }
        SettingsGroup("Apps list") {
            SettingsToggle("Normal scroll", "Continuous A-Z list like a phone. Off = letter pages", "apps", vm.normalScroll, vm::applyNormalScroll)
            Spacer(Modifier.height(12.dp))
            Txt("Alphabet scrubber", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf(AlphaSide.Right to "Right", AlphaSide.Left to "Left", AlphaSide.Off to "Off"),
                vm.alphaSide,
                vm::applyAlphaSide
            )
        }
        SettingsGroup("Motion") {
            Txt("${(vm.motionIntensity * 100).toInt()}% motion", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            GlassSlider(vm.motionIntensity, vm::applyMotionIntensity)
        }
        SettingsGroup("Onboarding") {
            SettingsAction("Restart onboarding") { vm.resetOnboarding() }
        }
    }
}

@Composable
private fun PrivacyPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    val version = remember(ctx) { appVersionLabel(ctx) }
    SettingsScaffold("Privacy & about", onBack = { vm.settingsPage = SettingsPage.Root }) {
        SettingsGroup("On this device") {
            Txt(
                "Plan, widget, and launcher preferences stay on this device. No account or analytics required.",
                13f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(12.dp))
            SettingsAction("Privacy policy") {
                ctx.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://jainam6610.github.io/calm-ui/".toUri()
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            SettingsAction("About Calm UI") { vm.aboutOpen = true }
        }
        SettingsGroup("Version") {
            Txt("Calm UI $version", 13f, c.fg2)
        }
    }
}

@Composable
internal fun SettingSection(title: String, summary: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalColors.current
    SettingsGroup(title) {
        if (summary != null) {
            Txt(summary, 12f, c.fg2, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        }
        content()
    }
}

@Composable
internal fun SwitchSetting(title: String, sub: String?, on: Boolean, change: (Boolean) -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, 16f, c.fg)
            if (sub != null) Txt(sub, 12f, c.fg2, maxLines = 2)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(on, change)
    }
}
