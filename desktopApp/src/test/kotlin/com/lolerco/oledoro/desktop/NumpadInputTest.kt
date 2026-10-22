package com.lolerco.oledoro.desktop

import com.lolerco.oledoro.ui.components.NumberInputHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumpadInputTest {

    @Test
    fun numberInput_allowsOnlyNumericCharacters() {
        assertEquals("123", NumberInputHelper.filterDigits("123"))
        assertEquals("123", NumberInputHelper.filterDigits("1a2b3c"))
        assertEquals("42", NumberInputHelper.filterDigits("$42 mins"))
    }

    @Test
    fun numberInput_clampsToMax() {
        val minValue = 1
        val maxValue = 90
        val clamped = NumberInputHelper.parseAndClamp("999", minValue, maxValue)
        assertEquals(90, clamped)
    }

    @Test
    fun numberInput_clampsToMin() {
        val minValue = 1
        val maxValue = 90
        val clamped = NumberInputHelper.parseAndClamp("0", minValue, maxValue)
        assertEquals(1, clamped)
    }

    @Test
    fun numberInput_parseValidNumber() {
        val parsed = NumberInputHelper.parseAndClamp("25", 1, 90)
        assertEquals(25, parsed)
    }

    @Test
    fun numberInput_parseEmptyNumber() {
        val parsed = NumberInputHelper.parseAndClamp("", 1, 90, default = 1)
        assertEquals(1, parsed)
    }

    @Test
    fun numberInput_parseInvalidNumber() {
        val parsed = NumberInputHelper.parseAndClamp("abc", 1, 90, default = 1)
        assertEquals(1, parsed)
    }

    @Test
    fun numberInput_enforcesMaxDigitsLimit() {
        val filtered = NumberInputHelper.filterDigits("12345", maxDigits = 3)
        assertEquals("123", filtered)
    }

    @Test
    fun numberInput_validatesDigitsOnly() {
        assertTrue(NumberInputHelper.isValidInput("25", maxDigits = 3))
        assertFalse(NumberInputHelper.isValidInput("25a", maxDigits = 3))
        assertFalse(NumberInputHelper.isValidInput("1234", maxDigits = 3))
    }
}