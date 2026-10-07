package dev.paradox.trace.data.local

/**
 * Stable dedupe keys. Same-observed-fact writes must be idempotent; distinct
 * viewings of the same content must stay distinct (docs/DATA_MODEL.md §5).
 */
object DedupeKeys {

    fun manualSession(
        platform: String,
        platformContentId: String?,
        startedAtMs: Long,
        endedAtMs: Long,
    ): String = listOf(
        "USER_INPUT",
        "SESSION_ENDED",
        platform,
        platformContentId ?: "-",
        startedAtMs.toString(),
        endedAtMs.toString(),
    ).joinToString(separator = "|")

    /**
     * Platform-observed usage sessions. Keyed by package and exact interval
     * so repeated imports of the same system-reported interval are idempotent.
     */
    fun usageSession(
        packageName: String,
        startedAtMs: Long,
        endedAtMs: Long,
    ): String = listOf(
        "USAGE_STATS",
        "SESSION_ENDED",
        packageName,
        startedAtMs.toString(),
        endedAtMs.toString(),
    ).joinToString(separator = "|")
}
