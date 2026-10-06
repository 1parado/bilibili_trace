package dev.paradox.trace.feature.timeline

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineMathTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun localMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `day window is local midnight to midnight`() {
        val (start, end) = TimelineMath.dayWindowMs(LocalDate.of(2026, 10, 6), zone)
        assertEquals(localMs(2026, 10, 6, 0, 0), start)
        assertEquals(localMs(2026, 10, 7, 0, 0), end)
    }

    @Test
    fun `dateForOffset moves back in time`() {
        val now = ZonedDateTime.of(2026, 10, 6, 20, 0, 0, 0, zone)
        assertEquals(LocalDate.of(2026, 10, 6), TimelineMath.dateForOffset(now, 0))
        assertEquals(LocalDate.of(2026, 10, 5), TimelineMath.dateForOffset(now, 1))
    }

    @Test
    fun `clampOffset limits navigation to 29 days back`() {
        assertEquals(0, TimelineMath.clampOffset(0))
        assertEquals(-29, TimelineMath.clampOffset(-100))
        assertEquals(0, TimelineMath.clampOffset(5))
    }

    @Test
    fun `overlap clips sessions crossing day boundaries`() {
        val windowStart = localMs(2026, 10, 6, 0, 0)
        val windowEnd = localMs(2026, 10, 7, 0, 0)
        val span = (windowEnd - windowStart).toFloat()

        // Session crossing midnight into the day: [23:00 prev, 01:00 today] -> [0, 1/24].
        val crossing = TimelineMath.overlapWithinWindow(
            windowStart - 3_600_000L,
            windowStart + 3_600_000L,
            windowStart,
            windowEnd,
        )
        assertEquals(0f, crossing?.first)
        assertEquals(3_600_000L / span, crossing?.second, 1e-6f)

        // Session entirely before the window yields nothing.
        assertNull(
            TimelineMath.overlapWithinWindow(
                windowStart - 7_200_000L,
                windowStart - 3_600_000L,
                windowStart,
                windowEnd,
            ),
        )
    }
}
