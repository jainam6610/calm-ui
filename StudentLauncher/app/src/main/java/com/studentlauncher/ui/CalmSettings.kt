package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.calm.launcher.CalmConfig
import com.calm.launcher.CalmSlot
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.InternalDestination
import com.studentlauncher.data.SettingsPage

/**
 * Detox home apps, editable from the normal launcher. Writes the same "calm" prefs the Detox
 * activity reads, so the list is already right when a Detoxify session starts.
 */
@Composable
internal fun CalmSettingsPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    var pickSlot by remember { mutableStateOf<CalmSlot?>(null) }
    LaunchedEffect(Unit) { vm.reloadCalmConfig() }
    // Read the revision so picks and count changes recompose this page.
    @Suppress("UNUSED_VARIABLE") val revision = vm.calmConfigVersion

    SettingsScaffold(
        "Detox home",
        onBack = { vm.settingsPage = SettingsPage.Root }
    ) {
        SettingSection("apps on home", "${vm.calmCount} apps") {
            Txt(
                "The short list is 9 apps: seven you choose, plus phone and camera. The long list adds six more app slots.",
                12f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(10.dp))
            Segmented(
                listOf(9 to "9 apps", 15 to "15 apps"),
                vm.calmCount,
                vm::setCalmAppCount
            )
        }

        SettingSection("which apps", "tap to change") {
            vm.calmSlots().forEach { slot ->
                val value = if (vm.calmChoicePending(slot.id)) "changing in 24h" else vm.calmSlotValue(slot)
                CalmConfigRow(slot, value) {
                    if (!CalmConfig.isFixed(slot)) pickSlot = slot
                }
            }
            Spacer(Modifier.height(4.dp))
            Txt(
                "Browser is built in. Phone and camera always stay at the end. Pointing a default row at another app waits 24 hours, so the switch is never impulsive.",
                11f, c.fg2, maxLines = 4
            )
        }

        Spacer(Modifier.height(20.dp))
        PillButton(
            if (vm.detox != null) "back to the session" else "start detoxify",
            {
                vm.destination = InternalDestination.Home
                vm.openDetoxSheet()
            },
            filled = true
        )
    }

    val slot = pickSlot
    if (slot != null) CalmAppPicker(vm, slot) { pickSlot = null }
}

@Composable
private fun CalmConfigRow(slot: CalmSlot, value: String, onClick: () -> Unit) {
    val c = LocalColors.current
    val fixed = CalmConfig.isFixed(slot)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (fixed) Modifier else Modifier.tap(onClick))
            .padding(horizontal = 2.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Txt(slot.label, 15f, if (fixed) c.fg2 else c.fg, modifier = Modifier.weight(1f))
        Txt(value, 13f, c.fg2)
        if (!fixed) Txt("\u203A", 17f, c.fg2)
    }
}

@Composable
private fun CalmAppPicker(vm: LauncherViewModel, slot: CalmSlot, onDismiss: () -> Unit) {
    val c = LocalColors.current
    var query by remember { mutableStateOf("") }
    val extra = CalmConfig.isExtra(slot)
    val chosen = vm.calmChoice(slot.id)
    val apps = vm.apps
        .filter { it.label.contains(query, true) || it.pkg.contains(query, true) }
        .sortedBy { it.label.lowercase() }

    BottomSheet(onDismiss, maxHeightFrac = 0.9f) {
        Txt(slot.label, 22f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(4.dp))
        Txt(
            if (extra) "Choose an app for this extra slot."
            else "Keep the default app, or point this row somewhere else.",
            12f, c.fg2, maxLines = 2
        )
        Spacer(Modifier.height(12.dp))
        GlassField(query, "Search apps", { query = it })
        Spacer(Modifier.height(10.dp))
        PickerRow(
            if (extra) "Leave empty" else "Use the default app",
            if (chosen.isEmpty()) "currently selected" else "",
            chosen.isEmpty()
        ) {
            vm.setCalmChoice(slot.id, "")
            onDismiss()
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            apps.forEach { app ->
                PickerRow(app.label, "", app.pkg == chosen) {
                    vm.setCalmChoice(slot.id, app.pkg)
                    onDismiss()
                }
            }
        }
        if (apps.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Txt("No apps match.", 13f, c.fg2)
        }
    }
}

@Composable
private fun PickerRow(label: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.track else Color.Transparent)
            .tap(onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(label, 15f, c.fg)
            if (sub.isNotEmpty()) Txt(sub, 11f, c.fg2)
        }
        if (selected) Txt("\u2713", 15f, c.fg)
    }
}
