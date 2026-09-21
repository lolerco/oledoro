package com.jakob.oledoro.ui.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jakob.oledoro.R
import com.jakob.oledoro.domain.TimerStatus
import com.jakob.oledoro.ui.theme.AmbientCoolGray
import com.jakob.oledoro.ui.utils.HapticHelper

/**
 * Minimalist action buttons using Compose Material icons:
 * - Play / Pause (PlayArrow / Pause)
 * - Next phase / Skip (SkipNext)
 * - Reset session (Refresh)
 */
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
    val context = LocalContext.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        IconButton(
            onClick = {
                HapticHelper.performClick(context)
                onReset()
            },
            modifier = Modifier.size(iconSize + 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.action_reset),
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }

        // Primary Play / Pause button
        IconButton(
            onClick = {
                HapticHelper.performClick(context)
                if (isRunning) onPause() else onStart()
            },
            modifier = Modifier.size(iconSize + 20.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) {
                    stringResource(R.string.action_pause)
                } else {
                    stringResource(R.string.action_start)
                },
                tint = tint,
                modifier = Modifier.size(iconSize + 4.dp)
            )
        }

        // Next Phase / Skip button
        IconButton(
            onClick = {
                HapticHelper.performClick(context)
                onSkip()
            },
            modifier = Modifier.size(iconSize + 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.action_skip),
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
