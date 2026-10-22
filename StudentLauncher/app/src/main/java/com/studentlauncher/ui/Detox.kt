package com.studentlauncher.ui

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.detox.DetoxPolicy
import java.time.format.DateTimeFormatter

/**
 * The Detoxify screen set. Home swaps to [DetoxHome] while a session runs — nothing but the
 * flat quiet home, the countdown, the allow-list and phone/camera. The child sheets are hosted
 * from SharedSheets so they appear over Plan and Settings too.
 */

// Calm's flat base: ship the launcher's own palette away for the length of a session.
private val DetoxDarkBg = Color.Black
private val DetoxLightBg = Color(0xFFF2F2F4)

@Composable
fun DetoxHome(vm: LauncherViewModel) {
    val c = LocalColors.current
    val session = vm.detox ?: return
    val now = rememberNow()
    BackHandler(enabled = vm.destination != InternalDestination.Home) {
        vm.destination = InternalDestination.Home
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(if (c.dark) DetoxDarkBg else DetoxLightBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(38.dp))
        Txt("detoxify", 12f, c.fg2)
        Spacer(Modifier.height(4.dp))
        Txt(DetoxPolicy.label(vm.detoxLeft), 54f, c.fg, FontWeight.Light)
        Txt(
            now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).lowercase(),
            13f, c.fg2, modifier = Modifier.padding(top = 2.dp)
        )
        if (vm.detoxReleaseAt != 0L) {
            Spacer(Modifier.height(12.dp))
            val rl = DetoxPolicy.releaseLeftMs(vm.detoxReleaseAt, System.currentTimeMillis())
            Txt("ends early in ${DetoxPolicy.label(rl)}", 12f, c.accent)
            Txt(
                "withdraw", 12f, c.fg2,
                modifier = Modifier.tap { vm.cancelDetoxRelease() }.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(28.dp))

        val sv = rememberScrollState()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(sv)) {
            userApps(vm, session).forEach { a ->
                Txt(
                    a.label.lowercase(), 24f, c.fg, FontWeight.Light,
                    modifier = Modifier.tap { vm.startApp(a.pkg) }.padding(vertical = 9.dp)
                )
            }
            Txt(
                "clock", 24f, c.fg, FontWeight.Light,
                modifier = Modifier.tap {
                    vm.openSystemIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }.padding(vertical = 9.dp)
            )
            if (userApps(vm, session).isEmpty()) {
                Txt(
                    "nothing else to open. phone, camera and clock stay.",
                    15f, c.fg2, maxLines = 3, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Txt("plan", 14f, c.fg2, modifier = Modifier.tap { vm.destination = InternalDestination.Plan })
            Txt("settings", 14f, c.fg2, modifier = Modifier.tap { vm.destination = InternalDestination.Settings })
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PillButton("phone", { vm.openSystemIntent(Intent(Intent.ACTION_DIAL)) }, Modifier.weight(1f))
            PillButton("camera", { vm.openSystemIntent(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }, Modifier.weight(1f))
        }
    }
}

/** The apps the user ticked, minus the always-allowed set (those live in the bottom pills). */
private fun userApps(vm: LauncherViewModel, session: DetoxPolicy.Session) =
    session.allowed
        .filter { it !in vm.detoxAlways }
        .mapNotNull { vm.appFor(it) }
        .sortedBy { it.label.lowercase() }

@Composable
internal fun DetoxStartSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    var lenMs by remember { mutableStateOf(DetoxPolicy.DAY_MS) }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    val label = DetoxPolicy.durations.first { it.first == lenMs }.second
    BottomSheet({ vm.detoxSheetOpen = false }, 0.92f) {
        Txt("Detoxify", 22f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Txt(
            "Your phone becomes this quiet home until the timer ends. Tick only what you " +
                "may use — everything else disappears. Ending early takes your PIN and a wait.",
            12f, c.fg2, maxLines = 4
        )
        Spacer(Modifier.height(14.dp))
        Segmented(DetoxPolicy.durations, lenMs, { lenMs = it })
        Spacer(Modifier.height(14.dp))
        if (!vm.pinSet) {
            Txt("A PIN is needed to start — it is the only way out before the timer.", 12f, c.fg2, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            PillButton("Set a PIN", { vm.requestCreatePin() }, filled = true)
        }
        Spacer(Modifier.height(12.dp))
        vm.apps.sortedBy { it.label.lowercase() }.forEach { app ->
            val always = app.pkg in vm.detoxAlways
            val on = always || app.pkg in picked
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).tap {
                    if (!always) picked = if (on) picked - app.pkg else picked + app.pkg
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(app.label, 14f, c.fg, modifier = Modifier.weight(1f))
                Txt(when { always -> "always"; on -> "allowed"; else -> "" }, 12f, c.fg2)
            }
        }
        Spacer(Modifier.height(12.dp))
        PillButton(
            "Begin · $label",
            { if (vm.pinSet) vm.startDetox(lenMs, picked) else vm.toast = "Set a PIN first" },
            Modifier.fillMaxWidth(), filled = true
        )
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
internal fun DetoxBlockSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val pkg = vm.detoxBlocked ?: return
    BottomSheet({ vm.clearDetoxBlock() }) {
        Txt("During detoxify", 20f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Txt("${vm.appFor(pkg)?.label ?: "This app"} is not on the allow-list.", 13f, c.fg, maxLines = 2)
        Txt("${DetoxPolicy.label(vm.detoxLeft)} left", 13f, c.accent)
        Spacer(Modifier.height(10.dp))
        PillButton("Back", { vm.clearDetoxBlock() }, filled = true)
    }
}

@Composable
internal fun DetoxSummarySheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val s = vm.detoxSummary ?: return
    BottomSheet({ vm.dismissDetoxSummary() }) {
        Txt(if (s.endedByTimer) "Detoxify complete" else "Detoxify ended", 22f, c.fg, FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Txt(
            "${DetoxPolicy.durations.firstOrNull { it.first == s.lengthMs }?.second ?: "short run"} · " +
                "${s.allowedCount} apps stayed usable",
            13f, c.fg2
        )
        Spacer(Modifier.height(6.dp))
        Txt("Your phone is back to normal.", 13f, c.fg2, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        PillButton("Done", { vm.dismissDetoxSummary() }, filled = true)
    }
}
