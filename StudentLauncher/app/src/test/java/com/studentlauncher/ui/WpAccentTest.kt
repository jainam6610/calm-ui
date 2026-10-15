package com.studentlauncher.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class WpAccentTest {

    @Test
    fun wallpapersContainsBuiltinPresets() {
        assertTrue(Wallpapers.presets.contains("dune"))
        assertTrue(Wallpapers.presets.contains("mist"))
        assertTrue(Wallpapers.presets.contains("meadow"))
        assertTrue(Wallpapers.presets.contains("plain"))
    }
}
