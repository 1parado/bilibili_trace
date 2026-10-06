package dev.paradox.trace.core.time

import dev.paradox.trace.domain.model.TimeInterval

/**
 * Pure interval arithmetic. All functions are side-effect free so analytics
 * results remain explainable and unit-testable, per docs/ANALYTICS.md.
 */
object IntervalMath {

    /**
     * Returns intervals sorted by start, with overlapping or adjacent
     * intervals merged and zero-length intervals dropped.
     */
    fun merge(intervals: List<TimeInterval>): List<TimeInterval> {
        if (intervals.isEmpty()) return emptyList()

        val candidates = intervals
            .filterNot { it.isZeroLength }
            .sortedWith(compareBy({ it.startInclusiveMs }, { it.endExclusiveMs }))
        if (candidates.isEmpty()) return emptyList()

        val merged = mutableListOf<TimeInterval>()
        var current = candidates.first()
        for (next in candidates.subList(1, candidates.size)) {
            if (next.startInclusiveMs <= current.endExclusiveMs) {
                current = TimeInterval(
                    startInclusiveMs = current.startInclusiveMs,
                    endExclusiveMs = maxOf(current.endExclusiveMs, next.endExclusiveMs),
                )
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        return merged
    }

    /** Total covered time after merging; overlapping time is counted once. */
    fun unionDurationMs(intervals: List<TimeInterval>): Long =
        merge(intervals).sumOf { it.durationMs }

    /** Returns the overlap of [a] and [b], or null when they do not intersect. */
    fun intersect(a: TimeInterval, b: TimeInterval): TimeInterval? {
        val start = maxOf(a.startInclusiveMs, b.startInclusiveMs)
        val end = minOf(a.endExclusiveMs, b.endExclusiveMs)
        return if (start < end) TimeInterval(start, end) else null
    }
}
