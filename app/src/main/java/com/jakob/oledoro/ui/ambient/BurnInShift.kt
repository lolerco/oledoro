package com.jakob.oledoro.ui.ambient

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents the 2D offset applied to UI elements to prevent OLED burn-in.
 */
data class PixelShiftOffset(
    val xDp: Float = 0f,
    val yDp: Float = 0f
) {
    val x: Dp get() = xDp.dp
    val y: Dp get() = yDp.dp
}

/**
 * Calculates a subtle coordinate translation based on the elapsed step count.
 * Uses out-of-phase sinusoidal patterns to explore a 2D area within [-maxShiftDp, +maxShiftDp].
 */
fun calculatePixelShift(step: Long, maxShiftDp: Float = 6f): PixelShiftOffset {
    val offsetX = (sin(step * 1.7) * maxShiftDp).toFloat()
    val offsetY = (cos(step * 1.3) * maxShiftDp).toFloat()
    return PixelShiftOffset(offsetX, offsetY)
}

/**
 * Compose state producer that updates the pixel shift periodically (every 60 seconds by default).
 */
@Composable
fun rememberBurnInShift(
    intervalMs: Long = 60_000L,
    maxShiftDp: Float = 6f,
    enabled: Boolean = true
): State<PixelShiftOffset> {
    return produceState(initialValue = PixelShiftOffset(0f, 0f), enabled, intervalMs, maxShiftDp) {
        if (!enabled) {
            value = PixelShiftOffset(0f, 0f)
            return@produceState
        }
        var step = 0L
        while (isActive) {
            value = calculatePixelShift(step, maxShiftDp)
            delay(intervalMs)
            step++
        }
    }
}

/**
 * Modifier extension to apply pixel shift translation to prevent subpixel degradation.
 */
fun Modifier.burnInShift(offset: PixelShiftOffset): Modifier {
    return this.burnInShift(offset.x, offset.y)
}

fun Modifier.burnInShift(x: Dp, y: Dp): Modifier {
    return this.offset(x = x, y = y)
}
