package com.calm.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class CalmConfigTest {

    @Test
    fun missingCountsUseTheDefault() {
        assertEquals(CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount(null))
        assertEquals(CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount(""))
    }

    @Test
    fun nonIntegerCountsUseTheDefault() {
        for (raw in listOf(" ", "nine", "fifteen", "9.5", "15.0", "15x", " 15 ")) {
            assertEquals("raw=\"$raw\"", CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount(raw))
        }
    }

    @Test
    fun unsupportedCountsUseTheDefault() {
        for (count in listOf(Int.MIN_VALUE, -15, -1, 0, 1, 8, 10, 14, 16, 100, Int.MAX_VALUE)) {
            assertEquals("count=$count", CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount(count.toString()))
        }
    }

    @Test
    fun overflowingCountsUseTheDefault() {
        for (raw in listOf("2147483648", "-2147483649", "4294967311", "99999999999999999999")) {
            assertEquals("raw=$raw", CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount(raw))
        }
    }

    @Test
    fun nineKeepsTheDefaultCount() {
        assertEquals(CalmConfig.COUNT_DEFAULT, CalmConfig.normalizeCount("9"))
    }

    @Test
    fun fifteenSelectsTheLargeCount() {
        assertEquals(CalmConfig.COUNT_LARGE, CalmConfig.normalizeCount("15"))
    }

    @Test
    fun defaultSlotsAreSevenAppsThenPhoneAndCameraWithoutExtras() {
        val slots = CalmConfig.slots(CalmConfig.COUNT_DEFAULT)

        assertEquals(CalmConfig.COUNT_DEFAULT, slots.size)
        assertEquals(
            listOf("messages", "whatsapp", "maps", "pay", "clock", "browser", "music", "phone", "camera"),
            slots.map { it.id }
        )
    }

    @Test
    fun largeSlotsAddSixExtrasBeforePhoneAndCamera() {
        val slots = CalmConfig.slots(CalmConfig.COUNT_LARGE)

        assertEquals(CalmConfig.COUNT_LARGE, slots.size)
        assertEquals(
            listOf(
                "messages", "whatsapp", "maps", "pay", "clock", "browser", "music",
                "extra1", "extra2", "extra3", "extra4", "extra5", "extra6", "phone", "camera"
            ),
            slots.map { it.id }
        )
    }

    @Test
    fun onlyTheSixExtraSlotsAreExtra() {
        val extraIds = setOf("extra1", "extra2", "extra3", "extra4", "extra5", "extra6")

        for (slot in CalmConfig.slots(CalmConfig.COUNT_LARGE)) {
            assertEquals("isExtra(${slot.id})", slot.id in extraIds, CalmConfig.isExtra(slot))
        }
    }

    @Test
    fun onlyBrowserPhoneAndCameraAreFixed() {
        val fixedIds = setOf("browser", "phone", "camera")

        for (slot in CalmConfig.slots(CalmConfig.COUNT_LARGE)) {
            assertEquals("isFixed(${slot.id})", slot.id in fixedIds, CalmConfig.isFixed(slot))
        }
    }

    @Test
    fun slotIdsAreUniqueForBothGridSizes() {
        for (count in listOf(CalmConfig.COUNT_DEFAULT, CalmConfig.COUNT_LARGE)) {
            val ids = CalmConfig.slots(count).map { it.id }

            assertEquals("Duplicate slot ids for count=$count: $ids", ids.size, ids.toSet().size)
        }
    }
}
