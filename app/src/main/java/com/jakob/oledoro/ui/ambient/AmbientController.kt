package com.jakob.oledoro.ui.ambient

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controller responsible for managing window properties during OLED Ambient Mode:
 * - Dynamic hardware luminance control (dimPercentage)
 * - Keeping screen on during active timers
 * - Immersive edge-to-edge fullscreen (hiding system bars)
 * - Displaying over lock screen without recurring unlock prompts
 */
object AmbientController {

    /**
     * Overrides window brightness to specified percentage (clamped 0.01f..1f) or restores default.
     */
    fun setAmbientBrightness(activity: Activity, enable: Boolean, dimPercentage: Int = 5) {
        val layout = activity.window.attributes
        val targetBrightness = (dimPercentage / 100f).coerceIn(0.01f, 1f)
        val newBrightness = if (enable) targetBrightness else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (layout.screenBrightness != newBrightness) {
            layout.screenBrightness = newBrightness
            activity.window.attributes = layout
        }
    }

    /**
     * Keeps screen awake while ambient mode / timer is active.
     */
    fun configureScreenWake(activity: Activity, keepOn: Boolean) {
        if (keepOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Enables displaying activity on top of the lock screen and turns the screen on when needed.
     */
    fun configureShowWhenLocked(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /**
     * Toggles immersive fullscreen mode by hiding/showing status bar and navigation bar.
     */
    fun setImmersiveMode(activity: Activity, immersive: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (immersive) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Exits ambient mode: restores normal brightness, allows screen to sleep, restores system bars.
     */
    fun exitAmbientMode(activity: Activity) {
        val layout = activity.window.attributes
        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity.window.attributes = layout
        configureScreenWake(activity, false)
        setImmersiveMode(activity, false)
    }
}

/**
 * Composable lifecycle effect to safely apply and cleanup ambient mode configuration.
 */
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
