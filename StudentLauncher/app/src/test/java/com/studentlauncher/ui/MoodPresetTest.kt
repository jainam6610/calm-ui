package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodPresetTest {

    private val accents = setOf("red", "blue", "green", "purple", "orange", "teal", "pink", "lime")
    private val wallpapers = setOf("dune", "mist", "meadow", "plain")

    @Test
    fun everyMoodHasACompletePreset() {
        MOODS.forEach { mood ->
            val p = moodPreset(mood)
            assertTrue(mood.name, p.accentId in accents)
            assertTrue(mood.name, p.wallpaper in wallpapers)
            assertTrue(mood.name, p.motion in 0f..1f)
            assertTrue(mood.name, p.homeColumns in 3..5)
            assertTrue(mood.name, p.glyphDotScale in 0.5f..1.4f)
            assertTrue(mood.name, GlyphModes.meta(p.glyphAmbient) != null)
            assertTrue(mood.name, p.uiFont.isNotBlank())
        }
    }

    @Test
    fun everyMoodIsDistinct() {
        val looks = MOODS.map { moodPreset(it) }
        assertEquals(MOODS.size, looks.toSet().size)
    }

    @Test
    fun moodsNeverPointAtAPhotoOrShuffleWallpaper() {
        // A mood must not silently replace the user's own photo or shuffle slots.
        MOODS.forEach { mood ->
            assertTrue(mood.name, moodPreset(mood).wallpaper in wallpapers)
        }
    }

    @Test
    fun labelsAndTaglinesArePresent() {
        MOODS.forEach { mood ->
            assertTrue(mood.label.isNotBlank())
            assertTrue(mood.tagline.isNotBlank())
        }
    }
}
