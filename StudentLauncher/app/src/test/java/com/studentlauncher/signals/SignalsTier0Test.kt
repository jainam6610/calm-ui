package com.studentlauncher.signals

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalsTier0Test {

    @Test
    fun screenUpIsFaceUp() {
        assertEquals(Flip.FaceUp, flipFrom(9.8f))
        assertEquals(Flip.FaceUp, flipFrom(7f))
    }

    @Test
    fun screenDownIsFaceDown() {
        assertEquals(Flip.FaceDown, flipFrom(-9.8f))
        assertEquals(Flip.FaceDown, flipFrom(-7f))
    }

    @Test
    fun uprightOrInHandIsUnknown() {
        assertEquals(Flip.Unknown, flipFrom(0f))
        assertEquals(Flip.Unknown, flipFrom(6.9f))
        assertEquals(Flip.Unknown, flipFrom(-6.9f))
        // Held vertically: gravity is on x/y, so z is near zero.
        assertEquals(Flip.Unknown, flipFrom(0.4f))
    }

    @Test
    fun thresholdIsConfigurable() {
        assertEquals(Flip.Unknown, flipFrom(5f, threshold = 6f))
        assertEquals(Flip.FaceUp, flipFrom(5f, threshold = 4f))
    }
}
