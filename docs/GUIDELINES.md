# Guidelines: oledoro

## 1. Battery & OLED Preservation Directives
1. **True OLED Black Background:** All background surfaces must remain absolute pure black (`#000000`) to guarantee individual OLED pixels are turned completely off, drawing zero power.
2. **Auto-Brighten Attention Grabber:** When a focus or break session completes, automatically deactivating dimming instantly restores system brightness, serving as a silent visual cue that grabs user attention without requiring loud acoustic alarms.
3. **Low-Power Ambient Display:** Anti-burn-in shift periodically offsets UI coordinates to protect OLED panels during prolonged stationary display.
4. **Hardware-Efficient Haptics:** Restrict tactile pulses to subtle 5–15ms events (`performClick` and `performTick`) to ensure immediate physical confirmation without exhausting battery reserves.

---

## 2. UI & Design Directives

### 2.1 Stationary Centered Non-Shifting Layout
* **Zero Displacement Rule:** The central timer block (phase label, countdown/overtime digits, NextPhasePreview) must remain permanently anchored to the exact vertical and horizontal center of the display.
* **Secondary Controls Placement:** Action controls (Play/Pause, Skip, Reset) must be placed at a fixed offset below the center timer block.
* **Never Shift on Visibility Toggles:** Standard `Column(verticalArrangement = Arrangement.Center)` layouts must be avoided because revealing controls causes the timer to jump upward. Multi-child `Layout` measuring `measurables[1]` crashes on startup when controls are hidden. Use a decoupled `Box` architecture where the timer column and controls box are independent siblings anchored to center.
* **Fluid Transitions:** Controls must enter/exit using subtle alpha fading (`fadeIn`/`fadeOut`) without triggering layout recalculations that could jitter the timer digits.

### 2.2 Customizable Durations, Slider Precision & Settings UX
* **Strict Range Enforcement:** Clamping must be enforced consistently across settings storage and domain layers:
  * Focus Duration: 1 to 90 minutes (`steps = 88`).
  * Short Break: 1 to 30 minutes (`steps = 28`).
  * Long Break: 1 to 60 minutes (`steps = 58`).
  * Long Break Interval: 1 to 10 completed focus sessions (`steps = 8`).
  * Dim Level: 1% to 50% (`steps = 48`).
* **Mathematical Precision via `roundToInt()`:**
  * Always use `floatVal.roundToInt()` when converting Compose slider float outputs to integer domain values. Never use `toInt()`, as IEEE-754 single precision float truncation causes intermediate values (e.g. 15 in Long Break, 28 and 56 in Focus) to be skipped.
* **Flicker-Free Brightness Adjustments:**
  * Base ambient mode configuration (immersive mode, wake lock, show-when-locked) must be strictly decoupled from brightness adjustments. Never teardown immersive mode or trigger window insets relayouts during continuous slider interaction.
* **Immediate Idle Synchronization:** Changing any duration or interval while the timer is in `IDLE` state must immediately update the on-screen digits and preview labels without requiring an app reload or manual reset.
* **Running State Preservation:** Changing durations while the timer is running or in overtime must update future target durations and phase previews without wiping active elapsed time or restarting current ticks.
* **Two-Way Auto Lightbulb Directives:**
  * **Auto-Dim on Start:** When `autoBrightenOnFinish` is enabled, starting any session (Focus or Break) from `TimerStatus.IDLE` must automatically activate screen dimming (`isDimmingActive = true`).
  * **Auto-Brighten on Finish:** When any session timer reaches `00:00`, screen dimming must be automatically deactivated (`isDimmingActive = false`) so the screen lights up to alert the user.
  * **Mid-Session Manual Override Preservation:** If the user turns the light on during an active session (or while paused), subsequent ticks (`tick()`) and pause/resume cycles must **NEVER** re-dim or break the screen state. Screen remains bright.
  * **Lowercase Subtitle Enforcement:** The text under the Auto Lightbulb toggle setting must strictly use lowercase letters only: `"dim when session starts, light up when finished"`.
* **Customizable Element Colors Directives:**
  * Support independent selection of Main Accent Color (`themeColor`), Break Timer Color (`breakColor`), and Negative Timer Color (`negativeColor`).
  * Non-exclusive: each element can select any of the 7 Gruvbox colors independently.
  * Reusable `ColorPickerSection` must have fixed sizing (`36.dp` circles) and fixed row heights to guarantee zero UI flicker or scroll jumping.
* **Clean Attribution & Placement:**
  * Main ambient screen is kept clean and minimalist without intrusive footer text.
  * Settings credits structured as:
    ```
    oledoro
    minimalist, battery-saving pomodoro for oled displays.

    font: jetbrains nerd font mono
    palette: gruvbox
    created by: lolerco & gemini
    ```

### 2.3 Reversed Lightbulb Logic
* Always map the lightbulb icon directly to the current state of screen illumination:
  * **Screen Bright (`!isDimmingActive`):** `Icons.Filled.Lightbulb` with theme accent tint.
  * **Screen Dimmed (`isDimmingActive`):** `Icons.Outlined.Lightbulb` with `AmbientDimGray` tint.
* Never invert this logic (e.g. do not show an illuminated bulb when the screen is dimmed).

### 2.4 Tactile Interaction Standards
* Every interactive component must provide immediate feedback:
  * Primary actions (Play/Pause, Skip, Reset): `HapticHelper.performClick(context)`.
  * Dialog controls (Close button, Color swatches, Auto Lightbulb switch): `HapticHelper.performClick(context)`.
  * Fine slider adjustments: `HapticHelper.performTick(context)` on integer value increments.

### 2.5 Gruvbox Aesthetics & Adaptive Icon Design
* Maintain strict Gruvbox palette consistency across the app, widgets, and launcher icon:
  * Yellow (`#FABD2F`), Orange (`#FE8019`), Green (`#B8BB26`), Aqua (`#8EC07C`), Blue (`#83A598`), Red (`#FB4934`), Cream (`#EBDBB2`).
* Adaptive icon foreground must center within the 72dp safe zone of the 108dp viewport.
* Pure black `#000000` must be used for the launcher icon background to maintain edge-to-edge OLED harmony on modern launcher docks and app grids.

---

## 3. Lockscreen & Background Service Directives

### 3.1 Android Version Guards
* Always guard Android 14+ specific foreground service types:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
  } else {
      0
  }
  ```
* Never pass unsupported foreground service type flags to `startForeground()` on Android 10–13.

### 3.2 Lockscreen Live Updates & Now Bar Stability
* Ongoing timer notifications must be configured with:
  * `NotificationCompat.CATEGORY_STOPWATCH`
  * `NotificationCompat.PRIORITY_DEFAULT`
  * `NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE`
  * `Notification.VISIBILITY_PUBLIC`
* Never set sound or vibration on the ongoing ticking notification channel (`CHANNEL_LIVE_UPDATES`), as 1-second ticks must remain strictly silent.
* When modifying notification channel parameters, always increment the channel ID (e.g., `oledoro_live_updates_v5`) to force the OS to apply new settings.

---

## 4. Testing & Verification Guidelines

### 4.1 Custom Durations & Interval Verification
* **Idle Duration Update Test:**
  * Configure Focus to 45m in `TimerEngine.updateDurations(45, 10, 20, 3)`.
  * In `TimerStatus.IDLE`, verify `targetDurationMs == 45 * 60 * 1000L` and `remainingMs == 45 * 60 * 1000L`.
* **Running Duration Update Test:**
  * Start timer at 25m, tick 5 seconds (`remainingMs = 24m 55s`).
  * Call `updateDurations(50, 10, 20, 3)`.
  * Verify `remainingMs` remains `24m 55s` while `focusDurationMs` becomes `50 * 60 * 1000L`.
* **Configurable Break Interval Progression:**
  * Set interval to 2 (`totalRounds = 2`).
  * Cycle: Focus 1 -> Short Break 1 -> Focus 2 -> Long Break.
  * Verify `LONG_BREAK` triggers at Round 2 rather than default Round 4.
* **Dynamic NextPhasePreview Validation:**
  * For custom settings (e.g. 50m Focus, 10m Short Break, 30m Long Break, Interval 2):
    * Round 1 Focus -> `"NEXT: 10 MIN BREAK"`.
    * Short Break 1 -> `"NEXT: 50 MIN FOCUS"`.
    * Round 2 Focus -> `"NEXT: 30 MIN BREAK"`.
    * Long Break -> `"NEXT: 50 MIN FOCUS"`.

### 4.2 Stationary Centered Layout Verification
* **Visual Non-Shifting Check:**
  * Verify that the Timer Display Column remains strictly anchored at `Alignment.Center` in the root `Box`.
  * Assert that toggling controls visibility produces zero vertical jump in the countdown digits.
* **Offset Verification:**
  * Verify Action Controls Box is positioned at `Alignment.Center` with `offset(y = 150.dp)`.

### 4.3 Auto-Brighten Setting Verification
* **Auto-Brighten Enabled (`autoBrightenOnFinish == true`):**
  * With `isDimmingActive == true`, tick to zero crossing (`00:00`).
  * Assert `TimerEvent.PhaseCompleted` is emitted and `settingsManager.isDimmingActive.value` becomes `false`.
  * Assert user's configured `dimPercentage` is preserved.
* **Auto-Brighten Disabled (`autoBrightenOnFinish == false`):**
  * With `isDimmingActive == true`, tick to zero crossing (`00:00`).
  * Assert `TimerEvent.PhaseCompleted` is emitted, but `settingsManager.isDimmingActive.value` remains `true`.

### 4.4 Build & Test Automation
* `./gradlew test` must pass all unit tests without warnings or failures.
* `./gradlew assembleDebug` must compile cleanly and produce `app-debug.apk` in `app/build/outputs/apk/debug/`.

