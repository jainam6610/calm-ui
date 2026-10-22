package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGridTest {

    @Test
    fun limitSnapsUpToWholeRows() {
        assertEquals(8, snapHomeLimit(6, 4))
        assertEquals(6, snapHomeLimit(6, 3))
        assertEquals(5, snapHomeLimit(4, 5))
        assertEquals(10, snapHomeLimit(8, 5))
        assertEquals(12, snapHomeLimit(12, 4))
    }

    @Test
    fun limitNeverExceedsThreeRowsOrSixteenApps() {
        assertEquals(12, snapHomeLimit(99, 4))
        assertEquals(15, snapHomeLimit(99, 5))
        assertEquals(9, snapHomeLimit(99, 3))
        assertEquals(15, snapHomeLimit(99, 6))
    }

    @Test
    fun optionsAreMultiplesOfTheColumnCount() {
        assertEquals(listOf(3, 6, 9), homeLimitOptions(3))
        assertEquals(listOf(4, 8, 12), homeLimitOptions(4))
        assertEquals(listOf(5, 10, 15), homeLimitOptions(5))
    }
}
