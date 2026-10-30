package com.lolerco.oledoro.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.data.GruvboxColor
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono

/**
 * Dialog for creating a new habit with custom name and Gruvbox accent color.
 */
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: GruvboxColor) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(GruvboxColor.YELLOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.Black,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "NEW HABIT",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Habit Name:",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = AmbientCoolGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    placeholder = {
                        Text(
                            text = "e.g., Read 30 mins",
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            color = AmbientDimGray
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 14.sp,
                        color = Color.White
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor.composeColor,
                        unfocusedBorderColor = AmbientDimGray,
                        cursorColor = selectedColor.composeColor
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Accent Color:",
                    fontFamily = JetBrainsMono,
                    fontSize = 12.sp,
                    color = AmbientCoolGray
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Color selection palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GruvboxColor.entries.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.composeColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (habitName.isNotBlank()) {
                        onConfirm(habitName.trim(), selectedColor)
                    }
                },
                enabled = habitName.isNotBlank()
            ) {
                Text(
                    text = "CREATE",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = if (habitName.isNotBlank()) selectedColor.composeColor else AmbientDimGray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    fontFamily = JetBrainsMono,
                    color = AmbientCoolGray
                )
            }
        }
    )
}
