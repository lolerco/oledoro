package com.lolerco.oledoro.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Utility for safe tactile and haptic feedback across Android API levels.
 */
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
     * Crisp tactile click feedback for primary buttons and toggles.
     */
    fun performClick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                vibrator.vibrate(effect)
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
     * Subtle micro-tick feedback for slider steps and fine value changes.
     */
    fun performTick(context: Context) {
        try {
            val vibrator = getVibrator(context) ?: return
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator.vibrate(effect)
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
