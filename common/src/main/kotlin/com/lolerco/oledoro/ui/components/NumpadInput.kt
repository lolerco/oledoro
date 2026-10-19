package com.lolerco.oledoro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.theme.OledBlack
import com.lolerco.oledoro.ui.utils.HapticHelper

@Composable
fun NumpadInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    themeColor: androidx.compose.ui.graphics.Color,
    minValue: Int = 1,
    maxValue: Int = 999,
    unit: String = "min",
    context: Any? = null
) {
    var showNumpad by remember { mutableStateOf(false) }
    var inputBuffer by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Label row with current value display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = AmbientCoolGray,
                letterSpacing = 1.sp
            )
            // Value display field - tappable to open numpad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .clickable {
                        HapticHelper.performClick(context)
                        inputBuffer = value.toString()
                        showNumpad = true
                    }
                    .background(
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = AmbientDimGray,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$value $unit",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = themeColor
                    )
                    // Small indicator that it's tappable
                    Text(
                        text = "▼",
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = AmbientDimGray
                    )
                }
            }
        }

        // Animated Numpad
        AnimatedVisibility(
            visible = showNumpad,
            enter = expandVertically(expandFrom = 0) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            NumpadKeyboard(
                inputBuffer = inputBuffer,
                onInputChange = { inputBuffer = it },
                onConfirm = { newValue ->
                    val clampedValue = newValue.coerceIn(minValue, maxValue)
                    onValueChange(clampedValue)
                    showNumpad = false
                },
                onCancel = {
                    showNumpad = false
                },
                themeColor = themeColor,
                context = context
            )
        }
    }
}

@Composable
private fun NumpadKeyboard(
    inputBuffer: String,
    onInputChange: (String) -> Unit,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit,
    themeColor: androidx.compose.ui.graphics.Color,
    context: Any?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Display current input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Text(
                text = if (inputBuffer.isEmpty()) "0" else inputBuffer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = themeColor,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Numpad grid
        Column {
            // Row 1: 1 2 3
            NumpadRow(
                keys = listOf("1", "2", "3"),
                onKeyPress = { digit ->
                    HapticHelper.performTick(context)
                    onInputChange(inputBuffer + digit)
                },
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: 4 5 6
            NumpadRow(
                keys = listOf("4", "5", "6"),
                onKeyPress = { digit ->
                    HapticHelper.performTick(context)
                    onInputChange(inputBuffer + digit)
                },
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: 7 8 9
            NumpadRow(
                keys = listOf("7", "8", "9"),
                onKeyPress = { digit ->
                    HapticHelper.performTick(context)
                    onInputChange(inputBuffer + digit)
                },
                themeColor = themeColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: backspace, 0, confirm
            NumpadRow(
                keys = listOf("⌫", "0", "✓"),
                onKeyPress = { key ->
                    HapticHelper.performClick(context)
                    when (key) {
                        "⌫" -> {
                            if (inputBuffer.isNotEmpty()) {
                                onInputChange(inputBuffer.dropLast(1))
                            }
                        }
                        "✓" -> {
                            val value = inputBuffer.toIntOrNull() ?: 0
                            onConfirm(value)
                        }
                        else -> onInputChange(inputBuffer + key)
                    }
                },
                themeColor = themeColor,
                isActionRow = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cancel button
        androidx.compose.material3.TextButton(
            onClick = {
                HapticHelper.performClick(context)
                onCancel()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "CANCEL",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = AmbientDimGray,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun NumpadRow(
    keys: List<String>,
    onKeyPress: (String) -> Unit,
    themeColor: androidx.compose.ui.graphics.Color,
    isActionRow: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { key ->
            val isAction = isActionRow && (key == "⌫" || key == "✓")
            val keyColor = if (isAction) themeColor else AmbientCoolGray
            val backgroundColor = if (isAction) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
            val borderColor = if (isAction) themeColor.copy(alpha = 0.5f) else AmbientDimGray

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(backgroundColor, RoundedCornerShape(10.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
                        indication = androidx.compose.foundation.ripple.rememberRipple()
                    ) {
                        onKeyPress(key)
                    }
            ) {
                Text(
                    text = key,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isAction) 20.sp else 24.sp,
                    color = keyColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}