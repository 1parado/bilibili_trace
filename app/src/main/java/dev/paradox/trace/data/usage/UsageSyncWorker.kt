package dev.paradox.trace.data.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.paradox.trace.TraceApplication
import kotlin.coroutines.cancellation.CancellationException

/**
 * Deferrable backfill of platform-observed usage sessions. When usage access
 * is not granted the source yields no events and the worker completes without
 * importing anything; it never crashes the app over a missing grant.
 */
class UsageSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val application = applicationContext as? TraceApplication ?: return Result.failure()
        return application.usageSessionSyncer.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is CancellationException) throw error else Result.retry()
            },
        )
    }
}
