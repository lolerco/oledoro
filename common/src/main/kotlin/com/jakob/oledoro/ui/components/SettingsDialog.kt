package com.jakob.oledoro.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jakob.oledoro.data.GruvboxColor
import com.jakob.oledoro.ui.theme.AmbientCoolGray
import com.jakob.oledoro.ui.theme.AmbientDimGray
import com.jakob.oledoro.ui.theme.JetBrainsMono
import com.jakob.oledoro.ui.theme.OledBlack
import com.jakob.oledoro.ui.utils.HapticHelper
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    selectedThemeColor: GruvboxColor,
    onThemeColorChange: (GruvboxColor) -> Unit,
    selectedBreakColor: GruvboxColor = GruvboxColor.AQUA,
    onBreakColorChange: (GruvboxColor) -> Unit = {},
    selectedNegativeColor: GruvboxColor = GruvboxColor.RED,
    onNegativeColorChange: (GruvboxColor) -> Unit = {},
    dimPercentage: Int,
    onDimPercentageChange: (Int) -> Unit,
    focusMinutes: Int,
    onFocusMinutesChange: (Int) -> Unit,
    shortBreakMinutes: Int,
    onShortBreakMinutesChange: (Int) -> Unit,
    longBreakMinutes: Int,
    onLongBreakMinutesChange: (Int) -> Unit,
    longBreakInterval: Int,
    onLongBreakIntervalChange: (Int) -> Unit,
    autoBrightenOnFinish: Boolean,
    onAutoBrightenOnFinishChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = null
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OledBlack),
            border = androidx.compose.foundation.BorderStroke(1.dp, AmbientDimGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = selectedThemeColor.color,
                        letterSpacing = 2.sp
                    )
                    IconButton(
                        onClick = {
                            HapticHelper.performClick(context)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AmbientCoolGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Accent Color (Gruvbox)
                ColorPickerSection(
                    title = "MAIN ACCENT COLOR",
                    selectedColor = selectedThemeColor,
                    onColorSelected = onThemeColorChange,
                    context = context
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Break Timer Color (Gruvbox)
                ColorPickerSection(
                    title = "BREAK TIMER COLOR",
                    selectedColor = selectedBreakColor,
                    onColorSelected = onBreakColorChange,
                    context = context
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Negative / Overtime Color (Gruvbox)
                ColorPickerSection(
                    title = "NEGATIVE TIMER COLOR",
                    selectedColor = selectedNegativeColor,
                    onColorSelected = onNegativeColorChange,
                    context = context
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Focus Duration Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOCUS TIME",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$focusMinutes min",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = selectedThemeColor.color
                    )
                }

                Slider(
                    value = focusMinutes.toFloat(),
                    onValueChange = { floatVal ->
                        val newInt = floatVal.roundToInt()
                        if (newInt != focusMinutes) {
                            HapticHelper.performTick(context)
                            onFocusMinutesChange(newInt)
                        }
                    },
                    valueRange = 1f..90f,
                    steps = 88,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedThemeColor.color,
                        activeTrackColor = selectedThemeColor.color,
                        inactiveTrackColor = AmbientDimGray,
                        activeTickColor = OledBlack,
                        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Short Break Duration Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHORT BREAK",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$shortBreakMinutes min",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = selectedThemeColor.color
                    )
                }

                Slider(
                    value = shortBreakMinutes.toFloat(),
                    onValueChange = { floatVal ->
                        val newInt = floatVal.roundToInt()
                        if (newInt != shortBreakMinutes) {
                            HapticHelper.performTick(context)
                            onShortBreakMinutesChange(newInt)
                        }
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedThemeColor.color,
                        activeTrackColor = selectedThemeColor.color,
                        inactiveTrackColor = AmbientDimGray,
                        activeTickColor = OledBlack,
                        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Long Break Duration Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LONG BREAK",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$longBreakMinutes min",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = selectedThemeColor.color
                    )
                }

                Slider(
                    value = longBreakMinutes.toFloat(),
                    onValueChange = { floatVal ->
                        val newInt = floatVal.roundToInt()
                        if (newInt != longBreakMinutes) {
                            HapticHelper.performTick(context)
                            onLongBreakMinutesChange(newInt)
                        }
                    },
                    valueRange = 1f..60f,
                    steps = 58,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedThemeColor.color,
                        activeTrackColor = selectedThemeColor.color,
                        inactiveTrackColor = AmbientDimGray,
                        activeTickColor = OledBlack,
                        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Long Break Interval Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LONG BREAK INTERVAL",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$longBreakInterval ${if (longBreakInterval == 1) "round" else "rounds"}",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = selectedThemeColor.color
                    )
                }

                Slider(
                    value = longBreakInterval.toFloat(),
                    onValueChange = { floatVal ->
                        val newInt = floatVal.roundToInt()
                        if (newInt != longBreakInterval) {
                            HapticHelper.performTick(context)
                            onLongBreakIntervalChange(newInt)
                        }
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedThemeColor.color,
                        activeTrackColor = selectedThemeColor.color,
                        inactiveTrackColor = AmbientDimGray,
                        activeTickColor = OledBlack,
                        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Brighten on Finish Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AUTO LIGHTBULB",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = AmbientCoolGray,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "dim when session starts, light up when finished",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = AmbientDimGray
                        )
                    }
                    Switch(
                        checked = autoBrightenOnFinish,
                        onCheckedChange = {
                            HapticHelper.performClick(context)
                            onAutoBrightenOnFinishChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = selectedThemeColor.color,
                            checkedTrackColor = selectedThemeColor.color.copy(alpha = 0.4f),
                            uncheckedThumbColor = AmbientDimGray,
                            uncheckedTrackColor = OledBlack
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dim Percentage Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DIM LEVEL",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$dimPercentage%",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = selectedThemeColor.color
                    )
                }

                Slider(
                    value = dimPercentage.toFloat(),
                    onValueChange = { floatVal ->
                        val newInt = floatVal.roundToInt()
                        if (newInt != dimPercentage) {
                            HapticHelper.performTick(context)
                            onDimPercentageChange(newInt)
                        }
                    },
                    valueRange = 1f..50f,
                    steps = 48,
                    colors = SliderDefaults.colors(
                        thumbColor = selectedThemeColor.color,
                        activeTrackColor = selectedThemeColor.color,
                        inactiveTrackColor = AmbientDimGray,
                        activeTickColor = OledBlack,
                        inactiveTickColor = selectedThemeColor.color.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Credits and Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "oledoro",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "minimalist, battery-saving pomodoro for oled displays.\n\nfont: jetbrains nerd font mono\npalette: gruvbox\ncreated by: lolerco\nversion 1.1",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = AmbientCoolGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerSection(
    title: String,
    selectedColor: GruvboxColor,
    onColorSelected: (GruvboxColor) -> Unit,
    context: Any?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AmbientCoolGray,
            letterSpacing = 1.sp
        )
        Text(
            text = selectedColor.displayName.uppercase(),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = selectedColor.color
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GruvboxColor.entries.forEach { gruvboxColor ->
            val isSelected = gruvboxColor == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(gruvboxColor.color)
                    .clickable {
                        HapticHelper.performClick(context)
                        onColorSelected(gruvboxColor)
                    }
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color.White, CircleShape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "${gruvboxColor.displayName} Selected",
                        tint = OledBlack,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
