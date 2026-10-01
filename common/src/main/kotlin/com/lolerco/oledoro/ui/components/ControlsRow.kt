package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lolerco.oledoro.domain.TimerStatus
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.utils.HapticHelper

@Composable
fun ControlsRow(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AmbientCoolGray,
    iconSize: Dp = 28.dp,
    spacing: Dp = 24.dp
) {
    val isRunning = status == TimerStatus.RUNNING || status == TimerStatus.OVERTIME

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { HapticHelper.performClick(null); onReset() },
            modifier = Modifier.size(iconSize + 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        IconButton(
            onClick = { HapticHelper.performClick(null); if (isRunning) onPause() else onStart() },
            modifier = Modifier.size(iconSize + 20.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Start",
                tint = tint,
                modifier = Modifier.size(iconSize + 4.dp)
            )
        }

        IconButton(
            onClick = { HapticHelper.performClick(null); onSkip() },
            modifier = Modifier.size(iconSize + 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
