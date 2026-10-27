package com.studentlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.IconPack
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.SettingsPage
import com.studentlauncher.data.ThemeMode
import com.studentlauncher.data.homeLimitOptions

@Composable
internal fun PersonalizationHub(vm: LauncherViewModel) {
    val themeSummary = when (vm.themeMode) {
        ThemeMode.Dark -> "Dark"
        ThemeMode.Light -> "Light"
        ThemeMode.System -> "Auto"
    }
    val homeSummary = if (vm.layout == HomeLayout.Grid) "Grid · ${vm.homeColumns} columns" else "List"
    val wallpaperSummary = when (vm.wallpaper) {
        "photo" -> "Custom photo"
        "shuffle" -> "Shuffle ${vm.shuffleCount}/5"
        else -> vm.wallpaper.replaceFirstChar { it.uppercase() }
    }
    val activeMood = vm.currentMood()
    val c = LocalColors.current
    SettingsScaffold("Personalisation", onBack = { vm.settingsPage = SettingsPage.Root }) {
        PersonalisationPreview(vm)
        Spacer(Modifier.height(12.dp))
        SettingsGroup("Moods") {
            Txt(
                activeMood?.let { "Current: ${it.label}. One tap applies a complete look." }
                    ?: "Custom look. Pick a mood to reset the surface in one tap.",
                12f, c.fg2, maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            MOODS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { mood ->
                        MoodCard(mood, activeMood == mood, Modifier.weight(1f)) { vm.applyMood(mood) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Txt("Apps, tasks, widgets, permissions and your own photos are never changed.", 11f, c.fg2, maxLines = 2)
        }
        SettingsGroup {
            SettingsLink(
                "Icons",
                "${vm.iconPack.name} · ${vm.iconShape.name.lowercase()}",
                "icons",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        vm.apps.take(3).forEach { AppIconView(it, vm, 20.dp) }
                    }
                },
                onClick = { vm.settingsPage = SettingsPage.Icons }
            )
            SettingsLink(
                "Theme",
                "$themeSummary · ${vm.accentId}",
                "theme",
                trailing = { AccentDot(accentColor(vm.accentId)) },
                onClick = { vm.settingsPage = SettingsPage.Theme }
            )
            SettingsLink("Wallpaper", wallpaperSummary, "wallpaper") { vm.settingsPage = SettingsPage.Wallpaper }
            SettingsLink("Home layout", homeSummary, "layout") { vm.settingsPage = SettingsPage.Home }
            SettingsLink(
                "Clock",
                clockLabel(vm.clockStyle),
                "clock"
            ) { vm.settingsPage = SettingsPage.Clock }
            SettingsLink(
                "Glyph matrix",
                (GlyphModes.meta(vm.glyphAmbient)?.label ?: vm.glyphAmbient) +
                    " · " + (if (vm.glyphGlow) "glow" else "flat"),
                "focus"
            ) { vm.settingsPage = SettingsPage.Glyph }
        }
    }
}

/** Mood cards describe a look without painting a colour preview; selection is a hairline. */
@Composable
private fun MoodCard(
    mood: LauncherMood,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.tile)
            .border(
                if (active) 1.4.dp else 0.8.dp,
                if (active) c.fg else c.fg2.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            )
            .tap(onClick)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Txt(mood.label, 15f, c.fg, FontWeight.Medium, Modifier.weight(1f))
            if (active) Txt("ON", 10f, c.fg2, FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
        Txt(mood.tagline, 11f, c.fg2, maxLines = 2)
    }
}

/** A tiny home-screen preview so personalisation has a visual anchor. */
@Composable
private fun PersonalisationPreview(vm: LauncherViewModel) {
    val c = LocalColors.current
    val icons = vm.pinned.mapNotNull { vm.appFor(it) }.ifEmpty { vm.apps }.take(4)
    Box(
        Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (c.dark) Ink.Card else Color(0xFFF6F6F8))
            .border(0.8.dp, if (c.dark) Ink.Hairline else Color(0x1F000000), RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Txt("9:41", 30f, c.fg, FontWeight.Light, tracking = -0.5f)
                    Txt("MON, 12 MAY", 9f, c.fg2, FontWeight.Bold, tracking = 1.4f)
                }
                Txt("PREVIEW", 9f, c.fg2, FontWeight.Bold, tracking = 1.4f)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                icons.forEach { AppIconView(it, vm, 34.dp) }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(c.tile)
                    .border(0.6.dp, if (c.dark) Ink.Hairline else Color(0x14000000), RoundedCornerShape(17.dp))
            )
        }
    }
}

@Composable
internal fun IconsPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Icons", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingsGroup("Icon pack") {
            Txt(
                "One glyph, several styles. Apps without a themed layer keep their original icon.",
                12f, c.fg2, maxLines = 3
            )
            Spacer(Modifier.height(10.dp))
            ICON_PACKS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { p ->
                        IconPackCard(p, p == vm.iconPack, vm, Modifier.weight(1f)) { vm.applyIconPack(p) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        SettingsGroup("Shape") {
            Segmented(
                listOf(IconShape.None to "None", IconShape.Circle to "Circle", IconShape.Squircle to "Squircle"),
                vm.iconShape,
                vm::applyIconShape
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                vm.apps.take(5).forEach { AppIconView(it, vm, 44.dp) }
            }
        }
    }
}

private val ICON_PACKS = listOf(
    IconPack.Original, IconPack.Mono, IconPack.Hairline, IconPack.Bold, IconPack.Dot, IconPack.Bubble, IconPack.Tile
)

@Composable
private fun IconPackCard(
    pack: IconPack,
    on: Boolean,
    vm: LauncherViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.tile)
            .border(
                if (on) 1.4.dp else 0.8.dp,
                if (on) c.fg else c.fg2.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            )
            .tap(onClick)
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vm.apps.take(4).forEach { AppIconView(it, vm, 26.dp, packOverride = pack) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(pack.name, 13f, c.fg, FontWeight.Medium, modifier = Modifier.weight(1f))
            if (on) Txt("\u2713", 14f, c.fg)
        }
    }
}

@Composable
internal fun ThemePage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Theme", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingsGroup("Appearance") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeCard(ThemeMode.Dark, "Dark", vm.themeMode == ThemeMode.Dark, Modifier.weight(1f)) { vm.applyTheme(ThemeMode.Dark) }
                ThemeCard(ThemeMode.Light, "Light", vm.themeMode == ThemeMode.Light, Modifier.weight(1f)) { vm.applyTheme(ThemeMode.Light) }
                ThemeCard(ThemeMode.System, "Auto", vm.themeMode == ThemeMode.System, Modifier.weight(1f)) { vm.applyTheme(ThemeMode.System) }
            }
            Spacer(Modifier.height(12.dp))
            SettingsToggle(
                "Wallpaper accent",
                "Picks accent colour from your wallpaper",
                "theme",
                vm.wpAccent,
                vm::applyWpAccent
            )
        }
        SettingsGroup("Accent") {
            listOf("red", "blue", "green", "purple", "orange", "teal", "pink", "lime").chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { id ->
                        AccentSwatch(id, vm.accentId == id, Modifier.weight(1f)) { vm.applyAccent(id) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        SettingsGroup("Preview") {
            Box(
                Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(16.dp))
                    .background(c.tile).padding(14.dp)
            ) {
                Column {
                    Txt("9:41", 24f, c.fg, FontWeight.Light)
                    Txt("A quieter home screen", 11f, c.fg2)
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode,
    label: String,
    on: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    val bg: Brush = when (mode) {
        ThemeMode.Dark -> SolidColor(Color(0xFF101012))
        ThemeMode.Light -> SolidColor(Color.White)
        ThemeMode.System -> Brush.horizontalGradient(listOf(Color.White, Color(0xFF101012)))
    }
    val ink = if (mode == ThemeMode.Light) Color(0xFF1C1C1E) else Color.White
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.tile)
            .border(
                if (on) 1.4.dp else 0.8.dp,
                if (on) c.fg else c.fg2.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .tap(onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(10.dp)).background(bg)
        ) {
            Column(Modifier.padding(8.dp)) {
                Box(Modifier.width(22.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ink))
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(36.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(ink.copy(alpha = 0.5f)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Txt(label, 12f, c.fg, FontWeight.Medium)
    }
}

@Composable
private fun AccentSwatch(id: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalColors.current
    Column(modifier.tap(onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor(id))
                .then(if (on) Modifier.border(3.dp, c.fg, CircleShape) else Modifier)
                .semantics {
                    contentDescription = id
                    selected = on
                }
        )
        Spacer(Modifier.height(4.dp))
        Txt(id, 11f, if (on) c.fg else c.fg2, if (on) FontWeight.Medium else FontWeight.Normal)
    }
}

@Composable
internal fun WallpaperPage(vm: LauncherViewModel, pickPhoto: () -> Unit, pickShufflePhotos: () -> Unit) {
    val c = LocalColors.current
    SettingsScaffold("Wallpaper", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingsGroup("Presets") {
            val previews = remember(c.dark) {
                Wallpapers.presets.map { it to Wallpapers.preset(it, c.dark, 360, 240).asImageBitmap() }
            }
            previews.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (id, bmp) ->
                        val on = vm.wallpaper == id
                        Column(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .border(
                                    if (on) 1.4.dp else 0.8.dp,
                                    if (on) c.fg else c.fg2.copy(alpha = 0.2f),
                                    RoundedCornerShape(18.dp)
                                )
                                .tap { vm.applyWallpaper(id) }
                                .padding(4.dp)
                        ) {
                            Image(
                                bmp, null,
                                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(6.dp))
                            Txt(
                                id.replaceFirstChar { it.uppercase() }, 12f, c.fg, FontWeight.Medium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        SettingsGroup("Your photos") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PhotoCard("Your photo", vm.wallpaper == "photo", Modifier.weight(1f)) { pickPhoto() }
                PhotoCard("Shuffle", vm.wallpaper == "shuffle", Modifier.weight(1f)) { vm.applyWallpaper("shuffle") }
            }
            if (vm.wallpaper == "shuffle") {
                Spacer(Modifier.height(14.dp))
                Txt("One photo per day. Tap a filled slot to remove it.", 12f, c.fg2, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until Wallpapers.MAX_SHUFFLE).forEach { i ->
                        val filled = i < vm.shuffleCount
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (filled) c.track else c.track.copy(alpha = 0.4f))
                                .border(
                                    if (i == vm.shuffleSlot && filled) 1.4.dp else 0.8.dp,
                                    if (i == vm.shuffleSlot && filled) c.fg else c.fg2.copy(alpha = 0.25f),
                                    RoundedCornerShape(14.dp)
                                )
                                .tap { if (filled) vm.removeShuffleSlot(i) else pickShufflePhotos() },
                            contentAlignment = Alignment.Center
                        ) { Txt(if (filled) "\u00D7" else "+", 18f, if (filled) c.fg else c.fg2) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SettingsAction("Pick photos (up to 5)") { pickShufflePhotos() }
            }
            Spacer(Modifier.height(10.dp))
            SettingsAction("Adjust wallpaper") { vm.wallpaperEditor = true }
            Spacer(Modifier.height(8.dp))
            SettingsAction("Choose a photo") { pickPhoto() }
        }
    }
}

@Composable
private fun PhotoCard(label: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = LocalColors.current
    Column(
        modifier
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(c.tile)
            .border(
                if (on) 1.4.dp else 0.8.dp,
                if (on) c.fg else c.fg2.copy(alpha = 0.2f),
                RoundedCornerShape(18.dp)
            )
            .tap(onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Txt(if (label == "Shuffle") "S" else "+", 24f, c.fg, FontWeight.Light)
        Spacer(Modifier.height(6.dp))
        Txt(label, 12f, c.fg, FontWeight.Medium)
    }
}

@Composable
internal fun HomeSettingsPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Home layout", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingsGroup("Layout") {
            Segmented(listOf(HomeLayout.List to "List", HomeLayout.Grid to "Grid"), vm.layout, vm::applyLayout)
            Spacer(Modifier.height(10.dp))
            SettingsToggle("Show app names", "Display labels under app icons", "apps", vm.showNames, vm::applyShowNames)
        }
        SettingsGroup("Home apps") {
            Txt(stringResource(R.string.grid_columns), 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf(3 to "3", 4 to "4", 5 to "5"),
                vm.homeColumns,
                vm::applyHomeColumns
            )
            Spacer(Modifier.height(12.dp))
            Txt(stringResource(R.string.apps_on_home), 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                homeLimitOptions(vm.homeColumns, vm.homeAppLimit).map { it to "$it" },
                vm.homeAppLimit,
                vm::applyHomeAppLimit
            )
            Spacer(Modifier.height(10.dp))
            SettingsAction("Pick home apps (${vm.pinned.size}/${vm.homeAppLimit})") { vm.pickHomeApps = true }
        }
        SettingsGroup("Dock") {
            Segmented(
                listOf(DockStyle.Bottom to "Bottom", DockStyle.Rail to "Rail", DockStyle.Off to "Off"),
                vm.dockStyle,
                vm::applyDockStyle
            )
        }
        SettingsGroup("Widgets") {
            SettingsAction("Add or arrange widgets") { vm.widgetCenter = true }
        }
    }
}

@Composable
internal fun ClockPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    val now = rememberNow()
    val is24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a"))
    SettingsScaffold("Clock", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        ClockHero(vm.clockStyle, time, is24, now)
        Spacer(Modifier.height(8.dp))
        Txt(
            "Live preview · ${clockLabel(vm.clockStyle)}",
            11f, c.fg2,
            modifier = Modifier.padding(start = 4.dp)
        )
        SettingsGroup("Signature") {
            DotSignatureCard(vm.clockStyle == "Dot") { vm.applyClockStyle("Dot") }
        }
        SettingsGroup("More styles") {
            CLOCK_STYLES.filter { it != "Dot" }.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { style ->
                        ClockCard(style, style == vm.clockStyle, is24, Modifier.weight(1f)) {
                            vm.applyClockStyle(style)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        SettingsGroup {
            Txt(
                "Your clock stays yours. Moods and themes never replace it.",
                11f, c.fg2, maxLines = 2
            )
        }
    }
}

/** A quiet stage for the chosen clock. The only animated surface on this page. */
@Composable
private fun ClockHero(style: String, time: String, is24: Boolean, now: LocalDateTime) {
    val c = LocalColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(c.tile)
            .border(0.8.dp, c.fg2.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
            .padding(horizontal = 22.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        ClockPreview(style, time, is24, now, hero = true)
    }
}

/** The dot matrix is the house style: full width, first, and never hidden in a grid. */
@Composable
private fun DotSignatureCard(on: Boolean, onClick: () -> Unit) {
    val c = LocalColors.current
    val preview = "9:41"
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.tile)
            .border(
                if (on) 1.4.dp else 0.8.dp,
                if (on) c.fg else c.fg2.copy(alpha = 0.18f),
                RoundedCornerShape(20.dp)
            )
            .tap(onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DotText(
            preview,
            Modifier.width(118.dp).aspectRatio(dotCols(preview) / 7f),
            c.fg, c.accent
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Txt("Dot matrix", 16f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            Txt("Drawn dot by dot. No font, no shortcut.", 11f, c.fg2, maxLines = 2)
        }
        if (on) {
            Spacer(Modifier.width(6.dp))
            AccentDot(c.fg, 8.dp)
        }
    }
}

@Composable
private fun ClockCard(
    style: String,
    on: Boolean,
    is24: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.tile)
            .border(
                if (on) 1.4.dp else 0.8.dp,
                if (on) c.fg else c.fg2.copy(alpha = 0.18f),
                RoundedCornerShape(18.dp)
            )
            .tap(onClick)
            .padding(14.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(62.dp), contentAlignment = Alignment.CenterStart) {
            ClockPreview(style, "9:41", is24, PREVIEW_NOW, hero = false)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(
                clockLabel(style), 12f,
                c.fg,
                if (on) FontWeight.SemiBold else FontWeight.Normal,
                Modifier.weight(1f)
            )
            if (on) AccentDot(c.fg, 6.dp)
        }
    }
}

private val PREVIEW_NOW: LocalDateTime = LocalDateTime.of(2026, 1, 1, 9, 41)

/** Human names for the two styles whose stored value is shorter than the idea. */
internal fun clockLabel(style: String): String = when (style) {
    "Dot" -> "Dot matrix"
    "Binary" -> "Binary dots"
    "Binary Digits" -> "Binary digits"
    else -> style
}

/**
 * Every style gets a real miniature, not a label with the same "9:41" text. The dot clock
 * keeps its drawn dots; the analog keeps its hands; the LCD keeps its segments.
 */
@Composable
private fun ClockPreview(
    style: String,
    time: String,
    is24: Boolean,
    now: LocalDateTime,
    hero: Boolean
) {
    val c = LocalColors.current
    val fg = c.fg
    val fg2 = c.fg2
    val accent = c.accent
    when (style) {
        "Dot" -> DotText(
            time,
            Modifier.fillMaxWidth(if (hero) 0.78f else 0.92f)
                .aspectRatio(dotCols(time) / 7f),
            fg, accent
        )
        "Digital" -> Txt(time, if (hero) 46f else 28f, fg, FontWeight.Light)
        "Words" -> Column {
            Txt(hourWordPreview(now.hour), if (hero) 26f else 16f, fg, FontWeight.Bold)
            Txt(minuteWordsPreview(now.minute), if (hero) 23f else 14f, fg, FontWeight.Light)
        }
        "Analog" -> MiniAnalog(now, if (hero) 78.dp else 46.dp, fg, accent)
        "Flip", "Split-Flap" -> MiniFlip(now, fg, accent, style == "Split-Flap", hero)
        "Retro Pixel" -> PreviewMono(time, if (hero) 36f else 22f, fg, FontWeight.Bold)
        "Thin" -> Txt(time, if (hero) 48f else 30f, fg, FontWeight.Thin)
        "Bold" -> Txt(time, if (hero) 44f else 28f, fg, FontWeight.Black)
        "Neon" -> PreviewNeon(time, if (hero) 40f else 24f, accent)
        "Binary" -> BinaryDotClock(now, is24, fg, fg2, accent, compact = !hero)
        "Binary Digits" -> BinaryDigitClock(
            if (is24) now.hour else ((now.hour + 11) % 12) + 1,
            now.minute,
            fg, fg2,
            compact = !hero
        )
        "Roman" -> Txt(
            "${romanPreview(now.hour)}:${romanPreview(now.minute)}",
            if (hero) 34f else 20f, fg, FontWeight.Light
        )
        "Gradient" -> PreviewGradient(time, if (hero) 40f else 24f, accent, fg)
        "Liquid Glass" -> LiquidGlassClock(time, fg, fg2, accent, compact = !hero)
        "Glyph Arc" -> Column {
            Txt(time, if (hero) 38f else 22f, fg, FontWeight.Light)
            Spacer(Modifier.height(if (hero) 8.dp else 4.dp))
            Canvas(
                Modifier.fillMaxWidth(if (hero) 0.8f else 0.9f)
                    .height(if (hero) 12.dp else 7.dp)
            ) {
                val y = size.height / 2f
                drawLine(fg2.copy(alpha = 0.22f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                drawLine(accent, Offset(0f, y), Offset(size.width * 0.68f, y), strokeWidth = 2.dp.toPx())
            }
        }
        "Micrographics" -> Column {
            Txt(time, if (hero) 34f else 20f, fg, FontWeight.Light)
            Spacer(Modifier.height(if (hero) 7.dp else 4.dp))
            Canvas(Modifier.fillMaxWidth(0.9f).height(if (hero) 10.dp else 6.dp)) {
                val n = 22
                repeat(n) { i ->
                    val x = size.width * i / (n - 1)
                    val h = if (i % 5 == 0) size.height else size.height * 0.45f
                    drawLine(
                        fg2.copy(alpha = 0.55f),
                        Offset(x, size.height - h), Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
        "Day Name" -> Column {
            Txt(
                now.format(DateTimeFormatter.ofPattern("EEE.", Locale.getDefault())).uppercase(),
                if (hero) 40f else 22f, fg, FontWeight.Light
            )
            Spacer(Modifier.height(if (hero) 4.dp else 2.dp))
            PreviewMono(time, if (hero) 16f else 10f, accent, FontWeight.Medium)
        }
        "Segment LCD" -> MiniSegment(now, is24, fg, accent, hero)
        else -> Txt(time, if (hero) 40f else 24f, fg, FontWeight.Medium)
    }
}

@Composable
private fun PreviewMono(text: String, size: Float, color: Color, weight: FontWeight) {
    BasicText(
        text,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = FontFamily.Monospace
        )
    )
}

@Composable
private fun PreviewNeon(text: String, size: Float, accent: Color) {
    BasicText(
        text,
        style = TextStyle(
            color = accent,
            fontSize = size.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(accent.copy(alpha = 0.85f), Offset(2f, 2f), 10f)
        )
    )
}

@Composable
private fun PreviewGradient(text: String, size: Float, accent: Color, fg: Color) {
    Box {
        BasicText(
            text,
            style = TextStyle(color = accent.copy(alpha = 0.55f), fontSize = size.sp, fontWeight = FontWeight.Bold)
        )
        BasicText(
            text,
            style = TextStyle(color = fg, fontSize = size.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.graphicsLayer { translationY = -2f * density }
        )
    }
}

@Composable
private fun MiniAnalog(now: LocalDateTime, size: Dp, fg: Color, accent: Color) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(fg.copy(alpha = 0.35f), r, center, style = Stroke(1.2.dp.toPx()))
        repeat(12) { i ->
            val a = Math.toRadians((i * 30 - 90).toDouble())
            val inner = r * 0.84f
            val outer = r * 0.94f
            drawLine(
                fg.copy(alpha = 0.5f),
                Offset(center.x + cos(a).toFloat() * inner, center.y + sin(a).toFloat() * inner),
                Offset(center.x + cos(a).toFloat() * outer, center.y + sin(a).toFloat() * outer),
                strokeWidth = 1.dp.toPx()
            )
        }
        val hourAngle = Math.toRadians(((now.hour % 12) * 30 + now.minute * 0.5 - 90).toDouble())
        val minuteAngle = Math.toRadians((now.minute * 6 - 90).toDouble())
        drawLine(
            fg, center,
            Offset(center.x + cos(hourAngle).toFloat() * r * 0.5f, center.y + sin(hourAngle).toFloat() * r * 0.5f),
            strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round
        )
        drawLine(
            accent, center,
            Offset(center.x + cos(minuteAngle).toFloat() * r * 0.72f, center.y + sin(minuteAngle).toFloat() * r * 0.72f),
            strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round
        )
        drawCircle(accent, r * 0.07f, center)
    }
}

@Composable
private fun MiniFlip(now: LocalDateTime, fg: Color, accent: Color, flap: Boolean, hero: Boolean) {
    val h = String.format("%02d", now.hour)
    val m = String.format("%02d", now.minute)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (hero) 6.dp else 3.dp)
    ) {
        FlipTile(h, fg, accent, flap, hero)
        Txt(":", if (hero) 24f else 14f, fg, FontWeight.Bold)
        FlipTile(m, fg, accent, flap, hero)
    }
}

@Composable
private fun FlipTile(text: String, fg: Color, accent: Color, flap: Boolean, hero: Boolean) {
    val c = LocalColors.current
    val shape = RoundedCornerShape(if (hero) 12.dp else 7.dp)
    Box(
        Modifier
            .size(if (hero) 58.dp else 34.dp, if (hero) 72.dp else 42.dp)
            .clip(shape)
            .background(if (c.dark) Color(0xFF1C1C1E) else Color.White)
            .border(0.8.dp, fg.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center
    ) {
        Txt(text, if (hero) 30f else 17f, fg, FontWeight.Medium)
        if (flap) {
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(accent.copy(alpha = 0.35f))
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun MiniSegment(now: LocalDateTime, is24: Boolean, fg: Color, accent: Color, hero: Boolean) {
    val h = if (is24) String.format("%02d", now.hour) else String.format("%02d", ((now.hour + 11) % 12) + 1)
    val m = String.format("%02d", now.minute)
    val digitW = if (hero) 26.dp else 15.dp
    val digitH = if (hero) 48.dp else 27.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (hero) 4.dp else 2.dp)
    ) {
        SegmentDigit(h[0], fg, digitW, digitH)
        SegmentDigit(h[1], fg, digitW, digitH)
        Canvas(Modifier.size(if (hero) 8.dp else 5.dp, digitH)) {
            drawCircle(accent, size.width * 0.35f, Offset(size.width / 2f, size.height * 0.34f))
            drawCircle(accent, size.width * 0.35f, Offset(size.width / 2f, size.height * 0.66f))
        }
        SegmentDigit(m[0], fg, digitW, digitH)
        SegmentDigit(m[1], fg, digitW, digitH)
    }
}

@Composable
private fun SegmentDigit(digit: Char, fg: Color, w: Dp, h: Dp) {
    val mask = when (digit) {
        '0' -> 0b1111110
        '1' -> 0b0110000
        '2' -> 0b1101101
        '3' -> 0b1111001
        '4' -> 0b0110011
        '5' -> 0b1011011
        '6' -> 0b1011111
        '7' -> 0b1110000
        '8' -> 0b1111111
        '9' -> 0b1111011
        else -> 0
    }
    Canvas(Modifier.size(w, h)) {
        val t = size.width * 0.18f
        val gap = 0.8.dp.toPx()
        val dim = fg.copy(alpha = 0.08f)
        fun col(bit: Int) = if ((mask and (1 shl bit)) != 0) fg else dim
        fun hbar(bit: Int, y: Float) = drawRoundRect(
            col(bit),
            topLeft = Offset(t + gap, y),
            size = Size((size.width - 2 * t - 2 * gap).coerceAtLeast(1f), t),
            cornerRadius = CornerRadius(t / 3f, t / 3f)
        )
        fun vbar(bit: Int, x: Float, y: Float) = drawRoundRect(
            col(bit),
            topLeft = Offset(x, y),
            size = Size(t, ((size.height - 3 * t) / 2f - 2 * gap).coerceAtLeast(1f)),
            cornerRadius = CornerRadius(t / 3f, t / 3f)
        )
        hbar(6, 0f)
        vbar(5, size.width - t, t + gap)
        vbar(4, size.width - t, (size.height + t) / 2f + gap)
        hbar(3, size.height - t)
        vbar(2, 0f, (size.height + t) / 2f + gap)
        vbar(1, 0f, t + gap)
        hbar(0, (size.height - t) / 2f)
    }
}

private val ONES_PREVIEW = listOf(
    "twelve", "one", "two", "three", "four", "five",
    "six", "seven", "eight", "nine", "ten", "eleven"
)
private val TEENS_PREVIEW = listOf(
    "ten", "eleven", "twelve", "thirteen", "fourteen",
    "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
)
private val UNITS_PREVIEW = listOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
private val TENS_PREVIEW = listOf("", "", "twenty", "thirty", "forty", "fifty")

private fun hourWordPreview(h: Int) = ONES_PREVIEW[h % 12]

private fun minuteWordsPreview(m: Int): String = when {
    m == 0 -> "o'clock"
    m < 10 -> "oh ${UNITS_PREVIEW[m]}"
    m < 20 -> TEENS_PREVIEW[m - 10]
    else -> TENS_PREVIEW[m / 10] + if (m % 10 != 0) "-${UNITS_PREVIEW[m % 10]}" else ""
}

private fun romanPreview(n: Int): String {
    if (n <= 0) return "—"
    val values = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var left = n
    val out = StringBuilder()
    for ((value, symbol) in values) {
        while (left >= value) {
            out.append(symbol)
            left -= value
        }
    }
    return out.toString()
}
