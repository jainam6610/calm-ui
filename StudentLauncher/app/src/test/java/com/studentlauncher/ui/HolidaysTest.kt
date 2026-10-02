package com.studentlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidaysTest {

    @Test fun upcomingIsSortedAndLimited() {
        val list = upcomingHolidays(LocalDate.of(2026, 6, 1), 3)
        assertEquals(3, list.size)
        assertEquals("Halloween", list[0].first)
        assertEquals(152, list[0].second) // 2026-06-01 -> 2026-10-31
        assertTrue(list.map { it.second } == list.map { it.second }.sorted())
    }

    @Test fun pastDatesRollToNextYear() {
        val list = upcomingHolidays(LocalDate.of(2026, 12, 30), 3)
        assertEquals("New Year", list[0].first)
        assertEquals(2, list[0].second) // 2026-12-30 -> 2027-01-01
    }

    @Test fun holidayOnTodayIsZeroDays() {
        val list = upcomingHolidays(LocalDate.of(2026, 12, 25), 3)
        assertEquals("Christmas", list[0].first)
        assertEquals(0, list[0].second)
    }
}
