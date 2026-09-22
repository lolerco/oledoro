# Implementation Details: oledoro

## 1. Stationary Centered Compose Layout

In `AmbientScreen.kt`, standard vertical columns with `Arrangement.Center` are avoided because showing/hiding controls causes existing centered content to jump. Furthermore, custom multi-child `Layout` with `measurables[1]` crashes on initial startup because `AnimatedVisibility(visible = false)` emits 0 layout nodes (`IndexOutOfBoundsException`).

Instead, a decoupled `Box` architecture is used:
1. The **Timer Display Column** is anchored to `Alignment.Center` with `burnInShift(burnInOffset)`.
2. The **Action Controls Box** is independently anchored to `Alignment.Center` with `burnInShift(burnInOffset)` and `offset(y = 150.dp)`.

Because both elements are independent direct children of the root full-screen `Box`, toggling `controlsVisible` introduces zero vertical displacement to the timer digits and has zero risk of indexing bounds exceptions:

```kotlin
// 1. Center Timer Display: strictly centered in the Box!
// Never shifts or jumps when action controls appear/disappear.
Column(
    modifier = Modifier
        .align(Alignment.Center)
        .burnInShift(burnInOffset)
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    // Phase label (e.g. FOCUS, SHORT BREAK, or OVERTIME)
    val phaseText = if (isOvertime) {
        "OVERTIME • ${state.phase.displayName.uppercase()}"
    } else {
        state.phase.displayName.uppercase()
    }

    Text(
        text = phaseText,
        color = secondaryColor,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Large JetBrains Mono countdown / overtime digits
    TimerDisplay(
        remainingMs = state.remainingMs,
        isOvertime = isOvertime,
        color = primaryColor
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Next Phase Preview (e.g. "NEXT: 5 MIN BREAK")
    NextPhasePreview(
        previewText = state.nextPhasePreview,
        color = secondaryColor
    )
}

// 2. Action Controls: Positioned below the timer block
// Decoupled from the Timer Column so the timer NEVER shifts.
Box(
    modifier = Modifier
        .align(Alignment.Center)
        .burnInShift(burnInOffset)
        .offset(y = 150.dp)
) {
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ControlsRow(
            status = state.status,
            onStart = onStart,
            onPause = onPause,
            onSkip = onSkip,
            onReset = onReset,
            tint = secondaryColor
        )
    }
}
```

---

## 2. AppSettingsManager Data Layer

`AppSettingsManager` in `AppSettings.kt` provides reactive `StateFlow` streams backed by Android `SharedPreferences`. Input validation enforces strict clamping bounds across all parameters:

```kotlin
class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor: StateFlow<GruvboxColor> = _themeColor.asStateFlow()

    private val _isDimmingActive = MutableStateFlow(loadDimmingActive())
    val isDimmingActive: StateFlow<Boolean> = _isDimmingActive.asStateFlow()

    private val _dimPercentage = MutableStateFlow(loadDimPercentage())
    val dimPercentage: StateFlow<Int> = _dimPercentage.asStateFlow()

    private val _focusMinutes = MutableStateFlow(loadFocusMinutes())
    val focusMinutes: StateFlow<Int> = _focusMinutes.asStateFlow()

    private val _shortBreakMinutes = MutableStateFlow(loadShortBreakMinutes())
    val shortBreakMinutes: StateFlow<Int> = _shortBreakMinutes.asStateFlow()

    private val _longBreakMinutes = MutableStateFlow(loadLongBreakMinutes())
    val longBreakMinutes: StateFlow<Int> = _longBreakMinutes.asStateFlow()

    private val _longBreakInterval = MutableStateFlow(loadLongBreakInterval())
    val longBreakInterval: StateFlow<Int> = _longBreakInterval.asStateFlow()

    private val _autoBrightenOnFinish = MutableStateFlow(loadAutoBrightenOnFinish())
    val autoBrightenOnFinish: StateFlow<Boolean> = _autoBrightenOnFinish.asStateFlow()

    fun setFocusMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 90)
        _focusMinutes.value = clamped
        prefs.edit().putInt(KEY_FOCUS_MINUTES, clamped).apply()
    }

    fun setShortBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 30)
        _shortBreakMinutes.value = clamped
        prefs.edit().putInt(KEY_SHORT_BREAK_MINUTES, clamped).apply()
    }

    fun setLongBreakMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 60)
        _longBreakMinutes.value = clamped
        prefs.edit().putInt(KEY_LONG_BREAK_MINUTES, clamped).apply()
    }

    fun setLongBreakInterval(interval: Int) {
        val clamped = interval.coerceIn(1, 10)
        _longBreakInterval.value = clamped
        prefs.edit().putInt(KEY_LONG_BREAK_INTERVAL, clamped).apply()
    }

    fun setAutoBrightenOnFinish(enabled: Boolean) {
        _autoBrightenOnFinish.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_BRIGHTEN_ON_FINISH, enabled).apply()
    }

    fun setDimPercentage(percentage: Int) {
        val clamped = percentage.coerceIn(1, 50)
        _dimPercentage.value = clamped
        prefs.edit().putInt(KEY_DIM_PERCENTAGE, clamped).apply()
    }

    companion object {
        private const val PREFS_NAME = "oledoro_prefs"
        const val DEFAULT_DIM_PERCENTAGE = 5
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_LONG_BREAK_INTERVAL = 4
        const val DEFAULT_AUTO_BRIGHTEN_ON_FINISH = true
    }
}
```

---

## 3. Dynamic Duration Updates in TimerEngine

In `TimerEngine.kt`, `updateDurations` recalculates phase durations in milliseconds and updates the current state. When the timer is idle, it immediately adapts both target and remaining time:

```kotlin
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
```

In `MainActivity.kt` and `MainViewModel`, settings adjustments invoke `syncEngineDurations()` and notify `TimerForegroundService`:

```kotlin
private fun syncEngineDurations() {
    engine.updateDurations(
        focusMinutes = settingsManager.focusMinutes.value,
        shortBreakMinutes = settingsManager.shortBreakMinutes.value,
        longBreakMinutes = settingsManager.longBreakMinutes.value,
        breakInterval = settingsManager.longBreakInterval.value
    )
}

// In MainActivity:
onFocusMinutesChange = {
    viewModel.setFocusMinutes(it)
    TimerForegroundService.startService(this, TimerForegroundService.ACTION_SYNC)
}
```

---

## 4. Dynamic NextPhasePreview

In `TimerState.kt`, `nextPhasePreview` calculates upcoming phase labels dynamically based on current completed focus counts, total rounds interval, and configured millisecond durations:

```kotlin
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
```

---

## 5. SettingsDialog Component & Controls

In `SettingsDialog.kt`, interactive controls provide discrete step adjustments with micro-tick tactile feedback (`HapticHelper.performTick`), a switch for auto-brighten, and creator credits:

```kotlin
// Focus Duration Slider (1..90 min)
Slider(
    value = focusMinutes.toFloat(),
    onValueChange = { floatVal ->
        val newInt = floatVal.roundToInt()
        if (newInt != focusMinutes) {
            HapticHelper.performTick(context)
            onFocusMinutesChange(newInt)
        }
    },
    valueRange = 1f..90f,
    steps = 88,
    colors = SliderDefaults.colors(
        thumbColor = selectedThemeColor.color,
        activeTrackColor = selectedThemeColor.color,
        inactiveTrackColor = AmbientDimGray,
        activeTickColor = OledBlack,
        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
    )
)

// Long Break Interval Slider (1..10 rounds)
Slider(
    value = longBreakInterval.toFloat(),
    onValueChange = { floatVal ->
        val newInt = floatVal.roundToInt()
        if (newInt != longBreakInterval) {
            HapticHelper.performTick(context)
            onLongBreakIntervalChange(newInt)
        }
    },
    valueRange = 1f..10f,
    steps = 8,
    colors = SliderDefaults.colors(
        thumbColor = selectedThemeColor.color,
        activeTrackColor = selectedThemeColor.color,
        inactiveTrackColor = AmbientDimGray,
        activeTickColor = OledBlack,
        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
    )
)

// Auto-Brighten on Finish Switch
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "AUTO-BRIGHTEN ON FINISH",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AmbientCoolGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Turn light bulb on when session ends",
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            color = AmbientDimGray
        )
    }
    Switch(
        checked = autoBrightenOnFinish,
        onCheckedChange = {
            HapticHelper.performClick(context)
            onAutoBrightenOnFinishChange(it)
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = selectedThemeColor.color,
            checkedTrackColor = selectedThemeColor.color.copy(alpha = 0.4f),
            uncheckedThumbColor = AmbientDimGray,
            uncheckedTrackColor = OledBlack
        )
    )
}

// Credits and Info (Build version removed, Creator lolerco added)
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
        .padding(12.dp)
) {
    Column {
        Text(
            text = "oledoro",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Created by lolerco\nMinimalist, battery-saving Pomodoro for OLED displays.\nFont: JetBrains Nerd Font Mono.\nPalette: Gruvbox.",
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            color = AmbientCoolGray,
            lineHeight = 16.sp
        )
    }
}
```

---

## 6. Reversed Lightbulb State Mapping

In `AmbientScreen.kt`, the lightbulb icon acts as a direct visual reflection of display luminance rather than an action indicator:

```kotlin
// When the screen is bright (dimming inactive), bulb is filled & illuminated with Gruvbox theme accent
// When the screen is dimmed (dimming active), bulb is an outline & unlit in subtle AmbientDimGray
IconButton(
    onClick = {
        HapticHelper.performClick(context)
        onToggleDimming()
    }
) {
    if (!isDimmingActive) {
        Icon(
            imageVector = Icons.Filled.Lightbulb,
            contentDescription = "Bright Screen (Illuminated)",
            tint = themeColor.color,
            modifier = Modifier.size(28.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = "Dim Screen (Unlit)",
            tint = AmbientDimGray,
            modifier = Modifier.size(28.dp)
        )
    }
}
```

---

## 7. Haptic Feedback Engine

Centralized in `HapticHelper.kt` to ensure uniform tactile feedback and backwards compatibility across Android versions:

```kotlin
object HapticHelper {
    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Crisp tactile click feedback for primary buttons, switches, and swatches.
     */
    fun performClick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15L)
            }
        } catch (e: Throwable) {
            // Safe fallback: never crash if vibration is unavailable
        }
    }

    /**
     * Subtle micro-tick feedback for slider steps and fine value adjustments.
     */
    fun performTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(5L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(5L)
            }
        } catch (e: Throwable) {
            // Safe fallback: never crash if vibration is unavailable
        }
    }
}
```

### Integrated Touchpoints:
* `ControlsRow.kt`: Play/Pause, Skip, and Reset buttons trigger `HapticHelper.performClick(context)`.
* `AmbientScreen.kt`: Lightbulb dimmer toggle and Settings gear trigger `HapticHelper.performClick(context)`.
* `SettingsDialog.kt`: Gruvbox color swatches, Auto-Brighten switch, and close button trigger `HapticHelper.performClick(context)`.
* `SettingsDialog.kt`: Focus, Short Break, Long Break, Interval, and Dim sliders trigger `HapticHelper.performTick(context)` on integer changes.

---

## 8. Lockscreen Action Card & Samsung Now Bar Fixes

### 8.1 Foreground Service Type Compatibility Guard
`TimerForegroundService.kt` prevents runtime crashes on Android 10–13:
```kotlin
private fun startForegroundWithNotification(state: TimerState) {
    val notification = notificationHelper.buildTimerNotification(state)
    val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    } else {
        0
    }

    ServiceCompat.startForeground(
        this,
        NotificationHelper.NOTIFICATION_ID_TIMER,
        notification,
        foregroundType
    )
    isForeground = true
}
```

### 8.2 Clean Notification Channel Recreation
In `OledoroApp.kt`:
```kotlin
const val CHANNEL_LIVE_UPDATES = "oledoro_live_updates_v5"

val liveUpdatesChannel = NotificationChannel(
    CHANNEL_LIVE_UPDATES,
    getString(R.string.channel_live_updates_name),
    NotificationManager.IMPORTANCE_DEFAULT
).apply {
    description = getString(R.string.channel_live_updates_description)
    setShowBadge(false)
    setSound(null, null)
    enableVibration(false)
    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
}
```

### 8.3 Notification Builder Optimization
In `NotificationHelper.kt`:
```kotlin
val builder = NotificationCompat.Builder(context, OledoroApp.CHANNEL_LIVE_UPDATES)
    .setSmallIcon(R.drawable.ic_timer)
    .setContentTitle(title)
    .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
    .setOngoing(true)
    .setOnlyAlertOnce(true)
    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    .setContentIntent(createContentPendingIntent())
```

---

## 9. Auto-Brighten on Session Completion & Settings Integration

### 9.1 Event Emission in TimerEngine
When the countdown hits `00:00`, `TimerEngine.kt` emits a single completion event:
```kotlin
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
```

### 9.2 Two-Way Auto Dimming and Brightening in MainViewModel & ForegroundService
In `MainActivity.kt`:
```kotlin
init {
    engine.events.onEach { event ->
        if (event is TimerEvent.PhaseCompleted) {
            // Two-way auto toggle: when timer finishes, light up screen if auto toggle is on
            if (settingsManager.autoBrightenOnFinish.value) {
                settingsManager.setDimmingActive(false)
            }
        }
    }.launchIn(viewModelScope)
}

fun start() {
    // Two-way auto toggle: when starting any session from IDLE, automatically dim screen
    if (settingsManager.autoBrightenOnFinish.value && engine.state.value.status == TimerStatus.IDLE) {
        settingsManager.setDimmingActive(true)
    }
    engine.startTicker(viewModelScope)
}
```

In `TimerForegroundService.kt`:
```kotlin
ACTION_START_SERVICE -> {
    if (OledoroApp.settingsManager.autoBrightenOnFinish.value && engine.state.value.status == TimerStatus.IDLE) {
        OledoroApp.settingsManager.setDimmingActive(true)
    }
    engine.startTicker(serviceScope)
    startForegroundWithNotification(engine.state.value)
}
NotificationHelper.ACTION_NOTIFICATION_NEXT_PHASE -> {
    if (OledoroApp.settingsManager.autoBrightenOnFinish.value) {
        OledoroApp.settingsManager.setDimmingActive(true)
    }
    engine.nextPhase(autoStart = true)
    notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_ALERT)
}
```

Crucially, manual lightbulb toggles during an ongoing session are completely respected. Because dimming is only activated when starting from `TimerStatus.IDLE`, normal clock ticks (`tick()`) and pause/resumptions never re-dim the screen if the user chose to turn the light on mid-session.

### 9.3 Luminance Restoration & Zero-Flicker AmbientModeEffect
In `AmbientController.kt`:
```kotlin
fun setAmbientBrightness(activity: Activity, enable: Boolean, dimPercentage: Int = 5) {
    val layout = activity.window.attributes
    val targetBrightness = (dimPercentage / 100f).coerceIn(0.01f, 1f)
    val newBrightness = if (enable) targetBrightness else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    if (layout.screenBrightness != newBrightness) {
        layout.screenBrightness = newBrightness
        activity.window.attributes = layout
    }
}
```

In `AmbientModeEffect.kt`, the base ambient window configuration (immersive mode, wake lock, showWhenLocked) is decoupled from dynamic brightness changes. This guarantees that dragging the dim percentage slider never flashes system bars, changes window insets, or shakes the dialog:
```kotlin
@Composable
fun AmbientModeEffect(
    dimmingEnabled: Boolean = true,
    dimPercentage: Int = 5
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // Base ambient mode configuration (immersive mode, wake lock, showWhenLocked).
    // Configured once on entry, and cleaned up only when the ambient screen leaves composition.
    DisposableEffect(activity) {
        AmbientController.configureShowWhenLocked(activity)
        AmbientController.configureScreenWake(activity, true)
        AmbientController.setImmersiveMode(activity, true)

        onDispose {
            AmbientController.exitAmbientMode(activity)
        }
    }

    // Dynamic screen brightness update: only updates window.attributes.screenBrightness.
    // Does NOT teardown immersive mode or flash system bars, completely preventing UI flicker!
    DisposableEffect(dimmingEnabled, dimPercentage, activity) {
        AmbientController.setAmbientBrightness(activity, dimmingEnabled, dimPercentage)
        onDispose { }
    }
}
```
Setting `enable = false` immediately restores system screen brightness without altering `dimPercentage`.

---

## 10. Minimalist Nerd / Gruvbox App Logo Design

### 10.1 Vector Adaptive Icon Structure
* **Background (`ic_launcher_background.xml`):**
  - Dimensions: 108dp x 108dp (Viewport: 108 x 108)
  - Pure `#000000` fill rect: `M0,0h108v108h-108z`
* **Foreground (`ic_launcher_foreground.xml`):**
  - Stopwatch Crown & Stem:
    - Crown pusher at (48.5, 18): `M48.5,18 h11 v3.5 h-11 z`, `#8EC07C` Aqua.
    - Stem at (52.5, 21.5): `M52.5,21.5 h3 v3.5 h-3 z`, `#8EC07C` Aqua.
    - Left shoulder at (35, 26) to (31.5, 22.5), `#8EC07C` Aqua.
    - Right shoulder at (73, 26) to (76.5, 22.5), `#FE8019` Orange.
  - Segmented Dial Progress Ring:
    - 270° Focus Arc: `M54,28 A26,26 0 1,1 28,54`, `#FE8019` Orange, strokeWidth 3.5.
    - 60° Break Arc: `M29,47 A26,26 0 0,1 47,29`, `#8EC07C` Aqua, strokeWidth 3.5.
  - Cardinal Dots:
    - 12 o'clock (54, 35.5), 3 o'clock (73, 54.5), 6 o'clock (54, 73.5), 9 o'clock (35, 54.5) in `#FABD2F` Yellow.
  - Nerd Monospaced Terminal Glyph `>_`:
    - Chevron `>`: `M43,47 L50,53 L43,59`, `#FABD2F` Yellow, strokeWidth 3.2.
    - Cursor `_`: `M54,59 L65,59`, `#FE8019` Orange, strokeWidth 3.2.
* **Mipmap Definitions:**
  - `res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` referencing `@drawable/ic_launcher_background` and `@drawable/ic_launcher_foreground`.
* **Manifest Reference:**
  - `AndroidManifest.xml` references `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.

---

## 11. Precision Slider Scaling & Screen Bottom Attribution

### 11.1 Slider Mathematical Precision via `roundToInt()`
In `SettingsDialog.kt`, using `toInt()` on Compose `Slider` values caused severe floating-point truncation bugs:
- Compose's internal `lerp(start, stop, fraction)` produced IEEE-754 single precision floats such as `14.99999905f` for step 15 on a 1..60 scale.
- Calling `.toInt()` on `14.99999905f` truncated towards zero to `14`, completely skipping `15` and jumping directly to `16`.
- Similar truncation skips occurred in Focus (skipping 28, 56) and Short Break (skipping 16).
- Replacing `.toInt()` with `kotlin.math.roundToInt()` guarantees that values in `[n - 0.5, n + 0.5)` map directly to integer $n$, achieving 100% number coverage across all scales.
- Dim Level slider was corrected from `steps = 49` to `steps = 48` for range `1f..50f` (48 interior steps yielding exact 1.0% increments).

### 11.2 Settings Credits Structure
In `SettingsDialog.kt`, credits are placed at the bottom of the dialog card with clean monospace hierarchy:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
        .padding(12.dp)
) {
    Column {
        Text(
            text = "oledoro",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "minimalist, battery-saving pomodoro for oled displays.\n\nfont: jetbrains nerd font mono\npalette: gruvbox\ncreated by: lolerco & gemini",
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            color = AmbientCoolGray,
            lineHeight = 16.sp
        )
    }
}
```
- The main ambient screen remains pure black and distraction-free, with the centered timer staying strictly focused.

---

## 12. Customizable Element Colors & Zero-Flicker Layout

### 12.1 Independent Element Colors
In `AmbientScreen.kt`, digits and indicators are dynamically styled based on timer phase and overtime status:
```kotlin
val isOvertime = state.isOvertime || state.status == TimerStatus.OVERTIME
val isBreak = state.phase == TimerPhase.SHORT_BREAK || state.phase == TimerPhase.LONG_BREAK
val primaryColor = when {
    isOvertime -> negativeColor.color
    isBreak -> breakColor.color
    else -> themeColor.color
}
val secondaryColor = when {
    isOvertime -> negativeColor.color.copy(alpha = 0.8f)
    isBreak -> breakColor.color.copy(alpha = 0.8f)
    else -> AmbientCoolGray
}
```

### 12.2 Reusable Zero-Flicker ColorPickerSection
In `SettingsDialog.kt`, three non-exclusive color picker rows allow independent selection of Main Accent, Break Timer, and Negative Timer colors across all 7 Gruvbox hues:
```kotlin
@Composable
private fun ColorPickerSection(
    title: String,
    selectedColor: GruvboxColor,
    onColorSelected: (GruvboxColor) -> Unit,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AmbientCoolGray,
            letterSpacing = 1.sp
        )
        Text(
            text = selectedColor.displayName.uppercase(),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = selectedColor.color
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GruvboxColor.entries.forEach { gruvboxColor ->
            val isSelected = gruvboxColor == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(gruvboxColor.color)
                    .clickable {
                        HapticHelper.performClick(context)
                        onColorSelected(gruvboxColor)
                    }
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color.White, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "${gruvboxColor.displayName} Selected",
                        tint = OledBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
```
Fixed swatch dimensions (`36.dp`) and stable Row containers ensure zero layout shifts, dialog jumping, or scroll position disruption when selecting colors.


