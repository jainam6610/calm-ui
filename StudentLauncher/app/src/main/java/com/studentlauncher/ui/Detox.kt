package com.studentlauncher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.detox.DetoxPolicy

/**
 * Detoxify opens the Detox home (see com.calm.launcher.MainActivity) and starts it with a session
 * deadline. So this file only holds the three small sheets the launcher itself puts over the
 * screen: the length picker that starts a session, the "not now" block notice, and the wrap-up
 * summary. Everything else about the mode lives in the Detox activity.
 */

@Composable
internal fun DetoxStartSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    BottomSheet({ vm.closeDetoxSheet() }) {
        Txt("Detoxify", 26f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(6.dp))
        Txt(
            "Detox home takes over the home screen for the length you pick. Its countdown runs in place of the clock. Your PIN is required to start and to leave early.",
            13f, c.fg2, maxLines = 3
        )
        Spacer(Modifier.height(14.dp))
        Txt("apps on detox home", 11f, c.fg2, FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Segmented(listOf(9 to "9 apps", 15 to "15 apps"), vm.calmCount, vm::setCalmAppCount)
        Spacer(Modifier.height(8.dp))
        PillButton("choose which apps", { vm.openCalmConfig() })
        Spacer(Modifier.height(14.dp))
        Segmented(DetoxPolicy.durations, vm.detoxDraftLen, vm::detoxDraftDuration)
        Spacer(Modifier.height(14.dp))
        PillButton("start", { vm.detoxBegin() }, filled = true)
    }
}

@Composable
internal fun DetoxBlockSheet(vm: LauncherViewModel) {
    val c = LocalColors.current
    val pkg = vm.detoxBlocked ?: return
    BottomSheet({ vm.clearDetoxBlock() }) {
        Txt("not now", 26f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(6.dp))
        Txt("${vm.appFor(pkg)?.label ?: "this app"} is off during detoxify.", 13f, c.fg2, maxLines = 2)
        Txt(
            "${DetoxPolicy.label(vm.detoxLeft)} left", 13f, c.fg2,
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
        Txt("${durationLabel(summary.lengthMs)} · your phone is back to normal.", 13f, c.fg2, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        PillButton("ok", { vm.dismissDetoxSummary() }, filled = true)
    }
}

private fun durationLabel(ms: Long): String =
    DetoxPolicy.durations.firstOrNull { it.first == ms }?.second ?: DetoxPolicy.label(ms)
