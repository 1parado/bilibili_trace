package dev.paradox.trace.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Locale

class DurationFormatterTest {
    @Test
    fun formatsZeroDuration() {
        assertEquals("0秒", DurationFormatter.format(0, Locale.CHINA))
    }

    @Test
    fun formatsSecondsAndMinutes() {
        assertEquals("59秒", DurationFormatter.format(59_000, Locale.CHINA))
        assertEquals("1分 01秒", DurationFormatter.format(61_000, Locale.CHINA))
    }

    @Test
    fun formatsHoursWithoutRoundingUpPartialSeconds() {
        assertEquals("1小时 00分", DurationFormatter.format(3_600_999, Locale.CHINA))
        assertEquals("2小时 03分", DurationFormatter.format(7_380_000, Locale.CHINA))
    }

    @Test
    fun rejectsNegativeDurations() {
        assertThrows(IllegalArgumentException::class.java) {
            DurationFormatter.format(-1, Locale.CHINA)
        }
    }
}
