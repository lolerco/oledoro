package com.lolerco.oledoro.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.ui.theme.JetBrainsMono

@Composable
fun NextPhasePreview(
    previewText: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Gray
) {
    Text(
        text = previewText,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp,
        color = color,
        modifier = modifier
    )
}
