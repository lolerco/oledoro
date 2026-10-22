package com.lolerco.oledoro.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberInputFieldTest {

    @Test
    fun filterDigits_allowsOnlyNumericCharacters() {
        assertEquals("123", NumberInputHelper.filterDigits("123"))
        assertEquals("123", NumberInputHelper.filterDigits("1a2b3c"))
        assertEquals("42", NumberInputHelper.filterDigits("$42 mins"))
        assertEquals("15", NumberInputHelper.filterDigits("-15"))
        assertEquals("", NumberInputHelper.filterDigits("abc"))
        assertEquals("", NumberInputHelper.filterDigits("   "))
    }

    @Test
    fun filterDigits_enforcesMaxDigitsLimit() {
        assertEquals("123", NumberInputHelper.filterDigits("12345", maxDigits = 3))
        assertEquals("99", NumberInputHelper.filterDigits("999", maxDigits = 2))
        assertEquals("5", NumberInputHelper.filterDigits("5", maxDigits = 2))
    }

    @Test
    fun filterDigits_handlesEmptyString() {
        assertEquals("", NumberInputHelper.filterDigits(""))
    }

    @Test
    fun parseAndClamp_validNumberWithinRange() {
        assertEquals(25, NumberInputHelper.parseAndClamp("25", minValue = 1, maxValue = 90))
        assertEquals(1, NumberInputHelper.parseAndClamp("1", minValue = 1, maxValue = 90))
        assertEquals(90, NumberInputHelper.parseAndClamp("90", minValue = 1, maxValue = 90))
    }

    @Test
    fun parseAndClamp_clampsAboveMax() {
        assertEquals(90, NumberInputHelper.parseAndClamp("999", minValue = 1, maxValue = 90))
        assertEquals(30, NumberInputHelper.parseAndClamp("45", minValue = 1, maxValue = 30))
    }

    @Test
    fun parseAndClamp_clampsBelowMin() {
        assertEquals(1, NumberInputHelper.parseAndClamp("0", minValue = 1, maxValue = 90))
        assertEquals(5, NumberInputHelper.parseAndClamp("2", minValue = 5, maxValue = 30))
    }

    @Test
    fun parseAndClamp_emptyInputReturnsDefaultClamped() {
        assertEquals(25, NumberInputHelper.parseAndClamp("", minValue = 1, maxValue = 90, default = 25))
        assertEquals(1, NumberInputHelper.parseAndClamp("", minValue = 1, maxValue = 90, default = 0))
    }

    @Test
    fun parseAndClamp_invalidInputReturnsDefaultClamped() {
        assertEquals(15, NumberInputHelper.parseAndClamp("xyz", minValue = 1, maxValue = 90, default = 15))
    }

    @Test
    fun isAllowedDigit_acceptsOnlyDigits() {
        for (c in '0'..'9') {
            assertTrue(NumberInputHelper.isAllowedDigit(c))
        }
        assertFalse(NumberInputHelper.isAllowedDigit('a'))
        assertFalse(NumberInputHelper.isAllowedDigit('-'))
        assertFalse(NumberInputHelper.isAllowedDigit('.'))
        assertFalse(NumberInputHelper.isAllowedDigit(' '))
    }

    @Test
    fun isValidInput_checksDigitsAndLength() {
        assertTrue(NumberInputHelper.isValidInput("25", maxDigits = 3))
        assertTrue(NumberInputHelper.isValidInput("123", maxDigits = 3))
        assertTrue(NumberInputHelper.isValidInput("", maxDigits = 3))
        assertFalse(NumberInputHelper.isValidInput("1234", maxDigits = 3))
        assertFalse(NumberInputHelper.isValidInput("25a", maxDigits = 3))
    }
}
