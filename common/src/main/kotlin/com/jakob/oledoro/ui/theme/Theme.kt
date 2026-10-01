package com.jakob.oledoro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PureBlackColorScheme = darkColorScheme(
    primary = Color(0xFFCC9933),
    onPrimary = OledBlack,
    primaryContainer = OledBlack,
    onPrimaryContainer = Color(0xFFCC9933),
    secondary = AmbientCoolGray,
    onSecondary = OledBlack,
    secondaryContainer = OledBlack,
    onSecondaryContainer = AmbientCoolGray,
    tertiary = Color(0xFFBBBBBB),
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = Color(0xFFBBBBBB),
    surface = OledBlack,
    onSurface = Color(0xFFBBBBBB),
    surfaceVariant = OledBlack,
    onSurfaceVariant = AmbientCoolGray,
    error = Color(0xFFCC4444),
    onError = OledBlack,
    outline = AmbientDimGray,
    outlineVariant = AmbientDarkGray
)

@Composable
fun OledPomodoroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PureBlackColorScheme,
        typography = Typography,
        content = content
    )
}
