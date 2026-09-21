package com.jakob.oledoro.ui.ambient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BurnInShiftTest {

    @Test
    fun `calculatePixelShift respects maxShift boundary`() {
        val maxShift = 6f

        // Test over 360 simulated minute ticks
        for (step in 0L..360L) {
            val offset = calculatePixelShift(step, maxShift)

            assertTrue(
                "X offset ${offset.xDp} exceeded maxShift $maxShift at step $step",
                abs(offset.xDp) <= maxShift + 0.001f
            )
            assertTrue(
                "Y offset ${offset.yDp} exceeded maxShift $maxShift at step $step",
                abs(offset.yDp) <= maxShift + 0.001f
            )
        }
    }

    @Test
    fun `calculatePixelShift step zero produces expected starting point`() {
        val offset = calculatePixelShift(0L, 6f)
        assertEquals(0f, offset.xDp, 0.001f)
        assertEquals(6f, offset.yDp, 0.001f)
    }

    @Test
    fun `calculatePixelShift produces shifting offsets across successive minutes`() {
        val offset0 = calculatePixelShift(0L, 6f)
        val offset1 = calculatePixelShift(1L, 6f)
        val offset2 = calculatePixelShift(2L, 6f)

        assertTrue(offset0.xDp != offset1.xDp || offset0.yDp != offset1.yDp)
        assertTrue(offset1.xDp != offset2.xDp || offset1.yDp != offset2.yDp)
    }
}
