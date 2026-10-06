package dev.paradox.trace.data.local

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * Wraps transactional writes so repository logic stays testable without an
 * Android database instance.
 */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

/** Room-backed implementation using room-ktx `withTransaction`. */
class RoomTransactionRunner(private val database: RoomDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }
}
