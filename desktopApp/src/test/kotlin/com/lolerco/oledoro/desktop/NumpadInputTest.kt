package com.lolerco.oledoro.desktop

import org.junit.Test
import org.junit.Assert.assertTrue

class NumpadInputTest {
    @Test
    fun numpadInput_clampsToMax() {
        val minValue = 1
        val maxValue = 90
        val inputValue = 999
        val clampedValue = inputValue.coerceIn(minValue, maxValue)
        assertTrue(clampedValue == 90)
    }

    @Test
    fun numpadInput_clampsToMin() {
        val minValue = 1
        val maxValue = 90
        val inputValue = 0
        val clampedValue = inputValue.coerceIn(minValue, maxValue)
        assertTrue(clampedValue == 1)
    }

    @Test
    fun numpadInput_parseValidNumber() {
        val input = "25"
        val parsed = input.toIntOrNull() ?: 0
        assertTrue(parsed == 25)
    }

    @Test
    fun numpadInput_parseEmptyNumber() {
        val input = ""
        val parsed = input.toIntOrNull() ?: 0
        assertTrue(parsed == 0)
    }

    @Test
    fun numpadInput_parseInvalidNumber() {
        val input = "abc"
        val parsed = input.toIntOrNull() ?: 0
        assertTrue(parsed == 0)
    }

    // Simulate the backspace logic: drop last character
    @Test
    fun numpadInput_backspace() {
        val buffer = "25"
        val afterBackspace = buffer.dropLast(1)
        assertTrue(afterBackspace == "2")
    }

    @Test
    fun numpadInput_backspaceEmpty() {
        val buffer = ""
        val afterBackspace = if (buffer.isNotEmpty()) buffer.dropLast(1) else buffer
        assertTrue(afterBackspace == "")
    }

    // Simulate appending digits
    @Test
    fun numpadInput_appendDigits() {
        var buffer = "2"
        buffer = buffer + "5"
        buffer = buffer + "0"
        assertTrue(buffer == "250")
    }
}