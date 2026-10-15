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
import com.studentlauncher.data.IconStyle
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
fun SettingsApp(vm: LauncherViewModel, pickPhoto: () -> Unit, pickShufflePhotos: () -> Unit, requestNotifications: () -> Unit) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    androidx.activity.compose.BackHandler { vm.back() }
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

        SettingSection("Home", "${vm.pinned.size}/${vm.homeAppLimit} apps") {
            Txt("Icon style", 12f, c.fg2)
            Segmented(listOf(IconStyle.Mono to "Mono", IconStyle.Original to "Original", IconStyle.MinimalBubble to "Bubble", IconStyle.MinimalOutline to "Outline"), vm.iconStyle, vm::applyIconStyle)
            Spacer(Modifier.height(8.dp))
            Txt("Icon shape", 12f, c.fg2)
            Segmented(listOf(IconShape.None to "None", IconShape.Circle to "Circle", IconShape.Squircle to "Squircle"), vm.iconShape, vm::applyIconShape)
            Spacer(Modifier.height(8.dp))
            Txt("Layout", 12f, c.fg2)
            Segmented(listOf(HomeLayout.List to "List", HomeLayout.Grid to "Grid"), vm.layout, vm::applyLayout)
            Spacer(Modifier.height(8.dp))
            SwitchSetting("Show app names", "Display labels under app icons", vm.showNames, vm::applyShowNames)
            Spacer(Modifier.height(8.dp))
            Txt("Home app limit: ${vm.homeAppLimit}", 12f, c.fg2)
            Segmented(
                listOf(4 to "4", 6 to "6", 8 to "8", 10 to "10", 12 to "12"),
                vm.homeAppLimit,
                vm::applyHomeAppLimit
            )
            Spacer(Modifier.height(8.dp))
            PillButton("Pick home apps (${vm.pinned.size}/${vm.homeAppLimit})", { vm.pickHomeApps = true })
            Spacer(Modifier.height(8.dp))
            Txt("Dock style", 12f, c.fg2)
            Segmented(listOf(DockStyle.Bottom to "Bottom", DockStyle.Rail to "Rail", DockStyle.Off to "Off"), vm.dockStyle, vm::applyDockStyle)
            Spacer(Modifier.height(8.dp))
            PillButton("Add or arrange widgets", { vm.widgetCenter = true })
        }

        SettingSection("Wallpaper", when (vm.wallpaper) { "photo" -> "Custom photo"; "shuffle" -> "Shuffle ${vm.shuffleCount}/5"; else -> vm.wallpaper }) {
            val previews = remember(c.dark) {
                Wallpapers.presets.map { it to Wallpapers.preset(it, c.dark, 96, 96).asImageBitmap() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                previews.forEach { (id, bmp) ->
                    val on = vm.wallpaper == id
                    androidx.compose.foundation.Image(
                        bmp, id, Modifier.size(34.dp).clip(CircleShape)
                            .border(if (on) 2.dp else 0.8.dp, if (on) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                            .tap { vm.applyWallpaper(id) },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                val photo = vm.wallpaper == "photo"
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(c.track)
                        .border(if (photo) 2.dp else 0.8.dp, if (photo) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                        .tap { pickPhoto() },
                    contentAlignment = Alignment.Center
                ) { Txt("+", 18f, c.fg) }
                val shuffleOn = vm.wallpaper == "shuffle"
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(c.accent)
                        .border(if (shuffleOn) 2.dp else 0.8.dp, if (shuffleOn) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                        .tap { vm.applyWallpaper("shuffle") },
                    contentAlignment = Alignment.Center
                ) { Txt("S", 13f, Color.White, FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            if (vm.wallpaper == "shuffle") {
                Txt("Shuffle photos (one per day)", 12f, c.fg2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until Wallpapers.MAX_SHUFFLE).forEach { i ->
                        val filled = i < vm.shuffleCount
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(if (filled) c.track else c.track.copy(alpha = 0.4f))
                                .border(if (i == vm.shuffleSlot && filled) 2.dp else 0.8.dp, if (i == vm.shuffleSlot && filled) c.accent else c.fg2.copy(alpha = 0.3f), CircleShape)
                                .tap {
                                    if (filled) vm.removeShuffleSlot(i)
                                    else pickShufflePhotos()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Txt(if (filled) "×" else "+", 16f, if (filled) c.fg else c.fg2)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                PillButton("Pick photos (up to 5)", pickShufflePhotos)
                Spacer(Modifier.height(6.dp))
                Txt("Tap a filled circle to remove that photo.", 11f, c.fg2, maxLines = 2)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            PillButton("Adjust wallpaper", { vm.wallpaperEditor = true })
            Spacer(Modifier.height(8.dp))
            PillButton("Choose wallpaper", pickPhoto)
        }

        SettingSection("Appearance", when (vm.themeMode) { ThemeMode.Dark -> "Dark"; ThemeMode.Light -> "Light"; else -> "Auto" }) {
            Segmented(
                listOf(ThemeMode.Dark to "Dark", ThemeMode.Light to "Light", ThemeMode.System to "Auto"),
                vm.themeMode,
                vm::applyTheme
            )
            Spacer(Modifier.height(10.dp))
            SwitchSetting("Wallpaper accent", "Picks accent colour from your wallpaper", vm.wpAccent, vm::applyWpAccent)
            Spacer(Modifier.height(10.dp))
            Txt("Accent", 12f, c.fg2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("red", "blue", "green", "purple", "orange", "teal", "pink").forEach { id ->
                    val col = accentColor(id)
                    val on = vm.accentId == id
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(col)
                            .border(if (on) 2.dp else 0.8.dp, if (on) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                            .tap { vm.applyAccent(id) }
                    )
                }
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
            SwitchSetting("Normal scroll", "Continuous A-Z list like a phone. Off = Niagara letter pages", vm.normalScroll, vm::applyNormalScroll)
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
private fun SettingSection(title: String, summary: String? = null, content: @Composable ColumnScope.() -> Unit) {
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
private fun SwitchSetting(title: String, sub: String?, on: Boolean, change: (Boolean) -> Unit) {
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
