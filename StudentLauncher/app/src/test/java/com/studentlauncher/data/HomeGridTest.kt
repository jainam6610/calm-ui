package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridTest {

    @Test
    fun optionsOfferWholeRowsForTheColumnCount() {
        assertEquals(listOf(3, 6, 9), homeLimitOptions(3))
        assertEquals(listOf(4, 8, 12), homeLimitOptions(4))
        assertEquals(listOf(5, 10, 15), homeLimitOptions(5))
    }

    /**
     * Regression: changing columns used to rewrite the app limit to the next whole row, which
     * looked like a second setting resetting on its own. The current limit must stay offered
     * (and therefore stay selected) whatever the column count is.
     */
    @Test
    fun currentLimitSurvivesAColumnChange() {
        for (columns in 3..5) {
            val options = homeLimitOptions(columns, current = 8)
            assertTrue("8 missing for $columns columns: $options", 8 in options)
            assertEquals(options.sorted(), options)
            assertEquals(options.distinct(), options)
        }
    }

    @Test
    fun optionsStayInsideTheSupportedRange() {
        assertTrue(homeLimitOptions(3, current = 16).all { it in 1..16 })
        assertTrue(homeLimitOptions(5, current = 99).all { it in 1..16 })
        assertTrue(homeLimitOptions(4, current = 0).none { it == 0 })
    }
}
