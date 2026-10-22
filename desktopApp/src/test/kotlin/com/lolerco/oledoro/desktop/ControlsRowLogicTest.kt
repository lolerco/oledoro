package com.lolerco.oledoro.desktop

import com.lolerco.oledoro.domain.TimerEngine
import com.lolerco.oledoro.domain.TimerPhase
import com.lolerco.oledoro.domain.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlsRowLogicTest {

    @Test
    fun `fastForward skips to next phase and automatically starts running`() {
        val engine = TimerEngine()
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)

        // Fast forward action (the >> button)
        engine.nextPhase(autoStart = true)

        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
    }

    @Test
    fun `skip advances to next phase but remains idle`() {
        val engine = TimerEngine()
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)

        // Standard skip action (the >| button)
        engine.nextPhase(autoStart = false)

        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
    }

    @Test
    fun `fastForward from active running session advances to next phase and continues running`() {
        val engine = TimerEngine()
        engine.start()
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(TimerPhase.FOCUS, engine.state.value.phase)

        // Press fast-forward while running
        engine.nextPhase(autoStart = true)

        assertEquals(TimerPhase.SHORT_BREAK, engine.state.value.phase)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)
        assertEquals(5 * 60 * 1000L, engine.state.value.remainingMs)
    }

    @Test
    fun `reset returns remaining time to target duration and sets status to idle`() {
        val engine = TimerEngine()
        engine.start()
        engine.tick(5000L)
        assertEquals(TimerStatus.RUNNING, engine.state.value.status)

        engine.reset()
        assertEquals(TimerStatus.IDLE, engine.state.value.status)
        assertEquals(25 * 60 * 1000L, engine.state.value.remainingMs)
    }
}
