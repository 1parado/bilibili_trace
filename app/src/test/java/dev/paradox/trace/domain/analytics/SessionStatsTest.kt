package dev.paradox.trace.domain.analytics

import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.model.TimeInterval
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStatsTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun localMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun session(id: String, startMs: Long, endMs: Long) = ContentSession(
        id = id,
        platform = "bilibili",
        interval = TimeInterval(startMs, endMs),
        source = EventSource.USER_INPUT,
        completeness = SessionCompleteness.COMPLETE,
        content = null,
    )

    @Test
    fun `overlapping sessions today count union once`() {
        // 2026-10-06 local 10:00-11:00 and 10:30-11:30.
        val sessions = listOf(
            session("a", localMs(2026, 10, 6, 10, 0), localMs(2026, 10, 6, 11, 0)),
            session("b", localMs(2026, 10, 6, 10, 30), localMs(2026, 10, 6, 11, 30)),
        )
        val stats = SessionStats.compute(sessions, localMs(2026, 10, 6, 20, 0), zone)
        assertEquals(5_400_000L, stats.todayMs)
        assertEquals(2, stats.todaySessionCount)
    }

    @Test
    fun `last7Days includes today and six previous local dates`() {
        val sessions = listOf(
            session("today", localMs(2026, 10, 6, 9, 0), localMs(2026, 10, 6, 10, 0)),
            session("weekAgo", localMs(2026, 9, 30, 9, 0), localMs(2026, 9, 30, 10, 0)),
            session("tooOld", localMs(2026, 9, 29, 9, 0), localMs(2026, 9, 29, 10, 0)),
        )
        val stats = SessionStats.compute(sessions, localMs(2026, 10, 6, 20, 0), zone)
        assertEquals(2 * 3_600_000L, stats.last7DaysMs)
        assertEquals(7, stats.dailyTotalsMs.size)
        assertEquals("2026-09-30", stats.dailyTotalsMs.first().first)
        assertEquals(3_600_000L, stats.dailyTotalsMs.first().second)
        assertEquals(3_600_000L, stats.dailyTotalsMs.last().second)
    }

    @Test
    fun `no sessions yields zeroed stats`() {
        val stats = SessionStats.compute(emptyList(), localMs(2026, 10, 6, 20, 0), zone)
        assertEquals(0L, stats.todayMs)
        assertEquals(0L, stats.last7DaysMs)
        assertEquals(0, stats.todaySessionCount)
        assertEquals(7, stats.dailyTotalsMs.size)
    }
}
