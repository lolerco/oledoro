# Changelog

All notable changes to oledoro will be documented in this file.

## [1.2.0] - 2026-10-01

### Added - Desktop (Linux/Windows/macOS) Support
- **New `desktop/` module** — Compose Multiplatform (JVM) desktop app sharing `common/` domain/UI/theme
- **Cross-platform native notifications** using system notification daemons:
  - Linux: `notify-send` (libnotify) with app icon
  - Windows: PowerShell `System.Windows.Forms.MessageBox`
  - macOS: `osascript` UserNotifications
- **Desktop settings dialog** with:
  - 3 Gruvbox color pickers (Main Accent, Break Timer, Negative/Overtime)
  - 4 time sliders (Focus 1-90min, Short Break 1-30min, Long Break 1-60min, Interval 1-10)
  - About section with credits
  - No auto-dim/dim-percentage (desktop doesn't need it)
- **App icon** — Uses Android app's vector drawable rendered to PNG (48x48 for notifications, multi-size for icon themes)
- **Always-visible controls** (no click-to-toggle)
- **Total focus/break time counters** at bottom (horizontal `·` separator)

### Added - Android
- **Total focus/break time counters** at bottom of AmbientScreen (vertically stacked)
- **Total time accumulation** in TimerEngine (tracks overtime too)
- **Settings version bump** to 1.2

### Changed - Shared (`common/` module)
- **TimerEngine**: Added `totalFocusTimeMs` / `totalBreakTimeMs` observable state
- **TimerState**: Added `totalFocusTimeMs` / `totalBreakTimeMs` fields
- **AppSettingsManager**: Reactive `mutableStateOf` properties for instant Compose recomposition
- **Persistence**: File-based (`~/.oledoro/desktop.properties` / `~/.oledoro/desktop.properties` on Android) with defaults fallback
- **Settings sync**: Changes apply immediately without closing dialog

### Fixed
- TimerEngine initialized with persisted settings on boot (was using hardcoded defaults)
- Android total time counters now tick in real-time
- Desktop notification icon scaling (uses 48x48 PNG, prevents oversized icon layering message)
- Icon now transparent outline-only with Gruvbox color strokes

### Testing
- **66 unit tests** across all modules:
  - `common`: 32 tests (TimerEngine, TimerState, AppSettingsManager, TimeFormatter, AutoBrightenAndCycle)
  - `desktop`: 5 tests (DesktopNotificationManager)
  - `app`: existing Android tests
- All tests pass on every commit

### Module Structure
```
oledoro/
├── app/           # Android (unchanged API, uses own TimerEngine copy)
├── common/        # Shared Kotlin: domain, data, UI components, theme
│   ├── domain/    # TimerEngine, TimerState, TimerPhase, TimerStatus, TimeFormatter
│   ├── data/      # AppSettingsManager, GruvboxColor
│   └── ui/        # Theme, components (TimerDisplay, ControlsRow, SettingsDialog, etc.)
├── desktop/       # Compose Desktop (JVM)
│   ├── Main.kt    # Entry point
│   ├── DesktopSettingsDialog.kt
│   └── DesktopNotificationManager.kt
└── settings.gradle.kts
```

### Technical Details
- **Kotlin**: 2.0.21
- **Compose Multiplatform**: 1.7.3
- **Android**: minSdk 26, targetSdk 34, Compose BOM 2024.09.00
- **Desktop**: Compose Desktop 1.7.3, JVM target 17
- **Coroutines**: 1.8.1
- **Architecture**: MVVM (Android) / Compose state (Desktop), shared domain layer