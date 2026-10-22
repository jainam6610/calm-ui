package com.studentlauncher.ui

import com.studentlauncher.data.WidgetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardRowNeighborsTest {

    private fun w(id: String, half: Boolean, height: Int = 2) =
        WidgetItem(
            id = id,
            type = "x",
            colSpan = if (half) 2 else 4,
            rowSpan = height
        )

    @Test fun pairsConsecutiveHalves() {
        val items = listOf(w("a", true), w("b", true), w("c", false), w("d", true), w("e", true))
        assertEquals(listOf(listOf(0, 1), listOf(2), listOf(3, 4)), boardRowIndexRows(items))
    }

    @Test fun dragSkipsHorizontalPartner() {
        // rows: [a,b], [c,d]
        val items = listOf(w("a", true), w("b", true), w("c", true), w("d", true))
        assertEquals(null to 2, boardVerticalNeighbors(items, 0)) // a: down is c (row below), not b
        assertEquals(null to 3, boardVerticalNeighbors(items, 1)) // b: down is d
        assertEquals(0 to null, boardVerticalNeighbors(items, 2)) // c: up is a
        assertEquals(1 to null, boardVerticalNeighbors(items, 3))
    }

    @Test fun fullWidthNeighborsAreImmediate() {
        val items = listOf(w("a", false), w("b", false))
        assertEquals(null to 1, boardVerticalNeighbors(items, 0))
        assertEquals(0 to null, boardVerticalNeighbors(items, 1))
    }

    @Test fun outOfRangeIsSafe() {
        assertNull(boardVerticalNeighbors(emptyList(), 0).first)
        assertNull(boardVerticalNeighbors(listOf(w("a", false)), 5).second)
    }

    @Test fun rowHeightIsMaxOfPair() {
        val items = listOf(w("a", true), w("b", true))
        val heights = mapOf("a" to 80f, "b" to 200f)
        assertEquals(200f, boardRowHeight(items, heights, 0))
        assertEquals(200f, boardRowHeight(items, heights, 1))
        assertNull(boardRowHeight(items, heights, 99))
    }

    @Test fun swapUsesRowAwareDeltas() {
        // from idx 1 (second of pair) jump to row below at idx 3 → delta +2
        assertEquals(2 to -20f, boardSwap(offset = 60f, heightUp = null, heightDown = 80f, deltaUp = -2, deltaDown = 2))
        assertEquals(-2 to 20f, boardSwap(offset = -60f, heightUp = 80f, heightDown = null, deltaUp = -2, deltaDown = 2))
    }
}
