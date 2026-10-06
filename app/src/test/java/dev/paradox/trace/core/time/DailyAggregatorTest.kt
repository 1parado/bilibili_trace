package dev.paradox.trace.core.time

import dev.paradox.trace.domain.model.TimeInterval
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyAggregatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `interval within one day counts entirely to that day`() {
        // 2026-10-06 10:00 - 11:00 local (UTC+8) => 02:00-03:00 UTC
        val interval = TimeInterval(
            utcMs(2026, 10, 6, 2, 0),
            utcMs(2026, 10, 6, 3, 0),
        )
        val totals = DailyAggregator.totalsByLocalDate(listOf(interval), zone)
        assertEquals(1, totals.size)
        assertEquals(3_600_000L, totals.getValue("2026-10-06").totalMs)
        assertEquals(1, totals.getValue("2026-10-06").intervalCount)
    }

    @Test
    fun `interval crossing local midnight is split between days`() {
        // 2026-10-05 23:00 - 2026-10-06 01:00 local => 15:00 UTC + 2h
        val interval = TimeInterval(
            utcMs(2026, 10, 5, 15, 0),
            utcMs(2026, 10, 5, 15, 0) + 2 * 3_600_000L,
        )
        val totals = DailyAggregator.totalsByLocalDate(listOf(interval), zone)
        assertEquals(2, totals.size)
        assertEquals(3_600_000L, totals.getValue("2026-10-05").totalMs)
        assertEquals(3_600_000L, totals.getValue("2026-10-06").totalMs)
    }

    @Test
    fun `multi-day interval attributes each day its own share`() {
        // 2026-10-04 10:00 local, 72h span => ends 2026-10-07 10:00 local.
        val start = localMs(2026, 10, 4, 10, 0)
        val interval = TimeInterval(start, start + 3 * 24 * 3_600_000L)
        val totals = DailyAggregator.totalsByLocalDate(listOf(interval), zone)
        assertEquals(4, totals.size)
        // First day: 10:00 to midnight = 14h; middle days: 24h each; last day: midnight to 10:00 = 10h.
        assertEquals(14 * 3_600_000L, totals.getValue("2026-10-04").totalMs)
        assertEquals(24 * 3_600_000L, totals.getValue("2026-10-05").totalMs)
        assertEquals(24 * 3_600_000L, totals.getValue("2026-10-06").totalMs)
        assertEquals(10 * 3_600_000L, totals.getValue("2026-10-07").totalMs)
    }

    @Test
    fun `overlapping intervals are merged before day attribution`() {
        // Two 1h intervals overlapping the same hour should count once.
        val first = TimeInterval(utcMs(2026, 10, 6, 2, 0), utcMs(2026, 10, 6, 3, 0))
        val second = TimeInterval(utcMs(2026, 10, 6, 2, 30), utcMs(2026, 10, 6, 3, 30))
        val totals = DailyAggregator.totalsByLocalDate(listOf(first, second), zone)
        assertEquals(1, totals.size)
        // 10:00-11:00 and 10:30-11:30 overlap => 1.5h union after merging.
        assertEquals(5_400_000L, totals.getValue("2026-10-06").totalMs)
        assertEquals(1, totals.getValue("2026-10-06").intervalCount)
    }

    @Test
    fun `zero-length interval contributes nothing`() {
        val totals = DailyAggregator.totalsByLocalDate(listOf(TimeInterval(123, 123)), zone)
        assertEquals(0, totals.size)
    }

    @Test
    fun `different zones attribute the same instant to different days`() {
        val shanghai = ZoneId.of("Asia/Shanghai")
        val utc = ZoneId.of("UTC")
        // 2026-10-06 01:00 local Shanghai (2026-10-05 17:00 UTC) spans the UTC date boundary.
        val interval = TimeInterval(
            utcMs(2026, 10, 5, 17, 0),
            utcMs(2026, 10, 5, 17, 0) + 3_600_000L,
        )
        val inShanghai = DailyAggregator.totalsByLocalDate(listOf(interval), shanghai).keys
        val inUtc = DailyAggregator.totalsByLocalDate(listOf(interval), utc).keys
        assertEquals(setOf("2026-10-06"), inShanghai)
        assertEquals(setOf("2026-10-05"), inUtc)
    }

    @Test
    fun `dateKeyOf formats as ISO local date`() {
        assertEquals("2026-10-06", DailyAggregator.dateKeyOf(utcMs(2026, 10, 6, 2, 0), zone))
    }

    private fun utcMs(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
    ): Long = java.time.ZonedDateTime
        .of(year, month, dayOfMonth, hour, minute, 0, 0, java.time.ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    private fun localMs(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
    ): Long = java.time.ZonedDateTime
        .of(year, month, dayOfMonth, hour, minute, 0, 0, zone)
        .toInstant()
        .toEpochMilli()
}
