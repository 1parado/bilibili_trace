package dev.paradox.trace.domain.collection

import dev.paradox.trace.core.time.IntervalMath
import dev.paradox.trace.domain.model.TimeInterval

/**
 * Pure planner: raw foreground transitions -> observed usage intervals.
 * No Android classes, fully unit-testable, and honest about gaps: a session
 * that is still open when the query window ends is skipped entirely instead
 * of being closed at an artificial boundary (it is captured on a later sync).
 */
object UsageSessionPlanner {

    data class PlannedUsageSession(
        val packageName: String,
        val interval: TimeInterval,
    )

    /**
     * Builds observed sessions from [events]. Intervals shorter than
     * [minDurationMs] are dropped as transition noise. Overlapping or
     * adjacent intervals for the same package are merged so covered time
     * counts once. Events outside `[windowStartMs, windowEndMs)` are ignored.
     */
    fun plan(
        events: List<ForegroundUsageSource.ForegroundEvent>,
        targetPackages: Set<String>,
        windowStartMs: Long,
        windowEndMs: Long,
        minDurationMs: Long = DEFAULT_MIN_DURATION_MS,
    ): List<PlannedUsageSession> {
        val result = mutableListOf<PlannedUsageSession>()
        val relevant = events
            .filter { it.packageName in targetPackages }
            .filter { it.timestampMs >= windowStartMs && it.timestampMs < windowEndMs }
            .groupBy { it.packageName }

        for ((packageName, packageEvents) in relevant) {
            val intervals = mutableListOf<TimeInterval>()
            var openStartMs: Long? = null
            for (event in packageEvents.sortedBy { it.timestampMs }) {
                if (event.isForeground) {
                    // A duplicate resume without a pause keeps the earlier start.
                    if (openStartMs == null) openStartMs = event.timestampMs
                } else {
                    val start = openStartMs
                    if (start != null && event.timestampMs >= start) {
                        intervals += TimeInterval(start, event.timestampMs)
                    }
                    openStartMs = null
                    // A pause without a matching resume has nothing to close.
                }
            }
            // An interval still open at window end is intentionally skipped.

            IntervalMath.merge(intervals)
                .filter { it.durationMs >= minDurationMs }
                .forEach { result += PlannedUsageSession(packageName, it) }
        }
        return result.sortedBy { it.interval.startInclusiveMs }
    }

    const val DEFAULT_MIN_DURATION_MS: Long = 5_000L
}
