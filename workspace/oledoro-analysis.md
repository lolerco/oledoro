# Oledoro Source Analysis — Desktop Compose Multiplatform Migration

## Project Overview
- **App**: `oledoro` — minimalist OLED-optimized Pomodoro timer
- **Location**: `/home/jakob/projects/programming/oledoro/app/`
- **Build**: Android Gradle Plugin (`build.gradle.kts`), `compileSdk=34`, `minSdk=26`, `targetSdk=34`, `jvmTarget=17`
- **Compose BOM**: `2024.09.00`
- **Language**: Kotlin (all source)

---

## File Inventory (All Source Files Read)

### Theme (`ui/theme/`)
- `Color.kt` — static `Color` values (`OledBlack`, `AmbientAmber`, etc.)
- `Theme.kt` — `OledPomodoroTheme` composable using `MaterialTheme` (`Material3` dark scheme)
- `Type.kt` — `Typography`, `JetBrainsMono` font family (`FontFamily` from `androidx.compose.ui.text.font.Font` with `R.font.*` resources)

### Components (`ui/components/`)
- `ControlsRow.kt` — `Row`, `IconButton`, `Icon` (Material icons: `PlayArrow`, `Pause`, `SkipNext`, `Refresh`)
- `TimerDisplay.kt` — `Text` with `JetBrainsMono`, `TextAlign.Center`
- `NextPhasePreview.kt` — `Text`
- `RoundIndicator.kt` — `Row`, `Box`, `CircleShape`
- `SettingsDialog.kt` — `Dialog`, `Card`, `Column`, `Row`, `Slider` (`Material3.Slider`), `Switch` (`Material3.Switch`), `IconButton`, `Icon`

### Screens (`ui/screens/`)
- `AmbientScreen.kt` — main screen: `Box`, `Column`, `Row`, `Text`, `AnimatedVisibility` (`fadeIn`/`fadeOut`), `Spacer`, `Modifier.background/border/clickable/offset/fillMaxSize/padding/align/size`

### Ambient / Window (`ui/ambient/`)
- `AmbientController.kt` — Android `Activity` window management (`WindowManager.LayoutParams`, `FLAG_KEEP_SCREEN_ON`, `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`, `setShowWhenLocked`, `setTurnScreenOn`, `WindowCompat`, `WindowInsetsControllerCompat`)
- `BurnInShift.kt` — anti-burn-in pixel shift (`Modifier.offset`, `produceState`, `delay`)

### Domain (`domain/`)
- `TimerEngine.kt` — `MutableStateFlow`, `MutableSharedFlow`, `CoroutineScope`, `launch`, `delay`, `Job`
- `TimerState.kt` — `data class` with computed properties
- `TimerPhase.kt` — `enum class`
- `TimerStatus.kt` — `enum class`
- `TimeFormatter.kt` — pure Kotlin (`String.format`, `Locale.US`, `abs`)

### Data (`data/`)
- `AppSettings.kt` — `SharedPreferences` (`Context.getSharedPreferences`), `MutableStateFlow`, `GruvboxColor` enum with `androidx.compose.ui.graphics.Color`

### Service (`service/`)
- `TimerForegroundService.kt` — `Service`, `Binder` (`IBinder`), `Notification`, `ServiceCompat.startForeground`, `PendingIntent`
- `NotificationHelper.kt` — `NotificationCompat.Builder`, `PendingIntent` (`FLAG_IMMUTABLE`, `FLAG_UPDATE_CURRENT`), `NotificationManager`

### Main / App (`.`)
- `MainActivity.kt` — `ComponentActivity`, `ViewModel` (`ViewModel`, `viewModelScope`), `registerForActivityResult` (`ActivityResultContracts.RequestPermission`), `NotificationManagerCompat`, `ContextCompat.checkSelfPermission`
- `OledoroApp.kt` — `Application`, `NotificationChannel`, `NotificationManager`, `RingtoneManager`, `AudioAttributes`, `getString`

### Utilities (`ui/utils/`)
- `HapticHelper.kt` — `Context.getSystemService`, `Vibrator`, `VibrationEffect`, `Build.VERSION.SDK_INT`

---

## 1. Compose APIs Used (Compose Multiplatform Relevant)

### Material3 (Standard in CMP Desktop)
- `MaterialTheme`, `darkColorScheme` (`androidx.compose.material3`)
- `MaterialTheme.typography` (`Typography`)
- `IconButton`, `Icon`, `Text`, `Slider`, `Switch`, `Card`, `Dialog`
- `Icon` from `androidx.compose.material.icons` (extended material icons: `Filled.PlayArrow`, `Filled.Pause`, `Filled.SkipNext`, `Filled.Refresh`, `Filled.Settings`, `Filled.Lightbulb`, `Outlined.Lightbulb`, `Filled.Check`, `Filled.Close`)

### Compose Foundation (Standard in CMP)
- Layout: `Box`, `Column`, `Row`, `Spacer`
- Modifiers: `Modifier.fillMaxSize()`, `.fillMaxWidth()`, `.padding()`, `.background()`, `.border()`, `.clickable()`, `.offset()`, `.align()`, `.size()`, `.clip()` (implicitly via `CircleShape`), `.verticalScroll()`
- Animation: `AnimatedVisibility`, `fadeIn()`, `fadeOut()` (`androidx.compose.animation`)
- State: `remember`, `mutableStateOf`, `produceState`, `MutableInteractionSource`
- Interaction: `interactionSource`, `indication = null`
- Drawing: `RoundedCornerShape`, `CircleShape` (`androidx.compose.foundation.shape`)

### Compose Graphics / UI (Standard in CMP)
- `Color`, `toArgb()` (`androidx.compose.ui.graphics`)
- `Modifier.background(Color, RoundedCornerShape)`, `Modifier.border(width, Color, RoundedCornerShape)`
- `LocalContext.current` (`androidx.compose.ui.platform.LocalContext`)
- `LocalView.current` (`androidx.compose.ui.platform.LocalView`)

### Compose Lifecycle / Runtime (Desktop-compatible with adjustments)
- `SideEffect` (`androidx.compose.runtime.SideEffect`)
- `getValue` / `setValue` (`androidx.compose.runtime`)
- `DisposableEffect` (`androidx.compose.runtime.DisposableEffect`)

---

## 2. Android-Specific APIs Requiring Replacement for Desktop

### A. Context / Activity Dependencies (Critical)
- `data/AppSettings.kt`: `Context.getSharedPreferences()` — **must replace** with desktop persistence (e.g., Java `Preferences` on desktop, or file-based JSON)
- `MainActivity.kt`: `ComponentActivity`, `registerForActivityResult`, `Intent`, `Settings.ACTION_APP_NOTIFICATION_SETTINGS`, `startActivity()` — **entirely Android-specific**
- `OledoroApp.kt`: `Application`, `NotificationChannel`, `NotificationManager`, `RingtoneManager`, `AudioAttributes`
- `ui/theme/Theme.kt`: `view.context as? Activity`, `WindowCompat.getInsetsController`, `window.statusBarColor`, `window.navigationBarColor`
- `ui/ambient/AmbientController.kt`: `Activity.window`, `WindowManager.LayoutParams`, `FLAG_KEEP_SCREEN_ON`, `FLAG_SHOW_WHEN_LOCKED`, `FLAG_TURN_SCREEN_ON`, `setShowWhenLocked()`, `WindowInsetsControllerCompat`

### B. Notification / Service (Critical)
- `service/NotificationHelper.kt`: `Notification`, `NotificationCompat`, `NotificationManager`, `PendingIntent` (`FLAG_IMMUTABLE` / `FLAG_UPDATE_CURRENT`), `Intent`
- `service/TimerForegroundService.kt`: `Service`, `Binder` (`IBinder`), `ServiceInfo`, `ServiceCompat.startForeground`, `foregroundServiceType`, `Property`
- `service/TimerForegroundService.kt`: `NotificationManager`, `Intent` actions (`ACTION_START_SERVICE`, `ACTION_SYNC`, `ACTION_STOP_SERVICE`)
- `AndroidManifest.xml`: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `VIBRATE`, `showWhenLocked`, `turnScreenOn`, `foregroundServiceType`, service definition

### C. System Services / Hardware Access
- `ui/utils/HapticHelper.kt`: `Context.getSystemService(VIBRATOR_SERVICE)`, `Vibrator`, `VibrationEffect.createPredefined()`, `Build.VERSION.SDK_INT`
- `data/AppSettings.kt`: `Context.MODE_PRIVATE`
- `service/NotificationHelper.kt`: `NotificationManagerCompat.from(context).areNotificationsEnabled()`

### D. Font / Resource References
- `ui/theme/Type.kt`: `Font(R.font.jetbrains_mono_regular, ...)` — relies on Android `res/font` resources. Desktop CMP uses desktop resource loading (e.g., file-based fonts or `compose.resources.Font()`)
- `service/NotificationHelper.kt`: `R.drawable.ic_timer`
- `OledoroApp.kt`: `getString(R.string...)`, `R.drawable.ic_timer`

### E. Permission / Runtime Requests
- `MainActivity.kt`: `Manifest.permission.POST_NOTIFICATIONS`, `registerForActivityResult(ActivityResultContracts.RequestPermission())`, `ContextCompat.checkSelfPermission()`, `PackageManager.PERMISSION_GRANTED`

---

## 3. Settings Persistence

Current mechanism (`data/AppSettings.kt`):
- `SharedPreferences` (`context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)`)
- Keys: `key_theme_color`, `key_break_color`, `key_negative_color`, `key_dimming_active`, `key_dim_percentage`, `key_focus_minutes`, `key_short_break_minutes`, `key_long_break_minutes`, `key_long_break_interval`, `key_auto_brighten_on_finish`
- Values stored as `String` (`GruvboxColor.name`) or `Int`/`Boolean`
- Exposed as `StateFlow` fields (`themeColor`, `breakColor`, `negativeColor`, `isDimmingActive`, `dimPercentage`, `focusMinutes`, etc.)
- Default values defined in `AppSettingsManager.Companion`

**Desktop replacement needed**: Replace `SharedPreferences` with desktop file-based storage (e.g., `java.util.Properties`, Kotlin `DataStore`, or JSON file in user config directory). The `AppSettingsManager` constructor takes `Context` — must be abstracted.

---

## 4. Build Configuration (Desktop-Related Notes)

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
```
- `compileOptions { JavaVersion.VERSION_17 }`
- `kotlinOptions { jvmTarget = "17" }`
- `buildFeatures { compose = true }`

**Desktop migration requirements**:
- Change plugin from `com.android.application` to `org.jetbrains.compose` (desktop plugin)
- Add desktop source set (`desktopMain`, `desktopTest`)
- Replace `androidx.activity:activity-compose` dependency with desktop-specific entry point (`main()` in desktop `main.kt`)
- Replace `androidx.compose.material3:material3` (available in desktop BOM) — already standard
- Replace `androidx.compose.ui:ui-tooling-preview` (desktop-compatible)
- Replace `androidx.compose.material:material-icons-extended` — available for desktop
- Replace `kotlinx-coroutines-android` with `kotlinx-coroutines-core` (already partially done)
- Remove `buildFeatures { compose = true }` (not needed for desktop plugin) or adapt
- The `build.gradle.kts` uses `Compose BOM 2024.09.00` which supports desktop

---

## 5. UI / Screen Architecture

### Main Activity (`MainActivity.kt`)
- `ComponentActivity` hosts single `AmbientScreen`
- Uses `MainViewModel` (`ViewModel`) with `TimerEngineHolder` singleton for shared timer engine
- Collects 11 `StateFlow` fields via `collectAsStateWithLifecycle()`
- Handles notification permission launch, settings intent navigation, ambient mode setup (`AmbientController.configureShowWhenLocked`)
- Starts/stops `TimerForegroundService` for background timer

### Ambient Screen (`AmbientScreen.kt`)
- Main composable receiving `TimerState`, 3 `GruvboxColor`, booleans/ints, callbacks
- Uses `AmbientModeEffect` (`DisposableEffect`) for brightness, wake lock, immersive mode
- Uses `rememberBurnInShift` (`produceState`) for anti-burn-in offset
- Displays phase label, large timer digits (`TimerDisplay`), next phase preview (`NextPhasePreview`), action controls (`ControlsRow` in `AnimatedVisibility`), notification banner, settings dialog (`SettingsDialog`)

### Settings (`SettingsDialog.kt`)
- Modal `Dialog` with scrollable `Card`
- Sections: 3 color pickers (`GruvboxColor`), 5 sliders (`focus`, `short break`, `long break`, `long break interval`, `dim percentage`), 1 switch (`autoBrightenOnFinish`)
- Sliders use `Material3.Slider` with `SliderDefaults.colors`
- Uses `LocalContext.current` for `HapticHelper` click feedback

---

## 6. Desktop-Specific Replacement Strategy

### Must Replace / Abstract:
1. **Persistence**: `AppSettings.kt` — abstract `Context` dependency; use desktop file storage
2. **Window / Brightness / Lock**: `AmbientController.kt` — desktop has no `WindowManager.LayoutParams` or `FLAG_KEEP_SCREEN_ON`; replace with desktop window API (e.g., `java.awt.Window` or Compose Desktop `Window` / `WindowState`) or remove screen-on/dimming features for desktop (or replace with desktop brightness simulation)
3. **Theme / Window Bars**: `Theme.kt` — `Activity` and `WindowCompat` references must be removed; desktop uses `Window` properties differently
4. **Notifications / Service**: `NotificationHelper.kt`, `TimerForegroundService.kt`, `OledoroApp.kt` — desktop has no Android notification channels or foreground services. Replace with desktop notification APIs (e.g., `java.awt.Toolkit` or desktop Compose notification library, or simply remove for desktop)
5. **Permissions / Runtime**: `MainActivity.kt` — notification permission request, `registerForActivityResult` — remove entirely for desktop
6. **Haptics**: `HapticHelper.kt` — `Vibrator` does not exist on desktop; replace with desktop haptic feedback (if available) or no-op
7. **Resource References**: `R.font.*`, `R.drawable.*`, `getString()` — desktop uses desktop resource system; fonts can be loaded as files (`font/jetbrains_mono_...`) directly in desktop source set, or embedded as resources
8. **Font Loading**: `Type.kt` uses `Font(R.font...)`; desktop Compose supports loading fonts from files or using desktop resource mechanism

### Compose APIs That Transfer Directly:
- `MaterialTheme`, `Material3` components (`IconButton`, `Slider`, `Switch`, `Card`, `Dialog`)
- All layout (`Box`, `Column`, `Row`, `Spacer`)
- All modifiers (`Modifier.background/border/clickable/offset/fillMaxSize/padding/align`)
- Animation (`AnimatedVisibility`, `fadeIn`, `fadeOut`)
- `MutableStateFlow`, `StateFlow`, `SharedFlow`, `CoroutineScope`, `delay`, `produceState`
- `Text`, `Image` (if added), `Modifier.verticalScroll()`

### Key Design Notes for Desktop:
- The timer engine (`TimerEngine`) is pure Kotlin/coroutines — fully desktop-compatible
- The domain layer (`TimerState`, `TimerPhase`, `TimerStatus`, `TimeFormatter`) is pure Kotlin — fully desktop-compatible
- The data layer (`AppSettings`) uses `SharedPreferences` — must be abstracted
- The UI layer relies heavily on `Material3` — fully supported on desktop
- The ambient/window layer (`AmbientController`, `AmbientModeEffect`) is tightly coupled to Android `Activity`/window APIs — must be redesigned or gated for desktop
- The notification/service layer (`TimerForegroundService`, `NotificationHelper`) is Android-only — must be redesigned or removed for desktop
- Font resources (`jetbrains_mono_...`) are in `res/font/` — desktop Compose uses different resource paths; must be relocated or loaded as file-based fonts
