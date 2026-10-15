package com.studentlauncher.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AlphaSide
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.SettingsPage
import com.studentlauncher.data.ThemeMode

@Composable
fun Onboarding(vm: LauncherViewModel, requestNotifications: () -> Unit) {
    val c = LocalColors.current
    val motion = LocalMotionScale.current
    var page by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF09090A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Txt("STUDENT / QUIET", 11f, Color(0x99FFFFFF), FontWeight.Bold)
                if (page < 2) PillButton("Skip", vm::finishOnboarding)
            }

            Spacer(Modifier.weight(1f))

            AnimatedContent(
                page,
                transitionSpec = {
                    fadeIn(tween((240 * motion).coerceAtLeast(1f).toInt())) togetherWith fadeOut(tween((120 * motion).coerceAtLeast(1f).toInt()))
                },
                label = "onboarding"
            ) { p ->
                val (kicker, title, body) = when (p) {
                    0 -> Triple(
                        "A CALMER HOME",
                        "Make room for what matters.",
                        "Your apps, classes, and next small step — with less pull toward distraction."
                    )
                    1 -> Triple(
                        "FOCUS, YOUR WAY",
                        "Gentle boundaries, never guilt.",
                        "Choose a pause before distracting apps. Notifications are optional and only support reminders you enable."
                    )
                    else -> Triple(
                        "MAKE IT YOURS",
                        "How much space do you need?",
                        "Choose a breathable layout now. You can change it anytime in Settings."
                    )
                }
                Column {
                    Box(
                        Modifier.size(54.dp).clip(CircleShape).background(c.accent),
                        contentAlignment = Alignment.Center
                    ) { Txt((p + 1).toString(), 19f, Color.White, FontWeight.Bold) }
                    Spacer(Modifier.height(28.dp))
                    Txt(kicker, 11f, Color(0x99FFFFFF), FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Txt(title, 32f, Color.White, FontWeight.SemiBold, maxLines = 2)
                    Spacer(Modifier.height(12.dp))
                    Txt(body, 16f, Color(0xB3FFFFFF), maxLines = 4)
                    if (p == 2) {
                        Spacer(Modifier.height(24.dp))
                        Segmented(
                            listOf(false to "Breathing room", true to "Compact"),
                            vm.compactDensity,
                            onSelect = { vm.applyCompactDensity(it) }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            if (page == 1) PillButton("Allow focus reminders", requestNotifications, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (page > 0) PillButton("Back", { page-- })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(if (i == page) 18.dp else 7.dp, 7.dp)
                                .clip(CircleShape)
                                .background(if (i == page) c.accent else Color(0x44FFFFFF))
                        )
                    }
                }
                PillButton(
                    if (page == 2) "Done" else "Next",
                    { if (page == 2) vm.finishOnboarding() else page++ },
                    filled = true
                )
            }
        }
    }
}

@Composable
fun SettingsApp(
    vm: LauncherViewModel,
    pickPhoto: () -> Unit,
    pickShufflePhotos: () -> Unit,
    requestNotifications: () -> Unit
) {
    androidx.activity.compose.BackHandler { vm.back() }
    when (vm.settingsPage) {
        SettingsPage.Root -> SettingsRoot(vm, requestNotifications)
        SettingsPage.Personalization -> PersonalizationHub(vm)
        SettingsPage.Icons -> IconsPage(vm)
        SettingsPage.Theme -> ThemePage(vm)
        SettingsPage.Wallpaper -> WallpaperPage(vm, pickPhoto, pickShufflePhotos)
        SettingsPage.Home -> HomeSettingsPage(vm)
        SettingsPage.Clock -> ClockPage(vm)
    }
}

@Composable
private fun SettingsRoot(vm: LauncherViewModel, requestNotifications: () -> Unit) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Txt("LAUNCHER APP", 11f, c.fg2, FontWeight.Bold)
                Txt("Settings", 30f, c.fg, FontWeight.SemiBold)
            }
            PillButton("Home", { vm.destination = InternalDestination.Home })
        }

        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.tile)
                .tap { vm.settingsPage = SettingsPage.Personalization }
                .padding(16.dp)
        ) {
            Txt("PERSONALIZATION", 11f, c.fg2, FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Txt("Icons, theme, wallpaper and clock", 15f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Txt("Looks-only settings, moved off the main list.", 12f, c.fg2)
        }

        SettingSection("Reactions", if (vm.torchEnabled) (if (vm.torch.available) "Torch on" else "No torch found") else "Off") {
            Txt(
                "Flash the camera light when a focus session ends. Off by default. Never used for touch, breathing or low battery. Capped at 3 flashes per second, 3 seconds total, 12 per hour, with a 10 second cool-down; skipped in battery saver, Do Not Disturb, when warm, when battery is under 15% and charging is off, or with reduced motion.",
                12f, c.fg2, maxLines = 8
            )
            Spacer(Modifier.height(10.dp))
            SwitchSetting("Torch reactions", "Camera light flashes for finished sessions", vm.torchEnabled, vm::applyTorchEnabled)
            if (!vm.torch.available) {
                Spacer(Modifier.height(6.dp))
                Txt("This device reports no flash unit, so torch reactions stay off.", 12f, c.fg2, maxLines = 2)
            }
            if (vm.torchEnabled) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Test torch", { vm.testTorch() })
                    PillButton("Preview Done", { vm.reactionDone() })
                }
                vm.torchNotice?.let {
                    Spacer(Modifier.height(6.dp))
                    Txt(it, 12f, c.fg2, maxLines = 2)
                }
            }
        }

        SettingSection("Signals (calls)", if (!vm.signalsAvailable) "Needs Android 12+" else if (vm.signalsEnabled) "On" else "Off") {
            Txt(
                "When a call comes in, one accelerometer reading decides the reaction: face up shows a matrix pulse and never the torch; face down uses the torch (if enabled) and stays off the matrix; in a pocket does nothing. Calls are detected from the audio mode alone. The launcher never reads caller names, numbers, SMS, contacts or call logs, and nothing is stored.",
                12f, c.fg2, maxLines = 8
            )
            Spacer(Modifier.height(10.dp))
            if (!vm.signalsAvailable) {
                Txt("Audio-mode call detection needs Android 12 or newer.", 12f, c.fg2, maxLines = 2)
            } else {
                SwitchSetting("Call signals", "React when a call arrives", vm.signalsEnabled, vm::applySignalsEnabled)
            }
        }

        SettingSection("Focus") {
            SwitchSetting("Study mode", "Hide apps marked distracting", vm.studyMode, vm::applyStudyMode)
            SwitchSetting("Mindful Mode", "Escalating pauses + session tracking", vm.mindfulEnabled, vm::applyMindful)
            AnimatedVisibility(vm.mindfulEnabled) {
                Column {
                    SwitchSetting("Escalating pause", "Delay grows each time you open a distracting app", vm.escalatingEnabled, vm::applyEscalating)
                    SwitchSetting("Session tracking", "Track time in distracting apps", vm.trackingEnabled, vm::applyTracking)
                    SwitchSetting("Earn your scroll", "Do a quick task to unlock for 10 min", vm.earnScrollEnabled, vm::applyEarnScroll)
                    Spacer(Modifier.height(6.dp))
                    Txt("Daily budget: ${vm.dailyBudgetMin} min", 12f, c.fg2)
                    Segmented(
                        listOf(30 to "30 min", 60 to "60 min", 90 to "90 min", 120 to "2h"),
                        vm.dailyBudgetMin,
                        vm::applyDailyBudget
                    )
                    Spacer(Modifier.height(8.dp))
                    PillButton("Mindful Mode settings", { vm.mindful.settingsOpen = true })
                    Spacer(Modifier.height(8.dp))
                    PillButton("App limits", { vm.mindful.appLimitsOpen = true })
                }
            }
            Segmented(
                listOf(0 to "No pause", 5 to "5 sec", 10 to "10 sec"),
                vm.pauseSec,
                vm::applyPauseSec
            )
            Spacer(Modifier.height(8.dp))
            Txt("Reminder interval", 12f, c.fg2)
            Segmented(
                listOf(5 to "5 min", 10 to "10 min", 15 to "15 min"),
                vm.reminderMin,
                { v -> vm.applyReminderMin(v); if (v > 0) requestNotifications() }
            )
            Spacer(Modifier.height(8.dp))
            PillButton("Allow reminders", requestNotifications)
            Spacer(Modifier.height(8.dp))
            if (!vm.pinSet) {
                PillButton("Set a PIN", { vm.requestCreatePin() })
            } else {
                SwitchSetting("Protect Study mode with PIN", null, vm.protectStudy, vm::applyProtectStudy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Change PIN", { vm.requestChangePin() }, Modifier.weight(1f))
                    PillButton("Remove PIN", { vm.requestRemovePin() }, Modifier.weight(1f))
                }
            }
        }

        SettingSection("Advanced", "${(vm.motionIntensity * 100).toInt()}% motion") {
            Txt("Font", 12f, c.fg2)
            Segmented(listOf("system" to "System", "serif" to "Serif", "mono" to "Mono", "poppins" to "Poppins", "geist" to "Geist"), vm.uiFont, vm::applyUiFont)
            Spacer(Modifier.height(10.dp))
            SwitchSetting("Compact density", "Fit more without changing text size", vm.compactDensity, vm::applyCompactDensity)
            Spacer(Modifier.height(8.dp))
            SwitchSetting("Normal scroll", "Continuous A-Z list like a phone. Off = letter pages", vm.normalScroll, vm::applyNormalScroll)
            Spacer(Modifier.height(8.dp))
            Txt("Alphabet scrubber", 12f, c.fg2)
            Segmented(listOf(AlphaSide.Right to "Right", AlphaSide.Left to "Left", AlphaSide.Off to "Off"), vm.alphaSide, vm::applyAlphaSide)
            Txt("Motion", 12f, c.fg2, modifier = Modifier.padding(top = 14.dp))
            GlassSlider(vm.motionIntensity, vm::applyMotionIntensity)
            Spacer(Modifier.height(8.dp))
            PillButton("Restart onboarding", vm::resetOnboarding)
        }

        SettingSection("Privacy & About", "On this device") {
            Txt(
                "Plan, widget, and launcher preferences stay on this device. No account or analytics required.",
                13f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Privacy Policy", {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://jainam662010-boop.github.io/StudentLauncher/privacy.html")))
                })
                PillButton("About", { vm.aboutOpen = true })
            }
        }
    }
}

@Composable
internal fun SettingSection(title: String, summary: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalColors.current
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Txt(title.uppercase(), 11f, c.fg2, FontWeight.Bold, modifier = Modifier.weight(1f))
        if (summary != null) Txt(summary, 11f, c.fg2)
    }
    Spacer(Modifier.height(8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.tile)
            .padding(14.dp),
        content = content
    )
}

@Composable
internal fun SwitchSetting(title: String, sub: String?, on: Boolean, change: (Boolean) -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, 15f, c.fg)
            if (sub != null) Txt(sub, 12f, c.fg2, maxLines = 2)
        }
        GlassSwitch(on, change)
    }
}
