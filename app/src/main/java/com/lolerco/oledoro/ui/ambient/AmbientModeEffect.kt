package com.lolerco.oledoro.ui.ambient

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * Applies the OLED dimming brightness to the Activity Window.
 */
@Composable
fun AmbientModeEffect(
    isDimmingActive: Boolean,
    dimPercentage: Int
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    val targetBrightness = if (isDimmingActive) {
        (dimPercentage.coerceIn(5, 95) / 100f).coerceAtLeast(0.01f)
    } else {
        WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }

    val animatedBrightness by animateFloatAsState(
        targetValue = targetBrightness,
        animationSpec = tween(durationMillis = 300),
        label = "ambientBrightness"
    )

    LaunchedEffect(animatedBrightness) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = animatedBrightness
        activity.window.attributes = layoutParams
    }

    DisposableEffect(Unit) {
        onDispose {
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = layoutParams
        }
    }
}

data class PixelShiftOffset(val x: Float, val y: Float)

@Composable
fun rememberBurnInShift(): State<PixelShiftOffset> {
    val offset = remember { mutableStateOf(PixelShiftOffset(0f, 0f)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            offset.value = PixelShiftOffset(
                x = (-2..2).random().toFloat(),
                y = (-2..2).random().toFloat()
            )
        }
    }
    return offset
}

fun Modifier.burnInShift(offset: State<PixelShiftOffset>): Modifier = graphicsLayer {
    translationX = offset.value.x
    translationY = offset.value.y
}
