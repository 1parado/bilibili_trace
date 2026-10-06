package dev.paradox.trace

import android.app.Application
import androidx.room.Room
import dev.paradox.trace.data.local.RoomTransactionRunner
import dev.paradox.trace.data.local.TraceDatabase
import dev.paradox.trace.data.repository.RoomSessionRepository
import dev.paradox.trace.domain.repository.SessionRepository

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
}
