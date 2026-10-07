package dev.paradox.trace.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dev.paradox.trace.domain.collection.ForegroundUsageSource

/**
 * UsageStatsManager-backed [ForegroundUsageSource]. Reads only package-level
 * activity transitions for the requested packages; never screen text. Without
 * the usage-access grant the system returns an empty event set, which callers
 * treat as "no data", not as an error.
 */
class UsageStatsForegroundSource(context: Context) : ForegroundUsageSource {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun queryEvents(
        packageNames: Set<String>,
        fromInclusiveMs: Long,
        toExclusiveMs: Long,
    ): List<ForegroundUsageSource.ForegroundEvent> {
        val result = mutableListOf<ForegroundUsageSource.ForegroundEvent>()
        val query = usageStatsManager.queryEvents(fromInclusiveMs, toExclusiveMs)
        val event = UsageEvents.Event()
        while (query.hasNextEvent()) {
            query.getNextEvent(event)
            if (event.packageName !in packageNames) continue
            val isForeground = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> true
                UsageEvents.Event.ACTIVITY_PAUSED -> false
                else -> null
            } ?: continue
            result += ForegroundUsageSource.ForegroundEvent(
                packageName = event.packageName,
                isForeground = isForeground,
                timestampMs = event.timeStamp,
            )
        }
        return result
    }
}
