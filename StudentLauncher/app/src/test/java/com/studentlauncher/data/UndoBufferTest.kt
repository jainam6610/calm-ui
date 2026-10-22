package com.studentlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UndoBufferTest {

    @Test
    fun undoReturnsTheItemAndItsOriginalIndex() {
        val b = UndoBuffer<String>()
        b.stage("clock", 3, "home", 1_000L)
        val e = b.undo(2_000L)!!
        assertEquals("clock", e.item)
        assertEquals(3, e.index)
        assertEquals("home", e.listKey)
        assertNull(b.pending)
        assertNull(b.undo(2_001L))
    }

    @Test
    fun expiredEntryCannotBeUndone() {
        val b = UndoBuffer<String>()
        b.stage("clock", 0, "home", 1_000L)
        assertNull(b.undo(6_001L))
        assertNull(b.pending)
    }

    @Test
    fun expiryIsOnlyTakenAfterTheWindow() {
        val b = UndoBuffer<String>()
        b.stage("clock", 0, "home", 1_000L)
        assertNull(b.takeExpired(6_000L))
        assertEquals("clock", b.takeExpired(6_001L)!!.item)
        assertNull(b.pending)
    }

    @Test
    fun restoredEntryKeepsItsOriginalDeadline() {
        val b = UndoBuffer<String>()
        b.restore(UndoEntry("timer", 2, "board", 7_000L))
        assertEquals("timer", b.undo(6_000L)!!.item)
    }

    @Test
    fun clearDropsThePendingEntry() {
        val b = UndoBuffer<String>()
        b.stage("clock", 0, "home", 1_000L)
        b.clear()
        assertNull(b.undo(2_000L))
    }
}
