package dev.paradox.trace.domain.collection

import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UsageSessionCommand
import kotlin.coroutines.cancellation.CancellationException

/**
 * Orchestrates one usage-import pass: query raw transitions, plan observed
 * sessions, persist them idempotently. Re-running the sync must never create
 * duplicate sessions; the repository's dedupe keys guarantee that.
 */
class UsageSessionSyncer(
    private val foregroundSource: ForegroundUsageSource,
    private val sessionRepository: SessionRepository,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val backfillDurationMs: Long = DEFAULT_BACKFILL_MS,
) {

    /** Returns the number of newly imported sessions. */
    suspend fun sync(): Result<Int> = try {
        val windowEndMs = nowMs()
        val windowStartMs = windowEndMs - backfillDurationMs

        val events = foregroundSource.queryEvents(
            packageNames = BilibiliPackages.APP_PACKAGES,
            fromInclusiveMs = windowStartMs,
            toExclusiveMs = windowEndMs,
        )
        val planned = UsageSessionPlanner.plan(
            events = events,
            targetPackages = BilibiliPackages.APP_PACKAGES,
            windowStartMs = windowStartMs,
            windowEndMs = windowEndMs,
        )
        if (planned.isEmpty()) {
            Result.success(0)
        } else {
            Result.success(
                sessionRepository.addUsageSessions(
                    planned.map { session ->
                        UsageSessionCommand(
                            packageName = session.packageName,
                            platform = BilibiliPackages.PLATFORM,
                            startedAtMs = session.interval.startInclusiveMs,
                            endedAtMs = session.interval.endExclusiveMs,
                        )
                    },
                ),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: SecurityException) {
        // Access revoked between the UI check and this query: not a crash.
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val DEFAULT_BACKFILL_MS: Long = 7L * 24 * 60 * 60 * 1000
    }
}
