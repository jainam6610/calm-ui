package com.studentlauncher.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.IconPack
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.SettingsPage
import com.studentlauncher.data.ThemeMode
import com.studentlauncher.data.homeLimitOptions

/** Shared scaffold for every Personalization sub-page: title, optional back, scrolling content. */
@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalColors.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("PERSONALIZATION", 11f, c.fg2, FontWeight.Bold)
                Txt(title, 30f, c.fg, FontWeight.SemiBold)
            }
            if (onBack != null) PillButton("Back", onBack)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun HubRow(title: String, summary: String, onClick: () -> Unit) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.tile)
            .tap(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Txt(title, 15f, c.fg, FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Txt(summary, 12f, c.fg2)
        }
        Txt("\u203A", 22f, c.fg2)
    }
}

@Composable
internal fun PersonalizationHub(vm: LauncherViewModel) {
    val iconsSummary = vm.iconPack.name
    val themeSummary = when (vm.themeMode) {
        ThemeMode.Dark -> "Dark"
        ThemeMode.Light -> "Light"
        ThemeMode.System -> "Auto"
    }
    val homeSummary = if (vm.layout == HomeLayout.Grid) "Grid layout" else "List layout"
    val wallpaperSummary = when (vm.wallpaper) {
        "photo" -> "Custom photo"
        "shuffle" -> "Shuffle ${vm.shuffleCount}/5"
        else -> vm.wallpaper.replaceFirstChar { it.uppercase() }
    }
    SettingsScaffold("Personalization", onBack = { vm.settingsPage = SettingsPage.Root }) {
        HubRow("Icons", "$iconsSummary \u00B7 shape", { vm.settingsPage = SettingsPage.Icons })
        Spacer(Modifier.height(10.dp))
        HubRow("Theme", "$themeSummary \u00B7 accent", { vm.settingsPage = SettingsPage.Theme })
        Spacer(Modifier.height(10.dp))
        HubRow("Wallpaper", wallpaperSummary, { vm.settingsPage = SettingsPage.Wallpaper })
        Spacer(Modifier.height(10.dp))
        HubRow("Home layout", homeSummary, { vm.settingsPage = SettingsPage.Home })
        Spacer(Modifier.height(10.dp))
        HubRow("Clock", vm.clockStyle, { vm.settingsPage = SettingsPage.Clock })
    }
}

@Composable
internal fun IconsPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Icons", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingSection("Icon pack", vm.iconPack.name) {
            Txt("One glyph, several styles. Apps without a themed layer keep their original icon.", 12f, c.fg2, maxLines = 3)
            Spacer(Modifier.height(10.dp))
            ICON_PACKS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { p ->
                        ChoiceChip(
                            p.name, p == vm.iconPack, { vm.applyIconPack(p) }, Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        SettingSection("Shape") {
            Segmented(
                listOf(IconShape.None to "None", IconShape.Circle to "Circle", IconShape.Squircle to "Squircle"),
                vm.iconShape,
                vm::applyIconShape
            )
        }
    }
}

private val ICON_PACKS = listOf(
    IconPack.Original, IconPack.Mono, IconPack.Hairline, IconPack.Bold, IconPack.Dot, IconPack.Bubble, IconPack.Tile
)

@Composable
internal fun ThemePage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Theme", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingSection("Appearance") {
            Segmented(
                listOf(ThemeMode.Dark to "Dark", ThemeMode.Light to "Light", ThemeMode.System to "Auto"),
                vm.themeMode,
                vm::applyTheme
            )
            Spacer(Modifier.height(10.dp))
            SwitchSetting("Wallpaper accent", "Picks accent colour from your wallpaper", vm.wpAccent, vm::applyWpAccent)
        }
        SettingSection("Accent") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("red", "blue", "green", "purple", "orange", "teal", "pink").chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { id ->
                            val col = accentColor(id)
                            val on = vm.accentId == id
                            Box(Modifier.size(48.dp).tap { vm.applyAccent(id) }, contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier.size(32.dp).clip(CircleShape).background(col)
                                        .border(if (on) 2.dp else 0.8.dp, if (on) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WallpaperPage(vm: LauncherViewModel, pickPhoto: () -> Unit, pickShufflePhotos: () -> Unit) {
    val c = LocalColors.current
    val summary = when (vm.wallpaper) {
        "photo" -> "Custom photo"
        "shuffle" -> "Shuffle ${vm.shuffleCount}/5"
        else -> vm.wallpaper
    }
    SettingsScaffold("Wallpaper", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingSection("Wallpaper", summary) {
            val previews = remember(c.dark) {
                Wallpapers.presets.map { it to Wallpapers.preset(it, c.dark, 96, 96).asImageBitmap() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                previews.forEach { (id, bmp) ->
                    val on = vm.wallpaper == id
                    Box(Modifier.size(48.dp).tap { vm.applyWallpaper(id) }, contentAlignment = Alignment.Center) {
                        Image(
                            bmp, id, Modifier.size(34.dp).clip(CircleShape)
                                .border(if (on) 2.dp else 0.8.dp, if (on) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                val photo = vm.wallpaper == "photo"
                Box(Modifier.size(48.dp).tap { pickPhoto() }, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.track)
                            .border(if (photo) 2.dp else 0.8.dp, if (photo) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Txt("+", 18f, c.fg) }
                }
                val shuffleOn = vm.wallpaper == "shuffle"
                Box(Modifier.size(48.dp).tap { vm.applyWallpaper("shuffle") }, contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(c.accent)
                            .border(if (shuffleOn) 2.dp else 0.8.dp, if (shuffleOn) c.fg else c.fg2.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Txt("S", 13f, Color.White, FontWeight.Bold) }
                }
            }
            if (vm.wallpaper == "shuffle") {
                Spacer(Modifier.height(10.dp))
                Txt("Shuffle photos (one per day)", 12f, c.fg2)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until Wallpapers.MAX_SHUFFLE).forEach { i ->
                        val filled = i < vm.shuffleCount
                        Box(
                            Modifier.size(48.dp).tap {
                                if (filled) vm.removeShuffleSlot(i)
                                else pickShufflePhotos()
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(if (filled) c.track else c.track.copy(alpha = 0.4f))
                                    .border(if (i == vm.shuffleSlot && filled) 2.dp else 0.8.dp, if (i == vm.shuffleSlot && filled) c.accent else c.fg2.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Txt(if (filled) "\u00D7" else "+", 16f, if (filled) c.fg else c.fg2)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                PillButton("Pick photos (up to 5)", pickShufflePhotos)
                Spacer(Modifier.height(6.dp))
                Txt("Tap a filled circle to remove that photo.", 11f, c.fg2, maxLines = 2)
            }
            Spacer(Modifier.height(10.dp))
            PillButton("Adjust wallpaper", { vm.wallpaperEditor = true })
            Spacer(Modifier.height(8.dp))
            PillButton("Choose wallpaper", pickPhoto)
        }
    }
}

@Composable
internal fun HomeSettingsPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Home layout", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingSection("Layout") {
            Segmented(listOf(HomeLayout.List to "List", HomeLayout.Grid to "Grid"), vm.layout, vm::applyLayout)
            Spacer(Modifier.height(10.dp))
            SwitchSetting("Show app names", "Display labels under app icons", vm.showNames, vm::applyShowNames)
        }
        SettingSection("Home apps", "${vm.pinned.size}/${vm.homeAppLimit}") {
            Txt("Grid columns", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                listOf(3 to "3", 4 to "4", 5 to "5"),
                vm.homeColumns,
                vm::applyHomeColumns
            )
            Spacer(Modifier.height(10.dp))
            Txt("Apps on home", 12f, c.fg2)
            Spacer(Modifier.height(6.dp))
            Segmented(
                homeLimitOptions(vm.homeColumns).map { it to "$it" },
                vm.homeAppLimit,
                vm::applyHomeAppLimit
            )
            Spacer(Modifier.height(10.dp))
            PillButton("Pick home apps (${vm.pinned.size}/${vm.homeAppLimit})", { vm.pickHomeApps = true })
        }
        SettingSection("Dock") {
            Segmented(listOf(DockStyle.Bottom to "Bottom", DockStyle.Rail to "Rail", DockStyle.Off to "Off"), vm.dockStyle, vm::applyDockStyle)
        }
        SettingSection("Widgets") {
            PillButton("Add or arrange widgets", { vm.widgetCenter = true })
        }
    }
}

@Composable
internal fun ClockPage(vm: LauncherViewModel) {
    val c = LocalColors.current
    SettingsScaffold("Clock", onBack = { vm.settingsPage = SettingsPage.Personalization }) {
        SettingSection("Clock style", vm.clockStyle) {
            Txt("Applies to every clock widget.", 12f, c.fg2)
            Spacer(Modifier.height(10.dp))
            CLOCK_STYLES.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { s ->
                        ChoiceChip(
                            s, s == vm.clockStyle, { vm.applyClockStyle(s) }, Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
