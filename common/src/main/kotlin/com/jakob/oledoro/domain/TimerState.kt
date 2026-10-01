package com.jakob.oledoro.domain

data class TimerState(
    val phase: TimerPhase = TimerPhase.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val targetDurationMs: Long = 25 * 60 * 1000L,
    val remainingMs: Long = targetDurationMs,
    val currentRound: Int = 1,
    val totalRounds: Int = 4,
    val completedFocusCount: Int = 0,
    val focusDurationMs: Long = 25 * 60 * 1000L,
    val shortBreakDurationMs: Long = 5 * 60 * 1000L,
    val longBreakDurationMs: Long = 15 * 60 * 1000L,
    val totalFocusTimeMs: Long = 0L,
    val totalBreakTimeMs: Long = 0L
) {
    val isOvertime: Boolean
        get() = remainingMs < 0 || status == TimerStatus.OVERTIME

    val elapsedMs: Long
        get() = (targetDurationMs - remainingMs).coerceAtLeast(0L)

    val progress: Float
        get() {
            if (targetDurationMs <= 0L) return 0f
            if (remainingMs <= 0L) return 1f
            val elapsed = targetDurationMs - remainingMs
            return (elapsed.toFloat() / targetDurationMs.toFloat()).coerceIn(0f, 1f)
        }

    /**
     * Preview text for the upcoming phase when the user finishes or taps skip.
     */
    val nextPhasePreview: String
        get() = when (phase) {
            TimerPhase.FOCUS -> {
                val nextCompleted = completedFocusCount + 1
                val isNextLongBreak = (nextCompleted % totalRounds == 0)
                val breakMinutes = if (isNextLongBreak) {
                    (longBreakDurationMs / 60000L).coerceAtLeast(1L)
                } else {
                    (shortBreakDurationMs / 60000L).coerceAtLeast(1L)
                }
                "NEXT: $breakMinutes MIN BREAK"
            }
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> {
                val focusMinutes = (focusDurationMs / 60000L).coerceAtLeast(1L)
                "NEXT: $focusMinutes MIN FOCUS"
            }
        }
}

sealed interface TimerEvent {
    data class PhaseCompleted(val phase: TimerPhase) : TimerEvent
}
