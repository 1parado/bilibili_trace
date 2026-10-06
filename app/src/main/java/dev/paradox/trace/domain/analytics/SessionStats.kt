package dev.paradox.trace.domain.analytics

import dev.paradox.trace.core.time.DailyAggregator
import dev.paradox.trace.core.time.IntervalMath
import dev.paradox.trace.domain.model.ContentSession
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Deterministic session statistics. Overlapping sessions are unioned before
 * summing so covered time counts once (docs/ANALYTICS.md rule 4).
 */
object SessionStats {

    data class Stats(
        val todayMs: Long,
        val last7DaysMs: Long,
        val todaySessionCount: Int,
        /** Totals for the last 7 local dates, oldest first. */
        val dailyTotalsMs: List<Pair<String, Long>>,
    )

    fun compute(sessions: List<ContentSession>, nowMs: Long, zone: ZoneId): Stats {
        val merged = IntervalMath.merge(sessions.map { it.interval })
        val totals = DailyAggregator.totalsByLocalDate(merged, zone)

        val now: java.time.Instant = java.time.Instant.ofEpochMilli(nowMs)
        val today = now.atZone(zone).toLocalDate()
        val todayKey = DailyAggregator.dateKeyOf(nowMs, zone)
        val weekKeys = (6 downTo 0).map { offset ->
            today.minusDays(offset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
        val weekMs = weekKeys.sumOf { totals[it]?.totalMs ?: 0L }

        val todayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEndMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todaySessionCount = sessions.count { session ->
            session.interval.startInclusiveMs < todayEndMs &&
                session.interval.endExclusiveMs > todayStartMs
        }

        return Stats(
            todayMs = totals[todayKey]?.totalMs ?: 0L,
            last7DaysMs = weekMs,
            todaySessionCount = todaySessionCount,
            dailyTotalsMs = weekKeys.map { it to (totals[it]?.totalMs ?: 0L) },
        )
    }
}
