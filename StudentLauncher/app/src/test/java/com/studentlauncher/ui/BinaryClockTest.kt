package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BinaryClockTest {

    @Test
    fun groupsAreFourBitsPerDecimalDigit() {
        assertEquals(listOf("0000 1001", "0100 0001"), binaryDigitGroups(9, 41))
    }

    @Test
    fun leadingZeroesAreKept() {
        assertEquals(listOf("0001 0010", "0000 0101"), binaryDigitGroups(12, 5))
    }

    @Test
    fun midnightAndLastMinute() {
        assertEquals(listOf("0000 0000", "0000 0000"), binaryDigitGroups(0, 0))
        assertEquals(listOf("0010 0011", "0101 1001"), binaryDigitGroups(23, 59))
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        assertEquals(listOf("0010 0011", "0101 1001"), binaryDigitGroups(99, 99))
        assertEquals(listOf("0000 0000", "0000 0000"), binaryDigitGroups(-1, -1))
    }
}
