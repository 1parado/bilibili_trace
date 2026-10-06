package dev.paradox.trace.domain.model

/**
 * Domain-wide source taxonomy. A source describes where an observation came
 * from; it never certifies that the observation is true.
 */
enum class EventSource {
    USER_INPUT,
    SHARE_INTENT,
    USAGE_STATS,
    ACCESSIBILITY,
    PLATFORM_API,
}

/**
 * Completeness of an observed session. Missing events must be represented as
 * PARTIAL / INTERRUPTED / ESTIMATED, never silently as COMPLETE.
 */
enum class SessionCompleteness {
    COMPLETE,
    PARTIAL,
    INTERRUPTED,
    ESTIMATED,
}

/**
 * A half-open time interval `[startInclusiveMs, endExclusiveMs)` in UTC epoch
 * milliseconds, per docs/DATA_MODEL.md. Adjacent intervals never double count.
 */
data class TimeInterval(
    val startInclusiveMs: Long,
    val endExclusiveMs: Long,
) {
    init {
        require(endExclusiveMs >= startInclusiveMs) {
            "interval end ($endExclusiveMs) must not be before start ($startInclusiveMs)"
        }
    }

    val durationMs: Long
        get() = endExclusiveMs - startInclusiveMs

    val isZeroLength: Boolean
        get() = durationMs == 0L

    companion object {
        fun of(startInclusiveMs: Long, endExclusiveMs: Long): TimeInterval =
            TimeInterval(startInclusiveMs, endExclusiveMs)
    }
}

/** Minimal content identity used for grouping and display. */
data class ContentRef(
    val platform: String,
    val platformContentId: String?,
    val title: String?,
    val creatorName: String?,
)

/**
 * A user-visible content consumption session. This is the domain projection
 * of raw events; it is rebuildable from persisted data and never stored as a
 * source of truth by itself.
 */
data class ContentSession(
    val id: String,
    val platform: String,
    val interval: TimeInterval,
    val source: EventSource,
    val completeness: SessionCompleteness,
    val content: ContentRef?,
)
