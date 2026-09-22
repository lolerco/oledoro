# Features Specification: oledoro

## 1. Overview
A hyper-minimalist, battery-focused Pomodoro timer for Android designed specifically for OLED/AMOLED displays with Gruvbox aesthetic, full tactile haptic feedback, customizable durations and intervals, stationary centered timer layout, and promoted lockscreen live updates (Samsung Now Bar & Android Lockscreen Action Card).

---

## 2. Core Functional Requirements

### 2.1 State Machine & Customizable Pomodoro Cycle
* **Focus Session:** Customizable from 1 to 90 minutes (default: 25 minutes).
* **Short Break:** Customizable from 1 to 30 minutes (default: 5 minutes).
* **Long Break:** Customizable from 1 to 60 minutes (default: 15 minutes).
* **Long Break Interval:** Customizable from 1 to 10 focus sessions before triggering a long break (default: 4 completed rounds).
* **Dynamic Duration Updating:** Adjusting duration or interval sliders in Settings while the timer is idle immediately recalculates target duration and resets remaining time. Updating settings while the timer is running or overtime seamlessly updates target duration and upcoming phase targets without restarting the current tick.
* **Strict Manual State Transitions:** The timer **MUST NEVER** automatically advance between focus and break states. It waits for active user confirmation to ensure deliberate control.
* **Overtime / Negative Ticking (`00:00 -> -00:01`):** Continues ticking into negative numbers upon zero crossing while alerting the user, clearly displaying elapsed overtime.

### 2.2 Upcoming Phase Preview (NextPhasePreview)
* Positioned directly below the large countdown digits, providing clean contextual awareness dynamically computed from current settings:
  * During Focus Rounds before the interval threshold -> `"NEXT: {shortBreakMinutes} MIN BREAK"` (e.g., `"NEXT: 5 MIN BREAK"`)
  * During Short Break -> `"NEXT: {focusMinutes} MIN FOCUS"` (e.g., `"NEXT: 25 MIN FOCUS"`)
  * During Focus Round at the interval threshold -> `"NEXT: {longBreakMinutes} MIN BREAK"` (e.g., `"NEXT: 15 MIN BREAK"`)
  * During Long Break -> `"NEXT: {focusMinutes} MIN FOCUS"` (e.g., `"NEXT: 25 MIN FOCUS"`)
* Persists accurately during countdown, paused state, and overtime.

### 2.3 Two-Way Auto Lightbulb & Session Auto-Dimming
* **Two-Way Auto Toggle:**
  * When **Auto Lightbulb** setting is enabled (`autoBrightenOnFinish == true`):
    * **Auto-Dim on Start:** Starting any session (Focus or Break) from `IDLE` automatically activates screen dimming (`isDimmingActive = true`), keeping the display dark and battery-efficient during work and rest.
    * **Auto-Brighten on Finish:** When any session timer completes (`00:00`), the screen automatically deactivates dimming (`isDimmingActive = false`), returning to normal brightness to visually alert the user.
    * **Manual Override & Session Idempotence:** If the user manually toggles the lightbulb on during an active session, the screen remains bright. Subsequent timer ticks and pause/resume cycles **NEVER** re-dim or alter the state unexpectedly.
  * When disabled (`autoBrightenOnFinish == false`): Starting sessions does not auto-dim, and timer completion does not auto-brighten.
* **Lowercase Subtitle:** In the Settings dialog, the toggle is titled `"AUTO LIGHTBULB"` with its descriptive subtitle strictly formatted in lowercase letters: `"dim when session starts, light up when finished"`.

---

## 3. OLED Ambient Mode & Controls

### 3.1 Stationary Centered Non-Shifting Timer Layout
* **Permanent True Centering:** The core timer block (phase label, countdown/overtime digits, and NextPhasePreview) is strictly anchored to the absolute vertical and horizontal center of the screen via a decoupled `Box` architecture.
* **Zero Layout Shift:** Tapping anywhere on the screen displays the action controls (Play/Pause, Skip, Reset) below the timer block with a smooth fade animation (`fadeIn`/`fadeOut`) without shifting, resizing, or jumping the timer display.
* **True Minimalist Focus:** When controls are hidden, the screen presents only the centered timer and top subtle action icons, maximizing OLED power savings and minimizing visual distraction.

### 3.2 Reversed Lightbulb Logic (State-Reflecting)
* **Bright Screen (Dimming Inactive):**
  * Displays an **illuminated filled lightbulb** (`Icons.Filled.Lightbulb`).
  * Glows with the selected Gruvbox accent color (`themeColor.color`), mirroring the active, bright display state.
* **Dark / Dimmed Screen (Dimming Active):**
  * Displays an **unlit outlined lightbulb** (`Icons.Outlined.Lightbulb`).
  * Rendered in subtle `AmbientDimGray` (`#504945`), mirroring the dark, low-energy display state.
* Tapping the lightbulb toggles dimming immediately with crisp tactile click feedback.

### 3.3 Full Tactile Haptic Feedback
* Crisp, distinct haptic feedback powered by `HapticHelper`:
  * **Crisp Click (`performClick`):** Triggered on primary action buttons (Play/Pause, Skip, Reset), top action bar buttons (Lightbulb dimmer toggle, Settings gear toggle), Settings dialog close button, Auto Lightbulb switch toggle, and Gruvbox color swatch selections.
  * **Micro-Tick (`performTick`):** Triggered on each discrete integer step change of the Dim Level Slider (1%..50%), Focus Time Slider (1..90m), Short Break Slider (1..30m), Long Break Slider (1..60m), and Long Break Interval Slider (1..10 rounds).
  * **Hardware Safety:** Automatically queries `VibratorManager` on Android 12+ (API 31+), `Vibrator` on API 26–30, and legacy vibrator service on pre-Oreo, gracefully no-oping without crashing on vibrator-less hardware.

### 3.4 Customizable Element Colors (Non-Exclusive Gruvbox Palette)
* **Independent Element Color Selection:**
  * **Main Accent Color:** Used across main UI elements, focus digits, and active sliders (default: Yellow `#FABD2F`).
  * **Break Timer Color:** Applied specifically to break countdowns (short break and long break) and break indicators (default: Aqua `#8EC07C`).
  * **Negative / Overtime Color:** Applied specifically to negative countdowns and overtime warnings (default: Red `#FB4934`).
  * **Non-Exclusive:** Each element can independently select any of the 7 Gruvbox colors (`YELLOW`, `ORANGE`, `GREEN`, `AQUA`, `BLUE`, `RED`, `CREAM`), allowing identical or contrasting palette combinations.
  * **Zero Flicker Selection:** Dedicated `ColorPickerSection` composables with fixed sizing and haptic feedback ensure immediate updates without UI flickering, layout jumping, or scroll position disruption.
* **Precision Slider Scaling (100% Number Coverage):**
  * All sliders use mathematically sound step counts and `roundToInt()` quantization, ensuring every single integer value is reachable without precision skips (e.g. eliminating the bug where Long Break skipped 15 and jumped from 14 to 16 due to float truncation).
  * Validated by unit tests across all 5 scales: Focus (1..90 min), Short Break (1..30 min), Long Break (1..60 min), Interval (1..10 rounds), and Dim Level (1%..50%).
* **Zero Flicker Dim Level Adjustment:**
  * Base ambient mode configuration (immersive mode, wake lock, show-when-locked) is strictly decoupled from brightness adjustments.
  * Adjusting the dim percentage modifies only `window.attributes.screenBrightness`, completely eliminating system bar flashes, window insets shifts, and modal shaking while sliding.
* **Theme-Reactive Sliders:**
  * Active track ticks rendered in pure OLED black (`#000000`).
  * Inactive track ticks rendered in a 40% translucent theme tint (`selectedThemeColor.color.copy(alpha = 0.4f)`).
  * Thumb and active track adopt the active Gruvbox color.
* **Credits & Attribution:**
  * Clean distraction-free main screen without intrusive footers.
  * Settings dialog credits card cleanly structured:
    ```
    oledoro
    minimalist, battery-saving pomodoro for oled displays.

    font: jetbrains nerd font mono
    palette: gruvbox
    created by: lolerco & gemini
    ```

### 3.5 Minimalist OLED / Gruvbox / Nerd App Logo
* Designed specifically for modern Android adaptive icons (API 26+):
  * **Background (`ic_launcher_background.xml`):** Absolute pure black (`#000000`) for true zero-emission OLED display.
  * **Foreground (`ic_launcher_foreground.xml`):**
    * **Stopwatch Silhouette:** Mechanical crown pusher (`#8EC07C` Aqua) and stopwatch angled shoulder buttons (`#8EC07C` / `#FE8019`).
    * **Segmented Dial:** 270° Pomodoro focus arc in Gruvbox Orange (`#FE8019`), geometric gap, and 60° break arc in Gruvbox Aqua (`#8EC07C`).
    * **Cardinal Dial Dots:** 12, 3, 6, and 9 o'clock index marks in Gruvbox Yellow (`#FABD2F`).
    * **Nerd Monospaced Terminal Glyph:** Monospaced terminal chevron `>` (`#FABD2F`) and cursor `_` (`#FE8019`) positioned in the dial center (`>_`), evoking developer CLI focus.
  * **Adaptive Mipmap Wrappers:** Standard `ic_launcher.xml` and round `ic_launcher_round.xml` in `res/mipmap-anydpi-v26/`.

---

## 4. Lockscreen Action Card & Samsung Now Bar Integration
* **Notification Channel Recreation (`oledoro_live_updates_v5`):** Ensures existing app installations cleanly adopt `IMPORTANCE_DEFAULT` without cached channel degradation.
* **Channel Properties:**
  * `setSound(null, null)` and `enableVibration(false)`: Prevents annoying audio interrupts on every second tick.
  * `lockscreenVisibility = Notification.VISIBILITY_PUBLIC`: Ensures visibility on secure lockscreens.
* **Notification Configuration:**
  * `CATEGORY_STOPWATCH`, `PRIORITY_DEFAULT`, and `FOREGROUND_SERVICE_IMMEDIATE` promote the ongoing notification directly to the lock screen card and Samsung One UI Now Bar.
* **API 34 Guard:** `ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE` is strictly guarded by `Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE`, safely using `0` on Android 10–13 to avoid `IllegalArgumentException`.
* **On-Screen Permission Prompt:** Renders a non-intrusive banner on Android 13+ (API 33+) if notification permissions have not yet been granted.

