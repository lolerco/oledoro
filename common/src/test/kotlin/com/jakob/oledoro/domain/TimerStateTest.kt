package com.jakob.oledoro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    @Test
    fun `isOvertime true when remainingMs negative`() {
        val state = TimerState(remainingMs = -1000L)
        assertTrue(state.isOvertime)
    }

    @Test
    fun `isOvertime true when status is OVERTIME`() {
        val state = TimerState(status = TimerStatus.OVERTIME, remainingMs = 1000L)
        assertTrue(state.isOvertime)
    }

    @Test
    fun `isOvertime false when positive and not overtime status`() {
        val state = TimerState(remainingMs = 1000L, status = TimerStatus.RUNNING)
        assertFalse(state.isOvertime)
    }

    @Test
    fun `elapsedMs calculates correctly`() {
        val state = TimerState(targetDurationMs = 10000L, remainingMs = 4000L)
        assertEquals(6000L, state.elapsedMs)
    }

    @Test
    fun `elapsedMs never negative`() {
        val state = TimerState(targetDurationMs = 10000L, remainingMs = 15000L)
        assertEquals(0L, state.elapsedMs)
    }

    @Test
    fun `progress calculates correctly`() {
        val state = TimerState(targetDurationMs = 10000L, remainingMs = 2500L)
        assertEquals(0.75f, state.progress, 0.001f)
    }

    @Test
    fun `progress clamped to 1 when overtime`() {
        val state = TimerState(targetDurationMs = 10000L, remainingMs = -1000L)
        assertEquals(1f, state.progress, 0.001f)
    }

    @Test
    fun `progress zero when target duration zero`() {
        val state = TimerState(targetDurationMs = 0L, remainingMs = 0L)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `nextPhasePreview focus shows next break`() {
        val state = TimerState(
            phase = TimerPhase.FOCUS,
            completedFocusCount = 0,
            totalRounds = 4,
            shortBreakDurationMs = 5 * 60 * 1000L,
            longBreakDurationMs = 15 * 60 * 1000L
        )
        assertEquals("NEXT: 5 MIN BREAK", state.nextPhasePreview)
    }

    @Test
    fun `nextPhasePreview focus at interval shows long break`() {
        val state = TimerState(
            phase = TimerPhase.FOCUS,
            completedFocusCount = 3,
            totalRounds = 4,
            shortBreakDurationMs = 5 * 60 * 1000L,
            longBreakDurationMs = 15 * 60 * 1000L
        )
        assertEquals("NEXT: 15 MIN BREAK", state.nextPhasePreview)
    }

    @Test
    fun `nextPhasePreview short break shows focus`() {
        val state = TimerState(
            phase = TimerPhase.SHORT_BREAK,
            focusDurationMs = 25 * 60 * 1000L
        )
        assertEquals("NEXT: 25 MIN FOCUS", state.nextPhasePreview)
    }

    @Test
    fun `nextPhasePreview long break shows focus`() {
        val state = TimerState(
            phase = TimerPhase.LONG_BREAK,
            focusDurationMs = 25 * 60 * 1000L
        )
        assertEquals("NEXT: 25 MIN FOCUS", state.nextPhasePreview)
    }

    @Test
    fun `totalFocusTimeMs and totalBreakTimeMs default to zero`() {
        val state = TimerState()
        assertEquals(0L, state.totalFocusTimeMs)
        assertEquals(0L, state.totalBreakTimeMs)
    }

    @Test
    fun `totalFocusTimeMs and totalBreakTimeMs can be set in constructor`() {
        val state = TimerState(totalFocusTimeMs = 3600000L, totalBreakTimeMs = 600000L)
        assertEquals(3600000L, state.totalFocusTimeMs)
        assertEquals(600000L, state.totalBreakTimeMs)
    }

    @Test
    fun `copy preserves totalFocusTimeMs and totalBreakTimeMs`() {
        val state = TimerState(totalFocusTimeMs = 5000L, totalBreakTimeMs = 3000L)
        val copied = state.copy(phase = TimerPhase.SHORT_BREAK)
        assertEquals(5000L, copied.totalFocusTimeMs)
        assertEquals(3000L, copied.totalBreakTimeMs)
    }

    @Test
    fun `copy can override totalFocusTimeMs and totalBreakTimeMs`() {
        val state = TimerState(totalFocusTimeMs = 5000L, totalBreakTimeMs = 3000L)
        val copied = state.copy(totalFocusTimeMs = 10000L, totalBreakTimeMs = 6000L)
        assertEquals(10000L, copied.totalFocusTimeMs)
        assertEquals(6000L, copied.totalBreakTimeMs)
    }
}