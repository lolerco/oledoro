package com.jakob.oledoro.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    @Test
    fun `default initial state matches oledoro specifications`() {
        val engine = TimerEngine()
        val state = engine.state.value

        assertEquals(TimerPhase.FOCUS, state.phase)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(25 * 60 * 1000L, state.targetDurationMs)
        assertEquals(25 * 60 * 1000L, state.remainingMs)
        assertEquals(0L, state.elapsedMs)
        assertEquals(1, state.currentRound)
        assertEquals(4, state.totalRounds)
        assertFalse(state.isOvertime)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals("NEXT: 5 MIN BREAK", state.nextPhasePreview)
    }

    @Test
    fun `full 25-5 25-5 25-5 25-15 cycle progression and nextPhasePreview`() {
        val engine = TimerEngine()

        // 1. Focus Round 1 (25m)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals(25 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Skip to Short Break 1 (5m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(5 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Skip to Focus Round 2 (25m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(2, engine.state.value.currentRound)
        assertEquals(25 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Skip to Short Break 2 (5m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(5 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Skip to Focus Round 3 (25m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(3, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Skip to Short Break 3 (5m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(5 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Skip to Focus Round 4 (25m) -> Next should be 15 MIN BREAK!
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(4, engine.state.value.currentRound)
        assertEquals("NEXT: 15 MIN BREAK", engine.state.value.nextPhasePreview)

        // Skip to Long Break (15m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.LONG_BREAK, engine.state.value.phase)
        assertEquals(15 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Skip restarts new cycle at Focus Round 1 (25m)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `overtime negative ticking continues past zero without auto advancing`() = runTest {
        val engine = TimerEngine(focusDurationMs = 2000L)
        val events = mutableListOf<TimerEvent>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            engine.events.toList(events)
        }

        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)

        // Tick 1s -> 1000ms remaining
        engine.tick(1000L)
        assertEquals(1000L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertTrue(events.isEmpty())

        // Tick 1s -> 0ms reached -> triggers PhaseCompleted and enters OVERTIME
        engine.tick(1000L)
        assertEquals(0L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertTrue(engine.state.value.isOvertime)
        assertEquals(1, events.size)
        assertEquals(TimerEvent.PhaseCompleted(TimerPhase.FOCUS), events.first())

        // Tick into negative (-1000ms) -> remains in FOCUS, ticks negative
        engine.tick(1000L)
        assertEquals(-1000L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase) // Does NOT auto-switch!
        assertEquals(1, events.size) // Event not fired again

        // Tick further negative (-2000ms)
        engine.tick(1000L)
        assertEquals(-2000L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        collectJob.cancel()
    }

    @Test
    fun `manual transition required from overtime to start break`() {
        val engine = TimerEngine(focusDurationMs = 1000L)
        engine.start()
        engine.tick(1000L) // reaches 0:00
        engine.tick(1500L) // -1500ms overtime

        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)

        // User actively taps Start Break / Next Phase
        engine.nextPhase(autoStart = true)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(5 * 60 * 1000L, engine.state.value.remainingMs)
    }

    @Test
    fun `pause and resume during negative ticking preserves negative time`() {
        val engine = TimerEngine(focusDurationMs = 1000L)
        engine.start()
        engine.tick(2000L) // reaches -1000ms

        assertEquals(-1000L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        engine.pause()
        assertEquals(TimerStatus.PAUSED, engine.state.value.status)
        assertEquals(-1000L, engine.state.value.remainingMs)

        engine.start()
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertEquals(-1000L, engine.state.value.remainingMs)

        engine.tick(1000L)
        assertEquals(-2000L, engine.state.value.remainingMs)
    }

    @Test
    fun `updateDurations while idle updates remaining and target`() {
        val engine = TimerEngine()
        engine.updateDurations(
            focusMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            breakInterval = 2
        )

        val state = engine.state.value
        assertEquals(50 * 60 * 1000L, state.targetDurationMs)
        assertEquals(50 * 60 * 1000L, state.remainingMs)
        assertEquals(2, state.totalRounds)
    }

    @Test
    fun `updateDurations while running preserves remaining time but updates target`() {
        val engine = TimerEngine(focusDurationMs = 25 * 60 * 1000L)
        engine.start()
        engine.tick(5000L) // 5 seconds elapsed

        val remainingBeforeUpdate = engine.state.value.remainingMs

        engine.updateDurations(
            focusMinutes = 60,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            breakInterval = 3
        )

        val state = engine.state.value
        assertEquals(60 * 60 * 1000L, state.focusDurationMs)
        assertEquals(remainingBeforeUpdate, state.remainingMs) // remaining unchanged
        assertEquals(3, state.totalRounds)
    }
}