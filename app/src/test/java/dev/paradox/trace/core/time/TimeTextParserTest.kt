package dev.paradox.trace.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeTextParserTest {

    @Test
    fun `parses two digit clock times`() {
        assertEquals(0, TimeTextParser.parseMinutesOfDay("00:00"))
        assertEquals(545, TimeTextParser.parseMinutesOfDay("09:05"))
        assertEquals(1439, TimeTextParser.parseMinutesOfDay("23:59"))
    }

    @Test
    fun `tolerates single digit hour`() {
        assertEquals(540, TimeTextParser.parseMinutesOfDay("9:00"))
    }

    @Test
    fun `rejects out of range and malformed input`() {
        assertNull(TimeTextParser.parseMinutesOfDay("24:00"))
        assertNull(TimeTextParser.parseMinutesOfDay("12:60"))
        assertNull(TimeTextParser.parseMinutesOfDay("12"))
        assertNull(TimeTextParser.parseMinutesOfDay("ab:cd"))
        assertNull(TimeTextParser.parseMinutesOfDay(""))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(600, TimeTextParser.parseMinutesOfDay(" 10:00 "))
    }
}
