package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalScrollIndexTest {

    // groups: A(2 apps) -> header 0, items 1..2; B(3) -> header 3, items 4..6; D(1) -> header 7, item 8
    private val sizes = listOf(2, 3, 1)
    private val letters = listOf('A', 'B', 'D')

    @Test fun headerIndexOfEachGroup() {
        assertEquals(0, groupScrollIndex(sizes, 0))
        assertEquals(3, groupScrollIndex(sizes, 1))
        assertEquals(7, groupScrollIndex(sizes, 2))
    }

    @Test fun outOfRangeGroupIsMinusOne() {
        assertEquals(-1, groupScrollIndex(sizes, -1))
        assertEquals(-1, groupScrollIndex(sizes, 3))
        assertEquals(-1, groupScrollIndex(emptyList(), 0))
    }

    @Test fun exactLetterWins() {
        assertEquals(1, targetGroupIndex(letters, 'B'))
        assertEquals(0, targetGroupIndex(letters, 'A'))
    }

    @Test fun emptyLetterJumpsToNearestAbove() {
        assertEquals(2, targetGroupIndex(letters, 'C')) // no C -> D group (next section above)
        assertEquals(2, targetGroupIndex(letters, 'E')) // nothing above E -> fall down to D
    }

    @Test fun letterPastEndFallsBackDown() {
        assertEquals(2, targetGroupIndex(letters, 'Z'))
    }

    @Test fun symbolGroupIsLastResort() {
        assertEquals(1, targetGroupIndex(listOf('#', 'M'), 'A')) // skips '#', lands on M
        assertEquals(0, targetGroupIndex(listOf('#'), 'M'))      // only '#' left -> show it
    }

    @Test fun emptyListIsMinusOne() {
        assertEquals(-1, targetGroupIndex(emptyList(), 'A'))
    }
}
