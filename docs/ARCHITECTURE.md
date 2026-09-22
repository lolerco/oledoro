# Architecture: oledoro

## 1. Architectural Overview

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   Presentation Layer                                   │
│  ┌──────────────────────────────────────────┐   ┌───────────────────────────────────┐  │
│  │         AmbientScreen (Compose)          │   │          SettingsDialog           │  │
│  │  - Stationary Centered Compose Layout    │   │  - Main Accent Color Swatches     │  │
│  │    (Zero-Shift Timer + Controls Below)   │   │  - Break Timer Color Swatches     │  │
│  │  - Reversed Lightbulb Indicator          │   │  - Negative Timer Color Swatches  │  │
│  │    (Bright=Filled, Dark=Outline)         │   │  - Focus Duration Slider (1..90m) │  │
│  │  - Anti-Burn-In Pixel Shift (±4dp)       │   │  - Short Break Slider (1..30m)    │  │
│  │  - Dynamic NextPhasePreview Display      │   │  - Long Break Slider (1..60m)     │  │
│  │  - Overtime Display (-MM:SS)             │   │  - Break Interval Slider (1..10r) │  │
│  │  - Independent Element Color Tinting     │   │  - Auto Lightbulb Toggle Switch   │  │
│  │  - Tactile Haptics (Click & Micro-Tick)  │   │  - Dim Level Slider (1..50%)      │  │
│  │                                          │   │  - Creator Credits (lolerco)      │  │
│  └────────────────────┬─────────────────────┘   └─────────────────┬─────────────────┘  │
│                       │                                           │                    │
│                       ▼                                           ▼                    │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                  MainViewModel                                   │  │
│  │  - Collects TimerState, themeColor, breakColor, negativeColor, Dimming, etc.     │  │
│  │  - Coordinates Settings Persistence via AppSettingsManager                       │  │
│  │  - Propagates Duration & Interval Updates to TimerEngine (syncEngineDurations)   │  │
│  │  - Two-Way Auto Toggle: Auto-dims on IDLE start; auto-brightens on completion    │  │
│  │  - Preserves Manual Brightness Toggle during running sessions (idempotent ticks) │  │
│  └────────────────────┬───────────────────────────────────────────┬─────────────────┘  │
└───────────────────────┼───────────────────────────────────────────┼────────────────────┘
                        │                                           │
                        ▼                                           ▼
┌──────────────────────────────────────────────────┐   ┌─────────────────────────────────┐
│                    Data Layer                    │   │          Domain Layer           │
│  ┌────────────────────────────────────────────┐  │   │  ┌───────────────────────────┐  │
│  │             AppSettingsManager             │  │   │  │        TimerEngine        │  │
│  │  - SharedPreferences Persistence           │  │   │  │  - Dynamic updateDurations│  │
│  │  - Reactive StateFlows:                    │  │   │  │  - Configurable Intervals │  │
│  │    • themeColor (GruvboxColor, default YEL)│  │   │  │  - Dynamic nextPhasePreview│  │
│  │    • breakColor (GruvboxColor, default AQU)│  │   │  │  - Negative Overtime Tick │  │
│  │    • negativeColor (GruvboxColor, def RED) │  │   │  │  - SharedFlow<TimerEvent> │  │
│  │    • isDimmingActive (Boolean)             │  │   │  └─────────────┬─────────────┘  │
│  │    • dimPercentage (1..50)                 │  │   └────────────────┼────────────────┘
│  │    • focusMinutes (1..90)                  │  │                    │
│  │    • shortBreakMinutes (1..30)             │  │                    │
│  │    • longBreakMinutes (1..60)              │  │                    │
│  │    • longBreakInterval (1..10)             │  │                    │
│  │    • autoBrightenOnFinish / autoLightbulb  │  │                    │
│  └────────────────────────────────────────────┘  │                    │
└──────────────────────────────────────────────────┘                    │
                        │                                               │
                        ▼                                               ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                           System & Background Service Layer                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                              TimerForegroundService                              │  │
│  │  - Synchronizes with Shared TimerEngine (TimerEngineHolder.engine)                │  │
│  │  - Receives ACTION_SYNC to Update Notification on Settings Changes               │  │
│  │  - API 34+ Guard: ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE               │  │
│  │  - Channel Recreation: oledoro_live_updates_v5 (IMPORTANCE_DEFAULT)              │  │
│  │  - Priority: NotificationCompat.PRIORITY_DEFAULT + CATEGORY_STOPWATCH            │  │
│  │  - Lockscreen Action Card & Samsung One UI Now Bar Promotion                     │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                          HapticHelper & Hardware Haptics                         │  │
│  │  - VibratorManager (API 31+) / Vibrator (API 26-30) / Legacy Fallback            │  │
│  │  - EFFECT_CLICK for buttons, toggles, switches, and swatches                     │  │
│  │  - EFFECT_TICK for discrete slider value steps                                   │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Stationary Centered Compose Layout Architecture

To maintain an unencumbered, distraction-free aesthetic and prevent jarring visual jumps, the timer block is anchored strictly to the geometric center of the viewport regardless of whether action controls are visible.

```
┌──────────────────────────────────────────────────────────────┐
│ [💡] Top Action Bar                                      [⚙️] │
│                                                              │
│                                                              │
│                     ┌──────────────────┐                     │
│                     │      FOCUS       │                     │
│                     │      25:00       │ ◄── Permanently      │
│                     │ NEXT: 5 MIN BREAK│     Centered Block   │
│                     └──────────────────┘     (timerX, timerY) │
│                                                              │
│                               ↕ 36dp Fixed Spacing           │
│                                                              │
│                     ┌──────────────────┐                     │
│                     │  [⏮]  [▶]  [⏭]   │ ◄── Controls Row     │
│                     └──────────────────┘     (Appears below   │
│                                              without shifting │
│                                              the timer block) │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### Layout Placement & Centering Architecture
In standard Jetpack Compose layouts, wrapping items in a `Column(verticalArrangement = Arrangement.Center)` causes existing items to jump upward whenever new items appear. Furthermore, using a custom multi-child `Layout` that accesses `measurables[1]` crashes with `IndexOutOfBoundsException` when controls are hidden (`AnimatedVisibility` emits 0 nodes).

`AmbientScreen` solves this cleanly and robustly using a decoupled `Box` architecture:

1. **Timer Display Column:**
   - Positioned with `Modifier.align(Alignment.Center).burnInShift(burnInOffset)`.
   - Anchored directly to the true geometric center of the root full-screen `Box`.
   - Its vertical position is strictly invariant to whether other views are visible or hidden.

2. **Action Controls Box:**
   - Positioned with `Modifier.align(Alignment.Center).burnInShift(burnInOffset).offset(y = 150.dp)`.
   - Anchored to the exact center and offset vertically below the timer block.
   - Wrapped in `AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut())`.

3. **Invariable Coordinates & Crash Immunity:** Because both elements are independent siblings within the root `Box`, toggling control visibility never alters the measurement or placement of the timer column, completely eliminating layout shifting and measurement bounds exceptions.

---

## 3. Settings Propagation & State Synchronization Flow

The following sequence details how custom durations, intervals, and auto-brighten preferences propagate through the system:

```
┌────────┐          ┌──────────────┐         ┌─────────────┐        ┌─────────────┐        ┌────────────────────────┐
│  User  │          │SettingsDialog│         │MainViewModel│        │ AppSettings │        │      TimerEngine       │
└───┬────┘          └──────┬───────┘         └──────┬──────┘        └──────┬──────┘        └───────────┬────────────┘
    │                      │                        │                      │                           │
    │ Adjusts Slider/Switch│                        │                      │                           │
    │─────────────────────>│                        │                      │                           │
    │                      │ onFocusMinutesChange() │                      │                           │
    │                      │───────────────────────>│                      │                           │
    │                      │                        │ setFocusMinutes(m)   │                           │
    │                      │                        │─────────────────────>│                           │
    │                      │                        │                      │ Store in SharedPreferences│
    │                      │                        │                      │ Update _focusMinutes Flow │
    │                      │                        │                      │<──────────────────────────│
    │                      │                        │ syncEngineDurations()│                           │
    │                      │                        │─────────────────────────────────────────────────>│
    │                      │                        │                      │                           │ updateDurations(...)
    │                      │                        │                      │                           │ - Recalculates durations
    │                      │                        │                      │                           │ - Updates target & remaining
    │                      │                        │                      │                           │ - Recomputes nextPhasePreview
    │                      │                        │                      │                           │ - Emits new TimerState
    │                      │                        │                      │                           │<──────────────────────────
    │                      │                        │ TimerForegroundService.startService(ACTION_SYNC) │
    │                      │                        │──────────────────────────────────────────┐       │
    │                      │                        │                                          ▼       │
    │                      │                        │                            ┌────────────────────────┐
    │                      │                        │                            │ TimerForegroundService │
    │                      │                        │                            │ - Rebuilds Notification│
    │                      │                        │                            │ - Syncs Lockscreen/Bar │
    │                      │                        │                            └────────────────────────┘
```

1. **User Interaction:** User adjusts a duration slider (e.g. Focus 1..90m), break interval (1..10 rounds), or auto-brighten switch.
2. **Persistence:** `AppSettingsManager` validates and clamps the input (e.g. `.coerceIn(1, 90)`), persists to Android `SharedPreferences`, and updates the corresponding reactive `StateFlow`.
3. **Engine Synchronization:** `MainViewModel.syncEngineDurations()` calls `engine.updateDurations(...)` with the current settings values.
4. **State Adaptation:** `TimerEngine` recalculates millisecond durations, updates `totalRounds`, updates `targetDurationMs`, and—if currently in `TimerStatus.IDLE`—updates `remainingMs`. `nextPhasePreview` immediately re-evaluates dynamically.
5. **Foreground Service Synchronization:** `MainActivity` sends `ACTION_SYNC` to `TimerForegroundService`, ensuring the ongoing notification on the lockscreen and Samsung Now Bar reflects updated durations immediately.

---

## 4. Auto-Brighten Mechanism

1. **Detection & Emission:**
   - In `TimerEngine.tick(deltaMs)`, when `remainingMs` transitions from positive to `<= 0L`, the engine transitions `status` to `TimerStatus.OVERTIME` and emits `TimerEvent.PhaseCompleted(current.phase)` onto its `SharedFlow`.
   - Subsequent negative ticks do not re-emit the event, ensuring clean single-pulse triggering.

2. **Conditional Deactivation in MainViewModel:**
   - `MainViewModel` subscribes to `engine.events` in its `init` block and checks the user's `autoBrightenOnFinish` preference:
     ```kotlin
     engine.events.onEach { event ->
         if (event is TimerEvent.PhaseCompleted) {
             // Auto-brighten on session completion: if enabled and dimming is active, deactivate it
             if (settingsManager.autoBrightenOnFinish.value && settingsManager.isDimmingActive.value) {
                 settingsManager.setDimmingActive(false)
             }
         }
     }.launchIn(viewModelScope)
     ```
   - Setting `dimmingActive` to `false` triggers `AmbientModeEffect` to execute:
     ```kotlin
     AmbientController.setAmbientBrightness(activity, enable = false, dimPercentage)
     ```
   - Window brightness is reset to `WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE`, immediately restoring normal display luminance to alert the user.
   - If `autoBrightenOnFinish` is disabled by the user, dimming remains active and the session continues into overtime quietly.

3. **Setting Preservation:**
   - The user's preferred `dimPercentage` in `AppSettingsManager` remains untouched, allowing subsequent sessions to re-dim with a single tap.

---

## 5. OLED Ambient Mode UI Logic

### 5.1 Reversed Lightbulb Logic
To maintain clear visual parity with screen illumination:
* **Bright Screen (`!isDimmingActive`):**
  - Icon: `Icons.Filled.Lightbulb`
  - Tint: `themeColor.color` (Illuminated bulb representing an active bright screen)
* **Dark Screen (`isDimmingActive`):**
  - Icon: `Icons.Outlined.Lightbulb`
  - Tint: `AmbientDimGray` (`#504945`) (Unlit bulb outline representing dimmed screen)

### 5.2 Theme-Reactive Slider Track & Controls
All Compose `Slider` components in `SettingsDialog` synchronize with the active Gruvbox theme:
* Active track color: `selectedThemeColor.color`
* Inactive track color: `AmbientDimGray`
* Active tick color: `OledBlack` (`#000000`)
* Inactive tick color: `selectedThemeColor.color.copy(alpha = 0.4f)`
* Thumb color: `selectedThemeColor.color`

---

## 6. Lock Screen & Samsung Now Bar Service Architecture

### 6.1 API 34 Compatibility Guard
* In Android 14+ (API 34, `UPSIDE_DOWN_CAKE`), foreground services declaring `specialUse` must specify the foreground service type parameter in `startForeground()`.
* Calling `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 26–33 causes a runtime `IllegalArgumentException`.
* Architecture implements an explicit version check:
  ```kotlin
  val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
  } else {
      0
  }
  ```

### 6.2 Notification Channel Recreation (`oledoro_live_updates_v5`)
* Android notification channels are immutable once created by the package manager.
* Channel ID updated to `oledoro_live_updates_v5` ensures that devices updating from earlier builds migrate to:
  * `NotificationManager.IMPORTANCE_DEFAULT`: Ensures system prioritizes the notification card on the lockscreen and Samsung Now Bar.
  * `setSound(null, null)` and `enableVibration(false)`: Guarantees silence during continuous 1-second chronometer ticks.
  * `lockscreenVisibility = Notification.VISIBILITY_PUBLIC`: Guarantees full visibility when the device is locked.

### 6.3 Notification Promotion Attributes
* `setCategory(NotificationCompat.CATEGORY_STOPWATCH)`
* `setPriority(NotificationCompat.PRIORITY_DEFAULT)`
* `setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)`
* Prevents 10-second foreground service notification delay and avoids lock screen unlock requirements.

---

## 7. Vector Adaptive Icon Architecture

* **Standard Android Adaptive Icon Spec (API 26+):**
  * `app/src/main/res/drawable/ic_launcher_background.xml`: 108dp x 108dp vector with pure black `#000000` path.
  * `app/src/main/res/drawable/ic_launcher_foreground.xml`: 108dp x 108dp vector containing:
    * Mechanical stopwatch crown (`#8EC07C` Aqua) and angled shoulder pushers.
    * 270° Focus arc (`#FE8019` Orange) and 60° Break arc (`#8EC07C` Aqua).
    * Cardinal 12/3/6/9 dial dots (`#FABD2F` Yellow).
    * Monospaced terminal glyph `>_` (`#FABD2F` Yellow chevron, `#FE8019` Orange cursor).
  * `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`: Adaptive icon manifest wrappers.
  * `AndroidManifest.xml`: References `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.

