package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lolerco.oledoro.ui.theme.AmbientAmber
import com.lolerco.oledoro.ui.theme.AmbientDimGray

/**
 * Minimalist dot indicator for Pomodoro rounds (e.g., ● ● ○ ○ for round 2 of 4).
 * Pure OLED design: filled dots for completed/current rounds, dimmed dots for remaining rounds.
 */
@Composable
fun RoundIndicator(
    currentRound: Int,
    totalRounds: Int = 4,
    modifier: Modifier = Modifier,
    activeColor: Color = AmbientAmber,
    inactiveColor: Color = AmbientDimGray,
    dotSize: Dp = 8.dp,
    spacing: Dp = 10.dp
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (round in 1..totalRounds) {
            val isCompletedOrCurrent = round <= currentRound
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = if (isCompletedOrCurrent) activeColor else inactiveColor,
                        shape = CircleShape
                    )
            )
        }
    }
}
