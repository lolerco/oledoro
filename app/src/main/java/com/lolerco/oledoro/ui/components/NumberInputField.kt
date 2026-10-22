package com.lolerco.oledoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolerco.oledoro.ui.theme.AmbientCoolGray
import com.lolerco.oledoro.ui.theme.AmbientDimGray
import com.lolerco.oledoro.ui.theme.JetBrainsMono
import com.lolerco.oledoro.ui.utils.HapticHelper

object NumberInputHelper {
    fun filterDigits(input: String, maxDigits: Int = 3): String {
        return input.filter { it in '0'..'9' }.take(maxDigits)
    }

    fun parseAndClamp(
        input: String,
        minValue: Int,
        maxValue: Int,
        default: Int = minValue
    ): Int {
        val parsed = input.toIntOrNull() ?: default
        return parsed.coerceIn(minValue, maxValue)
    }

    fun isAllowedDigit(char: Char): Boolean = char in '0'..'9'

    fun isValidInput(text: String, maxDigits: Int = 3): Boolean {
        return text.length <= maxDigits && text.all { isAllowedDigit(it) }
    }
}

@Composable
fun NumberInputField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color,
    minValue: Int = 1,
    maxValue: Int = 999,
    unit: String = "min",
    context: Any? = null
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    val maxDigits = maxOf(maxValue.toString().length, 3)

    LaunchedEffect(value) {
        if (!isFocused) {
            textValue = value.toString()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
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

        val borderColor = if (isFocused) themeColor else AmbientDimGray
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusRequester.requestFocus()
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newText ->
                        val digits = NumberInputHelper.filterDigits(newText, maxDigits = maxDigits)
                        textValue = digits
                        if (digits.isNotEmpty()) {
                            val parsed = digits.toIntOrNull()
                            if (parsed != null) {
                                onValueChange(parsed.coerceIn(minValue, maxValue))
                            }
                        }
                    },
                    modifier = Modifier
                        .widthIn(min = 28.dp, max = 42.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused != isFocused) {
                                isFocused = focusState.isFocused
                                if (isFocused) {
                                    HapticHelper.performClick(context)
                                } else {
                                    val parsed = NumberInputHelper.parseAndClamp(
                                        textValue,
                                        minValue,
                                        maxValue,
                                        default = value
                                    )
                                    textValue = parsed.toString()
                                    if (parsed != value) {
                                        onValueChange(parsed)
                                    }
                                }
                            }
                        },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = themeColor,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(themeColor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = NumberInputHelper.parseAndClamp(
                                textValue,
                                minValue,
                                maxValue,
                                default = value
                            )
                            textValue = parsed.toString()
                            onValueChange(parsed)
                            focusManager.clearFocus()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (textValue.isEmpty()) {
                                Text(
                                    text = "0",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AmbientDimGray,
                                    textAlign = TextAlign.End
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = AmbientCoolGray
                    )
                }
            }
        }
    }
}

@Composable
fun NumpadInput(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color,
    minValue: Int = 1,
    maxValue: Int = 999,
    unit: String = "min",
    context: Any? = null
) {
    NumberInputField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        themeColor = themeColor,
        minValue = minValue,
        maxValue = maxValue,
        unit = unit,
        context = context
    )
}
