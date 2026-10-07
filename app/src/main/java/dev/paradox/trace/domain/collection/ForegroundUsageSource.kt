package dev.paradox.trace.domain.collection

/**
 * Platform-facing adapter that yields raw foreground transitions for a set of
 * packages. Implementations live behind this interface so domain planning and
 * persistence stay testable without Android framework classes.
 */
interface ForegroundUsageSource {

    /** One observed app lifecycle transition, in UTC epoch milliseconds. */
    data class ForegroundEvent(
        val packageName: String,
        val isForeground: Boolean,
        val timestampMs: Long,
    )

    /**
     * Returns transitions for [packageNames] within the half-open window
     * `[fromInclusiveMs, toExclusiveMs)`. When the caller lacks the required
     * platform access, implementations should return an empty list rather
     * than pretending data does not exist.
     */
    fun queryEvents(
        packageNames: Set<String>,
        fromInclusiveMs: Long,
        toExclusiveMs: Long,
    ): List<ForegroundEvent>
}
