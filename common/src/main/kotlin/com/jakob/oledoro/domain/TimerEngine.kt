package com.jakob.oledoro.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerEngine(
    var focusDurationMs: Long = DEFAULT_FOCUS_DURATION_MS,
    var shortBreakDurationMs: Long = DEFAULT_SHORT_BREAK_DURATION_MS,
    var longBreakDurationMs: Long = DEFAULT_LONG_BREAK_DURATION_MS,
    var totalRounds: Int = DEFAULT_TOTAL_ROUNDS,
    private val coroutineScope: CoroutineScope? = null,
    initialState: TimerState? = null
) {
    companion object {
        const val DEFAULT_FOCUS_DURATION_MS = 25 * 60 * 1000L
        const val DEFAULT_SHORT_BREAK_DURATION_MS = 5 * 60 * 1000L
        const val DEFAULT_LONG_BREAK_DURATION_MS = 15 * 60 * 1000L
        const val DEFAULT_TOTAL_ROUNDS = 4
    }

    private val _state = MutableStateFlow(
        initialState ?: TimerState(
            phase = TimerPhase.FOCUS,
            status = TimerStatus.IDLE,
            targetDurationMs = focusDurationMs,
            remainingMs = focusDurationMs,
            currentRound = 1,
            totalRounds = totalRounds,
            completedFocusCount = 0,
            focusDurationMs = focusDurationMs,
            shortBreakDurationMs = shortBreakDurationMs,
            longBreakDurationMs = longBreakDurationMs
        )
    )
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    private var completedFocusCount: Int = initialState?.completedFocusCount ?: 0

    private var activeScope: CoroutineScope? = coroutineScope
    private var tickerJob: Job? = null

    @Synchronized
    fun start() {
        val current = _state.value
        if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.OVERTIME) {
            return
        }
        val newStatus = if (current.remainingMs <= 0L) TimerStatus.OVERTIME else TimerStatus.RUNNING
        _state.value = current.copy(status = newStatus)

        activeScope?.let { scope ->
            ensureTickerRunning(scope)
        }
    }

    private fun ensureTickerRunning(scope: CoroutineScope, tickIntervalMs: Long = 1000L) {
        if (tickerJob == null || tickerJob?.isActive == false) {
            tickerJob = scope.launch {
                while (isActive) {
                    delay(tickIntervalMs)
                    tick(tickIntervalMs)
                }
            }
        }
    }

    @Synchronized
    fun pause() {
        val current = _state.value
        if (current.status == TimerStatus.RUNNING || current.status == TimerStatus.OVERTIME) {
            _state.value = current.copy(status = TimerStatus.PAUSED)
        }
    }

    @Synchronized
    fun reset() {
        val current = _state.value
        _state.value = current.copy(
            status = TimerStatus.IDLE,
            remainingMs = current.targetDurationMs
        )
    }

    @Synchronized
    fun resetAll() {
        completedFocusCount = 0
        _state.value = TimerState(
            phase = TimerPhase.FOCUS,
            status = TimerStatus.IDLE,
            targetDurationMs = focusDurationMs,
            remainingMs = focusDurationMs,
            currentRound = 1,
            totalRounds = totalRounds,
            completedFocusCount = 0,
            focusDurationMs = focusDurationMs,
            shortBreakDurationMs = shortBreakDurationMs,
            longBreakDurationMs = longBreakDurationMs
        )
    }

    /**
     * Advances to the next phase in the cycle progression:
     * Focus 1 -> Short Break 1 -> ... -> Focus N -> Long Break -> Focus 1 ...
     */
    @Synchronized
    fun nextPhase(autoStart: Boolean = false) {
        val current = _state.value
        val initialStatus = if (autoStart) TimerStatus.RUNNING else TimerStatus.IDLE

        when (current.phase) {
            TimerPhase.FOCUS -> {
                completedFocusCount++
                val isLongBreak = (completedFocusCount % totalRounds == 0)
                val nextPhase = if (isLongBreak) TimerPhase.LONG_BREAK else TimerPhase.SHORT_BREAK
                val targetDuration = if (isLongBreak) longBreakDurationMs else shortBreakDurationMs

                _state.value = current.copy(
                    phase = nextPhase,
                    status = initialStatus,
                    targetDurationMs = targetDuration,
                    remainingMs = targetDuration,
                    completedFocusCount = completedFocusCount,
                    totalRounds = totalRounds,
                    focusDurationMs = focusDurationMs,
                    shortBreakDurationMs = shortBreakDurationMs,
                    longBreakDurationMs = longBreakDurationMs
                )
            }
            TimerPhase.SHORT_BREAK -> {
                val nextRound = (current.currentRound % totalRounds) + 1
                _state.value = current.copy(
                    phase = TimerPhase.FOCUS,
                    status = initialStatus,
                    targetDurationMs = focusDurationMs,
                    remainingMs = focusDurationMs,
                    currentRound = nextRound,
                    completedFocusCount = completedFocusCount,
                    totalRounds = totalRounds,
                    focusDurationMs = focusDurationMs,
                    shortBreakDurationMs = shortBreakDurationMs,
                    longBreakDurationMs = longBreakDurationMs
                )
            }
            TimerPhase.LONG_BREAK -> {
                completedFocusCount = 0
                _state.value = current.copy(
                    phase = TimerPhase.FOCUS,
                    status = initialStatus,
                    targetDurationMs = focusDurationMs,
                    remainingMs = focusDurationMs,
                    currentRound = 1,
                    completedFocusCount = 0,
                    totalRounds = totalRounds,
                    focusDurationMs = focusDurationMs,
                    shortBreakDurationMs = shortBreakDurationMs,
                    longBreakDurationMs = longBreakDurationMs
                )
            }
        }

        if (autoStart) {
            activeScope?.let { scope ->
                ensureTickerRunning(scope)
            }
        }
    }

    @Synchronized
    fun updateDurations(
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        breakInterval: Int
    ) {
        this.focusDurationMs = focusMinutes * 60 * 1000L
        this.shortBreakDurationMs = shortBreakMinutes * 60 * 1000L
        this.longBreakDurationMs = longBreakMinutes * 60 * 1000L
        this.totalRounds = breakInterval

        val current = _state.value
        val newTarget = when (current.phase) {
            TimerPhase.FOCUS -> this.focusDurationMs
            TimerPhase.SHORT_BREAK -> this.shortBreakDurationMs
            TimerPhase.LONG_BREAK -> this.longBreakDurationMs
        }
        val newRemaining = if (current.status == TimerStatus.IDLE) newTarget else current.remainingMs

        _state.value = current.copy(
            targetDurationMs = if (current.status == TimerStatus.IDLE) newTarget else current.targetDurationMs,
            remainingMs = newRemaining,
            totalRounds = this.totalRounds,
            focusDurationMs = this.focusDurationMs,
            shortBreakDurationMs = this.shortBreakDurationMs,
            longBreakDurationMs = this.longBreakDurationMs
        )
    }

    @Synchronized
    fun tick(deltaMs: Long = 1000L) {
        if (deltaMs <= 0L) return
        val current = _state.value
        if (current.status != TimerStatus.RUNNING && current.status != TimerStatus.OVERTIME) {
            return
        }

        val wasPositive = current.remainingMs > 0L
        val newRemainingMs = current.remainingMs - deltaMs
        val reachedZeroOrOvertime = wasPositive && (newRemainingMs <= 0L)
        val newStatus = if (newRemainingMs <= 0L) TimerStatus.OVERTIME else TimerStatus.RUNNING

        _state.value = current.copy(
            remainingMs = newRemainingMs,
            status = newStatus
        )

        if (reachedZeroOrOvertime) {
            _events.tryEmit(TimerEvent.PhaseCompleted(current.phase))
        }
    }

    fun startTicker(coroutineScope: CoroutineScope, tickIntervalMs: Long = 1000L): Job {
        this.activeScope = coroutineScope
        start()
        tickerJob?.cancel()
        val job = coroutineScope.launch {
            while (isActive) {
                delay(tickIntervalMs)
                tick(tickIntervalMs)
            }
        }
        tickerJob = job
        return job
    }

    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }
}
