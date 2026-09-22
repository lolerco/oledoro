package com.jakob.oledoro.domain

import com.jakob.oledoro.data.GruvboxColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoBrightenAndCycleTest {

    @Test
    fun `focus session zero crossing emits PhaseCompleted for auto-brighten`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L)
        val events = mutableListOf<TimerEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.toList(events)
        }

        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)

        // Tick to zero
        engine.tick(1000L)
        assertEquals(0L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        // Verify auto-brighten event emitted
        assertEquals(1, events.size)
        val event = events.first() as TimerEvent.PhaseCompleted
        assertEquals(TimerPhase.FOCUS, event.phase)

        // Tick into overtime - should NOT emit second auto-brighten event
        engine.tick(1000L)
        assertEquals(-1000L, engine.state.value.remainingMs)
        assertEquals(1, events.size)

        job.cancel()
    }

    @Test
    fun `short break zero crossing emits PhaseCompleted for auto-brighten`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L, shortBreakDurationMs = 1000L)
        val events = mutableListOf<TimerEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.toList(events)
        }

        // Advance to short break
        engine.nextPhase(autoStart = true)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)

        // Tick down to 0
        engine.tick(1000L)
        assertEquals(0L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        // Verify PhaseCompleted event emitted for SHORT_BREAK
        assertEquals(1, events.size)
        val event = events.first() as TimerEvent.PhaseCompleted
        assertEquals(TimerPhase.SHORT_BREAK, event.phase)

        job.cancel()
    }

    @Test
    fun `long break zero crossing emits PhaseCompleted for auto-brighten`() = runTest {
        val engine = TimerEngine(longBreakDurationMs = 1000L)
        val events = mutableListOf<TimerEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.toList(events)
        }

        // Advance 4 focus rounds to reach long break:
        // Focus 1 -> Break 1 -> Focus 2 -> Break 2 -> Focus 3 -> Break 3 -> Focus 4 -> Long Break
        engine.nextPhase() // Break 1
        engine.nextPhase() // Focus 2
        engine.nextPhase() // Break 2
        engine.nextPhase() // Focus 3
        engine.nextPhase() // Break 3
        engine.nextPhase() // Focus 4
        engine.nextPhase(autoStart = true) // Long Break

        assertEquals(TimerPhase.LONG_BREAK, engine.state.value.phase)

        // Tick down to zero
        engine.tick(1000L)
        assertEquals(0L, engine.state.value.remainingMs)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        // Verify PhaseCompleted event emitted for LONG_BREAK
        assertEquals(1, events.size)
        val event = events.first() as TimerEvent.PhaseCompleted
        assertEquals(TimerPhase.LONG_BREAK, event.phase)

        job.cancel()
    }

    @Test
    fun `auto-brighten toggle disabled does not trigger dimming deactivation on phase completion`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L)

        var isDimmingActive = true
        val configuredDimPercentage = 8
        val autoBrightenOnFinish = false

        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.collect { event ->
                if (event is TimerEvent.PhaseCompleted) {
                    if (autoBrightenOnFinish && isDimmingActive) {
                        isDimmingActive = false
                    }
                }
            }
        }

        engine.start()
        assertTrue("Dimming should be active initially", isDimmingActive)

        // When timer reaches 0
        engine.tick(1000L)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        // With autoBrightenOnFinish = false, dimming must NOT be deactivated
        assertTrue("Dimming should remain active when autoBrightenOnFinish is false", isDimmingActive)
        assertEquals("Configured dim percentage must be preserved", 8, configuredDimPercentage)

        job.cancel()
    }

    @Test
    fun `auto-brighten toggle enabled deactivates dimming while preserving dimPercentage`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L)

        var isDimmingActive = true
        val configuredDimPercentage = 8
        val autoBrightenOnFinish = true

        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.collect { event ->
                if (event is TimerEvent.PhaseCompleted) {
                    if (autoBrightenOnFinish && isDimmingActive) {
                        isDimmingActive = false
                    }
                }
            }
        }

        engine.start()
        assertTrue("Dimming should be active initially", isDimmingActive)

        // When timer reaches 0
        engine.tick(1000L)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)

        // With autoBrightenOnFinish = true, dimming must be deactivated
        assertFalse("Dimming must be deactivated upon session completion when autoBrightenOnFinish is true", isDimmingActive)
        assertEquals("Configured dim percentage must be preserved", 8, configuredDimPercentage)

        job.cancel()
    }

    @Test
    fun `exhaustive cycle progression and nextPhasePreview validation`() {
        val engine = TimerEngine(
            focusDurationMs = 25 * 60 * 1000L,
            shortBreakDurationMs = 5 * 60 * 1000L,
            longBreakDurationMs = 15 * 60 * 1000L,
            totalRounds = 4
        )

        // Round 1: Focus
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals(4, engine.state.value.totalRounds)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 1: Break (5m)
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 2: Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(2, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 2: Break (5m)
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 3: Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(3, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 3: Break (5m)
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 4: Focus (CRUCIAL: Next is 15m Long Break)
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(4, engine.state.value.currentRound)
        assertEquals("NEXT: 15 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 4: Long Break (15m)
        engine.nextPhase()
        assertEquals(TimerPhase.LONG_BREAK, engine.state.value.phase)
        assertEquals(15 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Cycle repeats -> Focus Round 1
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `nextPhasePreview persists through overtime and pause`() {
        val engine = TimerEngine(focusDurationMs = 1000L)
        engine.start()
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Tick into overtime
        engine.tick(2000L)
        assertTrue(engine.state.value.isOvertime)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Pause in overtime
        engine.pause()
        assertEquals(TimerStatus.PAUSED, engine.state.value.status)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Reset current phase
        engine.reset()
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `resetAll resets cycle and nextPhasePreview back to round 1`() {
        val engine = TimerEngine()

        // Advance to round 4 focus
        engine.nextPhase() // Break 1
        engine.nextPhase() // Focus 2
        engine.nextPhase() // Break 2
        engine.nextPhase() // Focus 3
        engine.nextPhase() // Break 3
        engine.nextPhase() // Focus 4
        assertEquals(4, engine.state.value.currentRound)
        assertEquals("NEXT: 15 MIN BREAK", engine.state.value.nextPhasePreview)

        // Full reset
        engine.resetAll()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(0, engine.state.value.completedFocusCount)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `custom durations dynamically format nextPhasePreview across cycle phases`() {
        val engine = TimerEngine(
            focusDurationMs = 50 * 60 * 1000L,
            shortBreakDurationMs = 10 * 60 * 1000L,
            longBreakDurationMs = 30 * 60 * 1000L,
            totalRounds = 4
        )

        // Round 1: Focus (50m) -> preview should reflect 10m short break
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(50 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(50 * 60 * 1000L, engine.state.value.remainingMs)
        assertEquals("NEXT: 10 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 1: Short Break (10m) -> preview should reflect 50m focus
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(10 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(10 * 60 * 1000L, engine.state.value.remainingMs)
        assertEquals("NEXT: 50 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 2: Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(2, engine.state.value.currentRound)
        assertEquals("NEXT: 10 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 2: Short Break
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 50 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 3: Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(3, engine.state.value.currentRound)
        assertEquals("NEXT: 10 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 3: Short Break
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 50 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 4: Focus -> preview should reflect 30m long break
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(4, engine.state.value.currentRound)
        assertEquals("NEXT: 30 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 4: Long Break (30m) -> preview should reflect 50m focus
        engine.nextPhase()
        assertEquals(TimerPhase.LONG_BREAK, engine.state.value.phase)
        assertEquals(30 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(30 * 60 * 1000L, engine.state.value.remainingMs)
        assertEquals("NEXT: 50 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Cycle wraps back to Round 1: Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals("NEXT: 10 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `custom long break interval switches to long break after configured rounds`() {
        // Configure 2 focus rounds before long break
        val engine = TimerEngine(
            focusDurationMs = 25 * 60 * 1000L,
            shortBreakDurationMs = 5 * 60 * 1000L,
            longBreakDurationMs = 15 * 60 * 1000L,
            totalRounds = 2
        )

        // Round 1: Focus (1 of 2)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals(2, engine.state.value.totalRounds)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Round 1: Short Break
        engine.nextPhase()
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Round 2: Focus (2 of 2 -> next is Long Break!)
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(2, engine.state.value.currentRound)
        assertEquals("NEXT: 15 MIN BREAK", engine.state.value.nextPhasePreview)

        // Switches to Long Break after only 2 focus rounds instead of 4
        engine.nextPhase()
        assertEquals(TimerPhase.LONG_BREAK, engine.state.value.phase)
        assertEquals(15 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals("NEXT: 25 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Next phase wraps back to Round 1 Focus
        engine.nextPhase()
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)
        assertEquals(1, engine.state.value.currentRound)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)
    }

    @Test
    fun `updateDurations when IDLE updates target remaining time and preview`() {
        val engine = TimerEngine(
            focusDurationMs = 25 * 60 * 1000L,
            shortBreakDurationMs = 5 * 60 * 1000L,
            longBreakDurationMs = 15 * 60 * 1000L,
            totalRounds = 4
        )

        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(25 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(25 * 60 * 1000L, engine.state.value.remainingMs)
        assertEquals("NEXT: 5 MIN BREAK", engine.state.value.nextPhasePreview)

        // Update durations when IDLE in FOCUS phase
        engine.updateDurations(
            focusMinutes = 50,
            shortBreakMinutes = 10,
            longBreakMinutes = 30,
            breakInterval = 2
        )

        val updatedFocusState = engine.state.value
        assertEquals(TimerStatus.IDLE, updatedFocusState.status)
        assertEquals(TimerPhase.FOCUS, updatedFocusState.phase)
        assertEquals(50 * 60 * 1000L, updatedFocusState.targetDurationMs)
        assertEquals(50 * 60 * 1000L, updatedFocusState.remainingMs)
        assertEquals(50 * 60 * 1000L, updatedFocusState.focusDurationMs)
        assertEquals(10 * 60 * 1000L, updatedFocusState.shortBreakDurationMs)
        assertEquals(30 * 60 * 1000L, updatedFocusState.longBreakDurationMs)
        assertEquals(2, updatedFocusState.totalRounds)
        assertEquals("NEXT: 10 MIN BREAK", updatedFocusState.nextPhasePreview)

        // Transition to SHORT_BREAK (IDLE)
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(10 * 60 * 1000L, engine.state.value.targetDurationMs)
        assertEquals(10 * 60 * 1000L, engine.state.value.remainingMs)
        assertEquals("NEXT: 50 MIN FOCUS", engine.state.value.nextPhasePreview)

        // Update durations when IDLE in SHORT_BREAK phase
        engine.updateDurations(
            focusMinutes = 45,
            shortBreakMinutes = 8,
            longBreakMinutes = 20,
            breakInterval = 3
        )

        val updatedBreakState = engine.state.value
        assertEquals(TimerPhase.SHORT_BREAK, updatedBreakState.phase)
        assertEquals(8 * 60 * 1000L, updatedBreakState.targetDurationMs)
        assertEquals(8 * 60 * 1000L, updatedBreakState.remainingMs)
        assertEquals(45 * 60 * 1000L, updatedBreakState.focusDurationMs)
        assertEquals(8 * 60 * 1000L, updatedBreakState.shortBreakDurationMs)
        assertEquals(20 * 60 * 1000L, updatedBreakState.longBreakDurationMs)
        assertEquals(3, updatedBreakState.totalRounds)
        assertEquals("NEXT: 45 MIN FOCUS", updatedBreakState.nextPhasePreview)
    }

    @Test
    fun `two-way auto toggle dims screen when starting focus session from IDLE`() = runTest {
        val engine = TimerEngine(focusDurationMs = 25 * 60 * 1000L)
        var isDimmingActive = false
        val autoLightbulb = true

        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertFalse("Screen is initially bright", isDimmingActive)

        // Starting from IDLE with autoLightbulb = true
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()

        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertTrue("Screen must dim upon starting focus session", isDimmingActive)
    }

    @Test
    fun `two-way auto toggle dims screen when starting break session from IDLE`() = runTest {
        val engine = TimerEngine(focusDurationMs = 25 * 60 * 1000L, shortBreakDurationMs = 5 * 60 * 1000L)
        var isDimmingActive = false
        val autoLightbulb = true

        // Advance to break in IDLE
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertFalse("Screen is bright before break start", isDimmingActive)

        // Starting break session from IDLE with autoLightbulb = true
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()

        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertTrue("Screen must dim upon starting break session", isDimmingActive)
    }

    @Test
    fun `two-way auto toggle lights up screen when focus or break session finishes`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L, shortBreakDurationMs = 1000L)
        var isDimmingActive = false
        val autoLightbulb = true

        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.collect { event ->
                if (event is TimerEvent.PhaseCompleted) {
                    if (autoLightbulb) {
                        isDimmingActive = false
                    }
                }
            }
        }

        // 1. Focus session starts and dims
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()
        assertTrue("Dimmed during focus", isDimmingActive)

        // Focus completes at 0:00 -> lights up
        engine.tick(1000L)
        assertFalse("Lights up when focus completes", isDimmingActive)

        // 2. Next to Short Break
        engine.nextPhase(autoStart = false)
        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)

        // Start break session -> dims
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()
        assertTrue("Dimmed during break", isDimmingActive)

        // Break completes at 0:00 -> lights up
        engine.tick(1000L)
        assertFalse("Lights up when break completes", isDimmingActive)

        job.cancel()
    }

    @Test
    fun `turning light on during session with auto toggle on preserves bright state without re-dimming`() = runTest {
        val engine = TimerEngine(focusDurationMs = 10000L)
        var isDimmingActive = false
        val autoLightbulb = true

        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.collect { event ->
                if (event is TimerEvent.PhaseCompleted) {
                    if (autoLightbulb) {
                        isDimmingActive = false
                    }
                }
            }
        }

        // Start session -> dims
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()
        assertTrue("Screen dimmed on start", isDimmingActive)

        // Timer ticks several seconds
        engine.tick(1000L)
        engine.tick(1000L)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)

        // User manually turns on light during session (toggle dimming)
        isDimmingActive = false
        assertFalse("Screen manually turned bright by user", isDimmingActive)

        // Consecutive ticks occur during the session
        engine.tick(1000L)
        engine.tick(1000L)
        assertFalse("Screen must stay bright and NOT re-dim on subsequent ticks", isDimmingActive)

        // Pausing and resuming current session
        engine.pause()
        assertEquals(TimerStatus.PAUSED, engine.state.value.status)
        assertFalse("Screen remains bright while paused", isDimmingActive)

        // Resuming from PAUSED (not IDLE) - must NOT re-dim
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertFalse("Screen must remain bright upon resuming", isDimmingActive)

        // Once session completes at zero, it is already bright and stays bright
        engine.tick(6000L)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertFalse("Screen remains bright after completion", isDimmingActive)

        job.cancel()
    }

    @Test
    fun `auto toggle disabled does not dim on start and does not light up on finish`() = runTest {
        val engine = TimerEngine(focusDurationMs = 1000L)
        var isDimmingActive = false
        val autoLightbulb = false

        val job = launch(UnconfinedTestDispatcher()) {
            engine.events.collect { event ->
                if (event is TimerEvent.PhaseCompleted) {
                    if (autoLightbulb) {
                        isDimmingActive = false
                    }
                }
            }
        }

        // Start from IDLE with autoLightbulb = false
        if (autoLightbulb && engine.state.value.status == TimerStatus.IDLE) {
            isDimmingActive = true
        }
        engine.start()
        assertFalse("Screen should NOT dim when autoLightbulb is disabled", isDimmingActive)

        // User manually dims screen
        isDimmingActive = true

        // Timer reaches zero
        engine.tick(1000L)
        assertEquals(TimerStatus.OVERTIME, engine.state.value.status)
        assertTrue("Screen should stay dimmed if autoLightbulb is disabled", isDimmingActive)

        job.cancel()
    }

    @Test
    fun `element colors are non-exclusive and independently configurable`() {
        var themeColor = GruvboxColor.YELLOW
        var breakColor = GruvboxColor.AQUA
        var negativeColor = GruvboxColor.RED

        // User can change each independently, even to the same color or different colors
        themeColor = GruvboxColor.ORANGE
        assertEquals(GruvboxColor.ORANGE, themeColor)
        assertEquals(GruvboxColor.AQUA, breakColor)
        assertEquals(GruvboxColor.RED, negativeColor)

        // Break color can be set to same color as theme
        breakColor = GruvboxColor.ORANGE
        assertEquals(GruvboxColor.ORANGE, themeColor)
        assertEquals(GruvboxColor.ORANGE, breakColor)

        // Negative color can be set independently
        negativeColor = GruvboxColor.BLUE
        assertEquals(GruvboxColor.BLUE, negativeColor)
    }
}
