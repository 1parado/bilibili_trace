package dev.paradox.trace.data.local

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Identity transaction runner for JVM unit tests; runs blocks sequentially. */
class FakeTransactionRunner : TransactionRunner {
    private val mutex = Mutex()
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        mutex.withLock { block() }
}
