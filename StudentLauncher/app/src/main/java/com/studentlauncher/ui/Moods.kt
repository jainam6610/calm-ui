package com.studentlauncher.ui

import com.studentlauncher.data.DockStyle
import com.studentlauncher.data.HomeLayout
import com.studentlauncher.data.IconPack
import com.studentlauncher.data.IconShape
import com.studentlauncher.data.ThemeMode

/**
 * One-tap looks. A mood only touches appearance settings: apps, tasks, widgets, photos
 * and permissions are never rewritten by [com.studentlauncher.LauncherViewModel.applyMood].
 */
enum class LauncherMood(val label: String, val tagline: String) {
    Quiet("Quiet", "Muted, still and out of the way"),
    Focus("Focus", "A working surface for the next task"),
    Studio("Studio", "Warmer type, colour and movement"),
    Neon("Neon", "Dark glass, sharp dots, a little speed"),
    Ink("Ink", "Black canvas, acid light, dot numerals")
}

data class MoodPreset(
    val theme: ThemeMode,
    val accentId: String,
    val iconPack: IconPack,
    val iconShape: IconShape,
    val wallpaper: String,
    val layout: HomeLayout,
    val dockStyle: DockStyle,
    val showNames: Boolean,
    val homeColumns: Int,
    val motion: Float,
    val compactDensity: Boolean,
    val uiFont: String,
    val glyphAmbient: String,
    val glyphDotScale: Float,
    val glyphGlow: Boolean
)

val MOODS: List<LauncherMood> = LauncherMood.entries.toList()

fun moodPreset(mood: LauncherMood): MoodPreset = when (mood) {
    LauncherMood.Quiet -> MoodPreset(
        theme = ThemeMode.System,
        accentId = "red",
        iconPack = IconPack.Mono,
        iconShape = IconShape.None,
        wallpaper = "plain",
        layout = HomeLayout.List,
        dockStyle = DockStyle.Off,
        showNames = false,
        homeColumns = 4,
        motion = 0.35f,
        compactDensity = false,
        uiFont = "serif",
        glyphAmbient = GlyphModes.QUIET,
        glyphDotScale = 0.8f,
        glyphGlow = false
    )
    LauncherMood.Focus -> MoodPreset(
        theme = ThemeMode.Light,
        accentId = "blue",
        iconPack = IconPack.Hairline,
        iconShape = IconShape.Squircle,
        wallpaper = "mist",
        layout = HomeLayout.Grid,
        dockStyle = DockStyle.Bottom,
        showNames = true,
        homeColumns = 4,
        motion = 0.75f,
        compactDensity = false,
        uiFont = "geist",
        glyphAmbient = GlyphModes.FOCUS,
        glyphDotScale = 0.95f,
        glyphGlow = true
    )
    LauncherMood.Studio -> MoodPreset(
        theme = ThemeMode.System,
        accentId = "purple",
        iconPack = IconPack.Bubble,
        iconShape = IconShape.Circle,
        wallpaper = "meadow",
        layout = HomeLayout.List,
        dockStyle = DockStyle.Bottom,
        showNames = true,
        homeColumns = 4,
        motion = 1f,
        compactDensity = false,
        uiFont = "poppins",
        glyphAmbient = GlyphModes.SPIRAL,
        glyphDotScale = 1f,
        glyphGlow = true
    )
    LauncherMood.Neon -> MoodPreset(
        theme = ThemeMode.Dark,
        accentId = "teal",
        iconPack = IconPack.Dot,
        iconShape = IconShape.Squircle,
        wallpaper = "dune",
        layout = HomeLayout.Grid,
        dockStyle = DockStyle.Rail,
        showNames = true,
        homeColumns = 4,
        motion = 1f,
        compactDensity = true,
        uiFont = "mono",
        glyphAmbient = GlyphModes.ORBIT,
        glyphDotScale = 1.1f,
        glyphGlow = true
    )
    LauncherMood.Ink -> MoodPreset(
        theme = ThemeMode.Dark,
        accentId = "lime",
        iconPack = IconPack.Dot,
        iconShape = IconShape.None,
        wallpaper = "plain",
        layout = HomeLayout.List,
        dockStyle = DockStyle.Bottom,
        showNames = true,
        homeColumns = 4,
        motion = 0.55f,
        compactDensity = false,
        uiFont = "geist",
        glyphAmbient = GlyphModes.CLOCK,
        glyphDotScale = 1f,
        glyphGlow = true
    )
}
