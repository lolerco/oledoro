package com.jakob.oledoro.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `format positive standard durations`() {
        assertEquals("25:00", TimeFormatter.format(25 * 60 * 1000L))
        assertEquals("05:00", TimeFormatter.format(5 * 60 * 1000L))
        assertEquals("15:00", TimeFormatter.format(15 * 60 * 1000L))
    }

    @Test
    fun `format boundary zero`() {
        assertEquals("00:00", TimeFormatter.format(0L))
    }

    @Test
    fun `format positive seconds and minutes`() {
        assertEquals("00:01", TimeFormatter.format(1000L))
        assertEquals("00:59", TimeFormatter.format(59 * 1000L))
        assertEquals("01:00", TimeFormatter.format(60 * 1000L))
        assertEquals("01:23", TimeFormatter.format(83 * 1000L))
    }

    @Test
    fun `format required overtime negative values`() {
        assertEquals("-00:01", TimeFormatter.format(-1000L))
        assertEquals("-01:05", TimeFormatter.format(-65 * 1000L))
        assertEquals("-59:59", TimeFormatter.format(-3599 * 1000L))
    }

    @Test
    fun `format additional overtime values`() {
        assertEquals("-00:02", TimeFormatter.format(-2000L))
        assertEquals("-01:23", TimeFormatter.format(-83 * 1000L))
        assertEquals("-15:30", TimeFormatter.format(-(15 * 60 + 30) * 1000L))
        assertEquals("-60:00", TimeFormatter.format(-3600 * 1000L))
    }

    @Test
    fun `format sub-second values without rounding up`() {
        assertEquals("00:00", TimeFormatter.format(999L))
        assertEquals("00:01", TimeFormatter.format(1999L))
        assertEquals("-00:00", TimeFormatter.format(-500L))
        assertEquals("-00:01", TimeFormatter.format(-1500L))
    }
}
