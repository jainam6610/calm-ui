package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPackTest {

    @Test
    fun iconPackHasSevenEntries() {
        val packs = IconPack.values().map { it.name }
        assertEquals(7, packs.size)
        assertTrue(packs.containsAll(listOf("Original", "Mono", "Hairline", "Bold", "Dot", "Bubble", "Tile")))
    }

    @Test
    fun storedValuesMigrateFromTheOldStyleNames() {
        assertEquals(IconPack.Original, iconPackFromStored(null))
        assertEquals(IconPack.Mono, iconPackFromStored("Mono"))
        assertEquals(IconPack.Original, iconPackFromStored("Original"))
        assertEquals(IconPack.Bubble, iconPackFromStored("MinimalBubble"))
        assertEquals(IconPack.Tile, iconPackFromStored("MinimalOutline"))
    }

    @Test
    fun unknownStoredValuesFallBackToOriginal() {
        assertEquals(IconPack.Original, iconPackFromStored("Nonsense"))
    }

    @Test
    fun newPackNamesRoundTrip() {
        IconPack.values().forEach { assertEquals(it, iconPackFromStored(it.name)) }
    }
}
