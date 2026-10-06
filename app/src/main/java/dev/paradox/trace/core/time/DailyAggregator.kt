package dev.paradox.trace.core.time

import dev.paradox.trace.domain.model.TimeInterval
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Buckets intervals into user-local calendar days.
 *
 * Intervals are half-open `[start, end)`; a segment crossing local midnight
 * is split at the boundary so each day only counts its own share. DST-aware:
 * day boundaries are resolved via [ZoneId] rules.
 */
object DailyAggregator {

    data class DailyTotal(
        val dateKey: String,
        val totalMs: Long,
        val intervalCount: Int,
    )

    private val DATE_KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Buckets the given intervals, returning totals keyed by `yyyy-MM-dd`.
     *  Overlapping intervals are merged first so covered time counts once. */
    fun totalsByLocalDate(
        intervals: List<TimeInterval>,
        zone: ZoneId,
    ): Map<String, DailyTotal> {
        val totals = LinkedHashMap<String, DailyTotal>()
        for (interval in IntervalMath.merge(intervals)) {
            for ((dateKey, segment) in splitByLocalDate(interval, zone)) {
                val existing = totals[dateKey]
                totals[dateKey] = if (existing == null) {
                    DailyTotal(dateKey, segment.durationMs, 1)
                } else {
                    existing.copy(
                        totalMs = existing.totalMs + segment.durationMs,
                        intervalCount = existing.intervalCount + 1,
                    )
                }
            }
        }
        return totals
    }

    /**
     * Splits [interval] into per-local-date segments. Zero-length intervals
     * produce no segments because they occupy no observable time.
     */
    fun splitByLocalDate(
        interval: TimeInterval,
        zone: ZoneId,
    ): List<Pair<String, TimeInterval>> {
        if (interval.isZeroLength) return emptyList()

        val segments = mutableListOf<Pair<String, TimeInterval>>()
        var cursor = interval.startInclusiveMs
        val end = interval.endExclusiveMs
        while (cursor < end) {
            val dateKey = dateKeyOf(cursor, zone)
            val nextMidnightMs = nextLocalMidnightMs(cursor, zone)
            val segmentEnd = minOf(end, nextMidnightMs)
            segments += dateKey to TimeInterval(cursor, segmentEnd)
            cursor = segmentEnd
        }
        return segments
    }

    fun dateKeyOf(epochMs: Long, zone: ZoneId): String =
        DATE_KEY_FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate())

    private fun nextLocalMidnightMs(epochMs: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(epochMs)
            .atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
}
