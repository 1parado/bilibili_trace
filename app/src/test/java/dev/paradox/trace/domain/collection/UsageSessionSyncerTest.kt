package dev.paradox.trace.domain.collection

import dev.paradox.trace.domain.collection.ForegroundUsageSource.ForegroundEvent
import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.repository.ManualSessionCommand
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UsageSessionCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageSessionSyncerTest {

    private val nowMs = 1_700_000_000_000L

    @Test
    fun `sync plans events and forwards usage commands to repository`() = runTest {
        val source = FakeSource(
            listOf(
                ForegroundEvent("tv.danmaku.bili", true, nowMs - 60_000L),
                ForegroundEvent("tv.danmaku.bili", false, nowMs - 10_000L),
                ForegroundEvent("com.other.app", true, nowMs - 60_000L),
            ),
        )
        val repository = FakeSessionRepository()
        val syncer = UsageSessionSyncer(source, repository, nowMs = { nowMs })

        val result = syncer.sync()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
        val commands = repository.importedUsageCommands
        assertEquals(1, commands.size)
        assertEquals("tv.danmaku.bili", commands[0].packageName)
        assertEquals("bilibili", commands[0].platform)
        assertEquals(nowMs - 60_000L, commands[0].startedAtMs)
        assertEquals(nowMs - 10_000L, commands[0].endedAtMs)
    }

    @Test
    fun `sync with no usable events succeeds with zero`() = runTest {
        val syncer = UsageSessionSyncer(FakeSource(emptyList()), FakeSessionRepository(), nowMs = { nowMs })

        val result = syncer.sync()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `source failure is surfaced as Result failure`() = runTest {
        val syncer = UsageSessionSyncer(
            FakeSource(error = SecurityException("access revoked")),
            FakeSessionRepository(),
            nowMs = { nowMs },
        )

        val result = syncer.sync()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `re-sync after repository import reports zero new sessions`() = runTest {
        val source = FakeSource(
            listOf(
                ForegroundEvent("tv.danmaku.bili", true, nowMs - 60_000L),
                ForegroundEvent("tv.danmaku.bili", false, nowMs - 10_000L),
            ),
        )
        val repository = FakeSessionRepository()
        val syncer = UsageSessionSyncer(source, repository, nowMs = { nowMs })

        val first = syncer.sync()
        val second = syncer.sync()

        assertTrue(first.isSuccess && second.isSuccess)
        assertEquals(1, first.getOrThrow())
        assertEquals(0, second.getOrThrow())
    }

    private class FakeSource(
        private val events: List<ForegroundEvent> = emptyList(),
        private val error: Exception? = null,
    ) : ForegroundUsageSource {
        override fun queryEvents(
            packageNames: Set<String>,
            fromInclusiveMs: Long,
            toExclusiveMs: Long,
        ): List<ForegroundEvent> {
            error?.let { throw it }
            return events
        }
    }

    private class FakeSessionRepository : SessionRepository {
        val importedUsageCommands = mutableListOf<UsageSessionCommand>()
        private val seenUsageKeys = mutableSetOf<String>()

        override fun observeAllSessions(): Flow<List<ContentSession>> = MutableStateFlow(emptyList())

        override fun observeSessionsBetween(fromMs: Long, toMs: Long): Flow<List<ContentSession>> =
            MutableStateFlow(emptyList())

        override suspend fun addManualSession(command: ManualSessionCommand): Result<ContentSession> =
            Result.failure(UnsupportedOperationException("not needed in this test"))

        override suspend fun addUsageSessions(commands: List<UsageSessionCommand>): Int {
            var imported = 0
            for (command in commands) {
                val key = "${command.packageName}|${command.startedAtMs}|${command.endedAtMs}"
                if (seenUsageKeys.add(key)) {
                    importedUsageCommands += command
                    imported++
                }
            }
            return imported
        }

        override suspend fun addAccessibilitySession(
            command: dev.paradox.trace.domain.repository.AccessibilitySessionCommand,
        ): Result<ContentSession> = Result.failure(UnsupportedOperationException("not needed in this test"))

        override suspend fun deleteSession(id: String) = Unit

        override suspend fun deleteAllData() = Unit
    }
}
