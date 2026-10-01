package com.lolerco.oledoro.ui.components

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests enforcing that all slider scales in Settings hit 100% of their discrete numbers
 * without skipping any numbers (e.g. preventing the bug where Long Break skipped 15 and jumped from 14 to 16
 * due to floating-point truncation with toInt()).
 */
class SettingsScaleTest {

    /**
     * Exact Compose lerp implementation from androidx.compose.ui.util.MathHelpersKt
     */
    private fun composeLerp(start: Float, stop: Float, fraction: Float): Float {
        return (1f - fraction) * start + fraction * stop
    }

    @Test
    fun `focus scale (1 to 90 min) hits every single integer from 1 to 90 without skipping`() {
        val start = 1f
        val stop = 90f
        val steps = 88 // 88 interior steps = 89 intervals = 90 discrete values

        val collectedInts = mutableListOf<Int>()
        val totalSteps = steps + 2

        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            val mappedInt = lerpValue.roundToInt()
            collectedInts.add(mappedInt)

            val expected = i + 1
            assertEquals("Step index $i should map to $expected", expected, mappedInt)
        }

        val expectedSet = (1..90).toSet()
        assertEquals("Focus scale must contain exactly 90 values", 90, collectedInts.size)
        assertEquals("Focus scale must contain all integers 1..90", expectedSet, collectedInts.toSet())

        // Contrast: verify that old toInt() failed by skipping numbers
        val failedWithToInt = mutableSetOf<Int>()
        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            failedWithToInt.add(lerpValue.toInt())
        }
        assertNotEquals("toInt() had precision gaps", expectedSet, failedWithToInt)
    }

    @Test
    fun `short break scale (1 to 30 min) hits every single integer from 1 to 30 without skipping`() {
        val start = 1f
        val stop = 30f
        val steps = 28 // 28 interior steps = 29 intervals = 30 discrete values

        val collectedInts = mutableListOf<Int>()
        val totalSteps = steps + 2

        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            val mappedInt = lerpValue.roundToInt()
            collectedInts.add(mappedInt)

            val expected = i + 1
            assertEquals("Step index $i should map to $expected", expected, mappedInt)
        }

        val expectedSet = (1..30).toSet()
        assertEquals("Short break scale must contain exactly 30 values", 30, collectedInts.size)
        assertEquals("Short break scale must contain all integers 1..30", expectedSet, collectedInts.toSet())
    }

    @Test
    fun `long break scale (1 to 60 min) hits every single integer from 1 to 60 including 15 without skipping`() {
        val start = 1f
        val stop = 60f
        val steps = 58 // 58 interior steps = 59 intervals = 60 discrete values

        val collectedInts = mutableListOf<Int>()
        val totalSteps = steps + 2

        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            val mappedInt = lerpValue.roundToInt()
            collectedInts.add(mappedInt)

            val expected = i + 1
            assertEquals("Step index $i should map to $expected", expected, mappedInt)
        }

        // Specifically assert that 14, 15, and 16 are all hit in sequence
        assertEquals(14, collectedInts[13])
        assertEquals(15, collectedInts[14])
        assertEquals(16, collectedInts[15])

        val expectedSet = (1..60).toSet()
        assertEquals("Long break scale must contain exactly 60 values", 60, collectedInts.size)
        assertEquals("Long break scale must contain all integers 1..60", expectedSet, collectedInts.toSet())

        // Confirm that the bug where toInt() jumped from 14 to 16 is proven
        val fraction15 = 14.toFloat() / 59f
        val lerp15 = composeLerp(start, stop, fraction15)
        // With IEEE-754 single precision float, lerp15 is 14.99999905f
        // toInt() truncated this to 14, causing 15 to be skipped
        assertTrue("lerp value for 15 has float precision near 15", lerp15 > 14.99f && lerp15 <= 15.01f)
        assertEquals("roundToInt correctly resolves to 15", 15, lerp15.roundToInt())
    }

    @Test
    fun `long break interval scale (1 to 10 rounds) hits every single integer from 1 to 10 without skipping`() {
        val start = 1f
        val stop = 10f
        val steps = 8 // 8 interior steps = 9 intervals = 10 discrete values

        val collectedInts = mutableListOf<Int>()
        val totalSteps = steps + 2

        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            val mappedInt = lerpValue.roundToInt()
            collectedInts.add(mappedInt)

            val expected = i + 1
            assertEquals("Step index $i should map to $expected", expected, mappedInt)
        }

        val expectedSet = (1..10).toSet()
        assertEquals("Interval scale must contain exactly 10 values", 10, collectedInts.size)
        assertEquals("Interval scale must contain all integers 1..10", expectedSet, collectedInts.toSet())
    }

    @Test
    fun `dim percentage scale (1 to 50 percent) hits every single integer from 1 to 50 without skipping`() {
        val start = 1f
        val stop = 50f
        val steps = 48 // 48 interior steps = 49 intervals = 50 discrete values (1.0f step size)

        val collectedInts = mutableListOf<Int>()
        val totalSteps = steps + 2

        for (i in 0 until totalSteps) {
            val fraction = i.toFloat() / (steps + 1).toFloat()
            val lerpValue = composeLerp(start, stop, fraction)
            val mappedInt = lerpValue.roundToInt()
            collectedInts.add(mappedInt)

            val expected = i + 1
            assertEquals("Step index $i should map to $expected", expected, mappedInt)
        }

        val expectedSet = (1..50).toSet()
        assertEquals("Dim scale must contain exactly 50 values", 50, collectedInts.size)
        assertEquals("Dim scale must contain all integers 1..50", expectedSet, collectedInts.toSet())
    }

    @Test
    fun `continuous drag across all scales can hit every integer without gaps`() {
        // Test dense sampling (simulating fine finger drags across the slider track)
        val scales = listOf(
            Triple(1f, 90f, 1..90),
            Triple(1f, 30f, 1..30),
            Triple(1f, 60f, 1..60),
            Triple(1f, 10f, 1..10),
            Triple(1f, 50f, 1..50)
        )

        for ((start, stop, range) in scales) {
            val seen = mutableSetOf<Int>()
            val sampleCount = 2000
            for (s in 0..sampleCount) {
                val fraction = s.toFloat() / sampleCount.toFloat()
                val value = composeLerp(start, stop, fraction)
                seen.add(value.roundToInt())
            }
            assertEquals(
                "Dense drag across range ${range.first}..${range.last} must hit all numbers",
                range.toSet(),
                seen
            )
        }
    }
}
