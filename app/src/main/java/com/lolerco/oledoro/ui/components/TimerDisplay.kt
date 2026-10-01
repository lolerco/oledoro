package com.lolerco.oledoro.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.domain.TimeFormatter
import com.lolerco.oledoro.ui.theme.AmbientAmber
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.theme.OvertimeRed

/**
 * Large, crisp, clean JetBrains Mono digits for the timer display.
 * Renders positive countdown (e.g. "24:59", "00:00") and negative overtime (e.g. "-00:01", "-01:23").
 */
@Composable
fun TimerDisplay(
    remainingMs: Long,
    modifier: Modifier = Modifier,
    isOvertime: Boolean = remainingMs < 0,
    fontSize: TextUnit = 76.sp,
    color: Color = if (isOvertime) OvertimeRed else AmbientAmber
) {
    val formattedTime = TimeFormatter.format(remainingMs)

    Text(
        text = formattedTime,
        modifier = modifier,
        color = color,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = (-1).sp,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}
