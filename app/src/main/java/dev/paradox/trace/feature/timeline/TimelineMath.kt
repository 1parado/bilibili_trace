package dev.paradox.trace.feature.timeline

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Day-window arithmetic for the 24h timeline. Windows are half-open
 * `[dayStartMs, dayEndMs)` in the user's local zone.
 */
object TimelineMath {

    const val MAX_PAST_DAYS: Int = 29

    fun dayWindowMs(date: LocalDate, zone: ZoneId): Pair<Long, Long> {
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end: Long = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun dateForOffset(now: ZonedDateTime, dayOffset: Int): LocalDate =
        now.toLocalDate().minusDays(dayOffset.toLong())

    /** Clamps [dayOffset] into the allowed past range `[0, -MAX_PAST_DAYS]`. */
    fun clampOffset(dayOffset: Int): Int = dayOffset.coerceIn(-MAX_PAST_DAYS, 0)

    /** Overlap fraction of [sessionStartMs, sessionEndMs) within the window, 0..1 each side. */
    fun overlapWithinWindow(
        sessionStartMs: Long,
        sessionEndMs: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Pair<Float, Float>? {
        val from = (sessionStartMs - windowStartMs).toFloat() / (windowEndMs - windowStartMs)
        val to = (sessionEndMs - windowStartMs).toFloat() / (windowEndMs - windowStartMs)
        val clampedFrom = from.coerceIn(0f, 1f)
        val clampedTo = to.coerceIn(0f, 1f)
        return if (clampedTo > clampedFrom) clampedFrom to clampedTo else null
    }
}
