package dev.paradox.trace

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.room.Room
import dev.paradox.trace.data.accessibility.AccessibilityServiceChecker
import dev.paradox.trace.data.local.RoomTransactionRunner
import dev.paradox.trace.data.local.TraceDatabase
import dev.paradox.trace.data.remote.BilibiliViewApi
import dev.paradox.trace.data.repository.RoomSessionRepository
import dev.paradox.trace.data.settings.DataStoreUserPreferencesRepository
import dev.paradox.trace.data.usage.UsageAccessChecker
import dev.paradox.trace.data.usage.UsageStatsForegroundSource
import dev.paradox.trace.data.usage.UsageSyncWorker
import dev.paradox.trace.domain.collection.UsageSessionSyncer
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UserPreferencesRepository
import dev.paradox.trace.domain.remote.BilibiliMetadataService
import java.util.concurrent.TimeUnit

/**
 * Minimal manual dependency graph (AGENTS.md: no DI framework by default).
 */
class TraceApplication : Application() {

    val database: TraceDatabase by lazy {
        Room.databaseBuilder(
            context = this,
            klass = TraceDatabase::class.java,
            name = TraceDatabase.NAME,
        ).build()
    }

    val sessionRepository: SessionRepository by lazy {
        RoomSessionRepository(
            appSessionDao = database.appSessionDao(),
            contentItemDao = database.contentItemDao(),
            behaviorEventDao = database.behaviorEventDao(),
            transactionRunner = RoomTransactionRunner(database),
        )
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(this)
    }

    val bilibiliMetadataService: BilibiliMetadataService by lazy {
        BilibiliViewApi()
    }

    val usageAccessChecker: UsageAccessChecker by lazy {
        UsageAccessChecker(this)
    }

    val accessibilityServiceChecker: AccessibilityServiceChecker by lazy {
        AccessibilityServiceChecker(this)
    }

    val usageSessionSyncer: UsageSessionSyncer by lazy {
        UsageSessionSyncer(
            foregroundSource = UsageStatsForegroundSource(this),
            sessionRepository = sessionRepository,
        )
    }

    override fun onCreate() {
        super.onCreate()
        scheduleUsageBackfill()
    }

    /**
     * Deferrable backfill of usage sessions; the work no-ops when the user
     * has not enabled usage access. KEEP policy preserves an existing schedule.
     */
    private fun scheduleUsageBackfill() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            USAGE_BACKFILL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UsageSyncWorker>(USAGE_BACKFILL_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .build(),
        )
    }

    private companion object {
        const val USAGE_BACKFILL_WORK_NAME = "usage-session-backfill"
        const val USAGE_BACKFILL_INTERVAL_MINUTES = 30L
    }
}
