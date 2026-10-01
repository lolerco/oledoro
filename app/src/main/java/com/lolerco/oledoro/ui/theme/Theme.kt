package com.lolerco.oledoro.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PureBlackColorScheme = darkColorScheme(
    primary = AmbientAmber,
    onPrimary = OledBlack,
    primaryContainer = OledBlack,
    onPrimaryContainer = AmbientAmber,
    secondary = AmbientCoolGray,
    onSecondary = OledBlack,
    secondaryContainer = OledBlack,
    onSecondaryContainer = AmbientCoolGray,
    tertiary = AmbientFaintWhite,
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = AmbientFaintWhite,
    surface = OledBlack,
    onSurface = AmbientFaintWhite,
    surfaceVariant = OledBlack,
    onSurfaceVariant = AmbientCoolGray,
    error = OvertimeRed,
    onError = OledBlack,
    outline = AmbientDimGray,
    outlineVariant = AmbientDarkGray
)

@Composable
fun OledPomodoroTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = OledBlack.toArgb()
                window.navigationBarColor = OledBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PureBlackColorScheme,
        typography = Typography,
        content = content
    )
}
