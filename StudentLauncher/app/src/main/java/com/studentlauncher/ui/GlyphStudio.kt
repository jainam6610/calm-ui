package com.studentlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.SettingsPage
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Glyph Studio: one live preview, a resting mode, a burst library and the dot style.
 * Everything is a global default; a widget can override ambient, dot size and glow.
 */
@Composable
internal fun GlyphStudioPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    var category by remember { mutableStateOf(GlyphModes.CAT_QUIET) }
    var preview by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var progressTick by remember { mutableIntStateOf(0) }

    val shown = preview ?: vm.glyphAmbient
    val shownMeta = GlyphModes.meta(shown)
    LaunchedEffect(shown) {
        if (shown != GlyphModes.FOCUS) return@LaunchedEffect
        while (true) {
            delay(15_000)
            progressTick++
        }
    }
    val progress = when (shown) {
        GlyphModes.FOCUS -> {
            progressTick
            vm.glyphFocusProgress()
        }
        GlyphModes.TASKS -> vm.glyphTaskProgress()
        else -> 0f
    }
    val sizes = listOf(0.8f to "Small", 1f to "Medium", 1.12f to "Large")
    val size = sizes.minByOrNull { abs(it.first - vm.glyphDotScale) }?.first ?: 1f

    SettingsScaffold("Glyph matrix", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Ink.Card)
                .border(0.8.dp, Ink.Hairline, RoundedCornerShape(26.dp))
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlyphMatrix(
                mode = shown,
                color = c.fg,
                accent = c.accent,
                modifier = Modifier.size(188.dp),
                burstKey = tick,
                batteryPct = vm.batteryPct,
                charging = vm.batteryCharging,
                progress = progress,
                dotScale = vm.glyphDotScale,
                glow = vm.glyphGlow,
                custom = vm.glyphCustomCells()
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt(if (preview == null) "Resting" else "Preview", 11f, c.fg2, FontWeight.Bold)
                Txt(shownMeta?.label ?: shown, 17f, c.fg, FontWeight.Medium)
                Txt(shownMeta?.summary ?: "", 12f, c.fg2, maxLines = 2)
            }
            if (preview != null) PillButton("Stop", { preview = null })
        }

        SettingSection("Resting mode", shownMeta?.label ?: vm.glyphAmbient) {
            Txt(
                "What stays on the matrix between events. Static modes draw once and cost no frames.",
                12f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(10.dp))
            val ambient = listOf(
                GlyphModes.CLOCK to "Clock",
                GlyphModes.QUIET to "Quiet",
                GlyphModes.FOCUS to "Focus ring",
                GlyphModes.TASKS to "Task arc",
                GlyphModes.CUSTOM to "Custom",
                GlyphModes.OFF to "Off"
            )
            ambient.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (mode, label) ->
                        ChoiceChip(label, vm.glyphAmbient == mode, {
                            preview = null
                            vm.applyGlyphAmbient(mode)
                        }, Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (vm.glyphAmbient == GlyphModes.FOCUS) {
                Spacer(Modifier.height(4.dp))
                Txt("Wired to the first Study timer widget: ${(vm.glyphFocusProgress() * 100).toInt()}% elapsed.", 11f, c.fg2, maxLines = 2)
            }
            if (vm.glyphAmbient == GlyphModes.TASKS) {
                Spacer(Modifier.height(4.dp))
                Txt("Wired to the first Tasks widget, or Plan when none exists: ${(vm.glyphTaskProgress() * 100).toInt()}% done.", 11f, c.fg2, maxLines = 2)
            }
        }

        SettingSection("Burst library", preview?.let { GlyphModes.meta(it)?.label } ?: "Tap to preview") {
            Txt("Signal, focus and touch reactions. Tap a row or Play to preview it here.", 12f, c.fg2, maxLines = 2)
            Spacer(Modifier.height(8.dp))
            Segmented(
                GlyphModes.categories.map { it to it },
                category,
                { category = it }
            )
            Spacer(Modifier.height(6.dp))
            GlyphModes.byCategory(category).forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.track.copy(alpha = 0.45f))
                        .tap { preview = m.id; tick++ }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt(m.label, 14f, c.fg, FontWeight.Medium)
                        Txt(m.summary, 11f, c.fg2, maxLines = 2)
                    }
                    Txt(if (m.durationMs == 0) "Still" else "${m.durationMs / 1000f}s", 11f, c.fg2)
                    Spacer(Modifier.size(8.dp))
                    Box(
                        Modifier.size(48.dp).tap { preview = m.id; tick++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Txt(if (m.durationMs == 0) "View" else "\u25B6", 13f, if (m.durationMs == 0) c.fg2 else c.fg, FontWeight.Medium)
                    }
                }
            }
            if (preview != null) {
                Spacer(Modifier.height(8.dp))
                PillButton("Clear preview", { preview = null })
            }
        }

        SettingSection("Custom art", "${vm.glyphCustomCells().size} dots saved") {
            Txt(
                "Draw inside the circle. Only dots that fit the 25 by 25 mask are kept; everything stays on this device.",
                12f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(8.dp))
            CustomArtEditor(
                initial = vm.glyphCustomCells(),
                onSave = { cells ->
                    vm.applyGlyphCustomArt(encodeGlyphArt(cells))
                    preview = GlyphModes.CUSTOM
                    tick++
                },
                onUse = { cells ->
                    vm.applyGlyphCustomArt(encodeGlyphArt(cells))
                    vm.applyGlyphAmbient(GlyphModes.CUSTOM)
                    preview = GlyphModes.CUSTOM
                    tick++
                }
            )
        }

        SettingSection("Dot style", "${(vm.glyphDotScale * 100).toInt()}%") {
            Txt("Dot size applies to every glyph matrix. A widget can override it.", 12f, c.fg2, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Segmented(sizes, size, vm::applyGlyphDotScale)
            Spacer(Modifier.height(6.dp))
            SwitchSetting("Accent glow", "A soft halo behind lit and accent dots", vm.glyphGlow, vm::applyGlyphGlow)
        }

        SettingSection("Torch reactions", if (vm.torchEnabled) (if (vm.torch.available) "On" else "No torch found") else "Off") {
            Txt(
                "Done maps to two short flashes; a priority signal maps to three at 2.5 Hz. Touch, breathing, charging and low battery never reach the torch.",
                12f, c.fg2, maxLines = 4
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton("Torch settings", { vm.settingsPage = SettingsPage.Root })
                if (vm.torchEnabled && vm.torch.available) {
                    PillButton("Test", { vm.testTorch() })
                    PillButton("Preview Done", { vm.reactionDone() })
                }
            }
            vm.torchNotice?.let {
                Spacer(Modifier.height(6.dp))
                Txt(it, 12f, c.fg2, maxLines = 2)
            }
        }
    }
}

/** A 25 x 25 dot canvas. Draw or erase, then save locally. */
@Composable
private fun CustomArtEditor(
    initial: Set<Int>,
    onSave: (Set<Int>) -> Unit,
    onUse: (Set<Int>) -> Unit
) {
    val c = LocalColors.current
    var cells by remember(initial) { mutableStateOf(initial) }
    var erase by remember { mutableStateOf(false) }

    Column {
        Segmented(listOf(false to "Draw", true to "Erase"), erase, { erase = it })
        Spacer(Modifier.height(8.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(c.tile)
                .pointerInput(erase) {
                    val grid = GlyphMatrixSpec.GRID
                    var lastIdx = -1
                    fun paint(pos: Offset) {
                        val cell = size.width / grid.toFloat()
                        if (cell <= 0f) return
                        val x = (pos.x / cell).toInt()
                        val y = (pos.y / cell).toInt()
                        if (x !in 0 until grid || y !in 0 until grid) return
                        if (!GlyphMatrixSpec.contains(x, y)) return
                        val idx = y * grid + x
                        if (idx == lastIdx) return
                        lastIdx = idx
                        cells = if (erase) cells - idx else cells + idx
                    }
                    detectDragGestures(
                        onDragStart = {
                            lastIdx = -1
                            paint(it)
                        },
                        onDragEnd = { lastIdx = -1 },
                        onDragCancel = { lastIdx = -1 },
                        onDrag = { change, _ ->
                            change.consume()
                            paint(change.position)
                        }
                    )
                }
        ) {
            val grid = GlyphMatrixSpec.GRID
            val cell = size.minDimension / grid
            val ox = (size.width - cell * grid) / 2f
            val oy = (size.height - cell * grid) / 2f
            val dim = ArrayList<Offset>(GlyphMatrixSpec.count)
            val lit = ArrayList<Offset>(GlyphMatrixSpec.count)
            for (y in 0 until grid) {
                for (x in 0 until grid) {
                    if (!GlyphMatrixSpec.contains(x, y)) continue
                    val o = Offset(ox + (x + 0.5f) * cell, oy + (y + 0.5f) * cell)
                    if (y * grid + x in cells) lit.add(o) else dim.add(o)
                }
            }
            drawPoints(dim, PointMode.Points, c.fg2.copy(alpha = 0.16f), cell * 0.48f, StrokeCap.Round)
            drawPoints(lit, PointMode.Points, c.fg, cell * 0.64f, StrokeCap.Round)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Save", { onSave(cells) })
            PillButton("Use as resting", { onUse(cells) })
            PillButton("Clear", { cells = emptySet() }, danger = true)
        }
        Spacer(Modifier.height(4.dp))
        Txt("${cells.size} dots · drag to draw, choose Erase to take one back", 11f, c.fg2, maxLines = 2)
    }
}
