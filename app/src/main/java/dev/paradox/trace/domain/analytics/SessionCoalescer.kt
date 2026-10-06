package dev.paradox.trace.domain.analytics

import dev.paradox.trace.domain.model.TimeInterval

/**
 * Coalesces nearby intervals into continuous-usage segments for timeline
 * display, per docs/DATA_MODEL.md §4. Raw intervals remain the source of
 * truth; coalescing is a presentation-level projection.
 *
 * Two intervals are joined when the gap between them is strictly less than
 * [gapThresholdMs].
 */
object SessionCoalescer {

    fun coalesce(
        intervals: List<TimeInterval>,
        gapThresholdMs: Long,
    ): List<TimeInterval> {
        require(gapThresholdMs >= 0) { "gapThresholdMs must be non-negative" }
        if (intervals.isEmpty()) return emptyList()
        if (gapThresholdMs == 0L) {
            return dev.paradox.trace.core.time.IntervalMath.merge(intervals)
        }

        val candidates = dev.paradox.trace.core.time.IntervalMath.merge(intervals)
        val segments = mutableListOf<TimeInterval>()
        var current = candidates.first()
        for (next in candidates.subList(1, candidates.size)) {
            val gap = next.startInclusiveMs - current.endExclusiveMs
            if (gap < gapThresholdMs) {
                current = TimeInterval(current.startInclusiveMs, next.endExclusiveMs)
            } else {
                segments += current
                current = next
            }
        }
        segments += current
        return segments
    }
}
