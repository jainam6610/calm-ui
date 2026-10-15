package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconStyleTest {

    @Test
    fun iconStyleHasFourEntries() {
        val styles = IconStyle.values().map { it.name }
        assertEquals(4, styles.size)
        assertTrue(styles.contains("Mono"))
        assertTrue(styles.contains("Original"))
        assertTrue(styles.contains("MinimalBubble"))
        assertTrue(styles.contains("MinimalOutline"))
    }
}
