package dev.paradox.trace.data.repository

import dev.paradox.trace.data.local.DedupeKeys
import dev.paradox.trace.data.local.FakeTransactionRunner
import dev.paradox.trace.data.local.dao.AppSessionDao
import dev.paradox.trace.data.local.dao.BehaviorEventDao
import dev.paradox.trace.data.local.dao.ContentItemDao
import dev.paradox.trace.data.local.entity.AppSessionEntity
import dev.paradox.trace.data.local.entity.BehaviorEventEntity
import dev.paradox.trace.data.local.entity.ContentItemEntity
import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.repository.AccessibilitySessionCommand
import dev.paradox.trace.domain.repository.ManualSessionCommand
import dev.paradox.trace.domain.repository.UsageSessionCommand
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomSessionRepositoryTest {

    private val now = AtomicLong(1_700_000_000_000L)

    private fun repository(): Pair<RoomSessionRepository, Fakes> {
        val fakes = Fakes()
        val repository = RoomSessionRepository(
            appSessionDao = fakes.sessionDao,
            contentItemDao = fakes.contentDao,
            behaviorEventDao = fakes.eventDao,
            transactionRunner = FakeTransactionRunner(),
            nowMs = { now.get() },
        )
        return repository to fakes
    }

    private fun command(
        startMs: Long = now.get() - 3_600_000L,
        endMs: Long = now.get(),
        contentId: String? = "BV1xx411c7mD",
        title: String? = "测试视频",
    ) = ManualSessionCommand(
        platform = "bilibili",
        title = title,
        platformContentId = contentId,
        creatorName = "测试UP主",
        startedAtMs = startMs,
        endedAtMs = endMs,
    )

    @Test
    fun `addManualSession stores session content and event`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addManualSession(command())

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals(EventSource.USER_INPUT, session.source)
        assertEquals(SessionCompleteness.COMPLETE, session.completeness)
        assertEquals(3_600_000L, session.interval.durationMs)
        assertEquals("测试视频", session.content?.title)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.contentDao.rows.size)
        assertEquals(1, fakes.eventDao.rows.size)
    }

    @Test
    fun `addManualSession without content identity still stores session`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addManualSession(command(contentId = null, title = null))

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrThrow().content)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(0, fakes.contentDao.rows.size)
    }

    @Test
    fun `addManualSession rejects inverted interval and stores nothing`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addManualSession(command(startMs = now.get(), endMs = now.get() - 1))

        assertTrue(result.isFailure)
        assertEquals(0, fakes.sessionDao.rows.size)
        assertEquals(0, fakes.eventDao.rows.size)
    }

    @Test
    fun `addManualSession rejects unreasonably future end`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addManualSession(
            command(startMs = now.get() + 10_000_000L, endMs = now.get() + 20_000_000L),
        )

        assertTrue(result.isFailure)
        assertEquals(0, fakes.sessionDao.rows.size)
    }

    @Test
    fun `exact duplicate submission is idempotent`() = runTest {
        val (repo, fakes) = repository()

        val first = repo.addManualSession(command())
        val second = repo.addManualSession(command())

        assertTrue(first.isSuccess && second.isSuccess)
        assertEquals(first.getOrThrow().id, second.getOrThrow().id)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.eventDao.rows.size)
    }

    @Test
    fun `same content watched at another time is a distinct session`() = runTest {
        val (repo, fakes) = repository()

        val first = repo.addManualSession(command())
        now.addAndGet(10_000_000L)
        val second = repo.addManualSession(command())

        assertTrue(first.getOrThrow().id != second.getOrThrow().id)
        assertEquals(2, fakes.sessionDao.rows.size)
    }

    @Test
    fun `deleteAllData clears sessions content and events`() = runTest {
        val (repo, fakes) = repository()
        repo.addManualSession(command())
        repo.addManualSession(command(contentId = null, title = null))

        repo.deleteAllData()

        assertEquals(0, fakes.sessionDao.rows.size)
        assertEquals(0, fakes.contentDao.rows.size)
        assertEquals(0, fakes.eventDao.rows.size)
    }

    @Test
    fun `observeSessionsBetween returns sessions overlapping window`() = runTest {
        val (repo, _) = repository()
        repo.addManualSession(command(startMs = 0L, endMs = 1_000L))
        repo.addManualSession(command(startMs = 5_000L, endMs = 6_000L))

        val sessions = repo.observeSessionsBetween(500L, 1_500L).first()

        assertEquals(1, sessions.size)
        assertEquals(1_000L, sessions.first().interval.endExclusiveMs)
    }

    @Test
    fun `dedupe key distinguishes content and time`() {
        val base = DedupeKeys.manualSession("bilibili", "BV1", 100L, 200L)
        assertTrue(base != DedupeKeys.manualSession("bilibili", "BV2", 100L, 200L))
        assertTrue(base != DedupeKeys.manualSession("bilibili", "BV1", 100L, 201L))
        assertEquals(base, DedupeKeys.manualSession("bilibili", "BV1", 100L, 200L))
    }

    private fun usageCommand(
        packageName: String = "tv.danmaku.bili",
        startMs: Long = now.get() - 3_600_000L,
        endMs: Long = now.get() - 1_800_000L,
    ) = UsageSessionCommand(
        packageName = packageName,
        platform = "bilibili",
        startedAtMs = startMs,
        endedAtMs = endMs,
    )

    @Test
    fun `addUsageSessions stores package-level sessions without content`() = runTest {
        val (repo, fakes) = repository()

        val imported = repo.addUsageSessions(listOf(usageCommand()))

        assertEquals(1, imported)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(0, fakes.contentDao.rows.size)
        val entity = fakes.sessionDao.rows.values.single()
        assertEquals(EventSource.USAGE_STATS.name, entity.source)
        assertEquals(SessionCompleteness.COMPLETE.name, entity.completeness)
        assertEquals("tv.danmaku.bili", entity.packageName)
        assertEquals(null, entity.contentId)
    }

    @Test
    fun `re-importing the same usage intervals is idempotent`() = runTest {
        val (repo, fakes) = repository()

        val first = repo.addUsageSessions(listOf(usageCommand()))
        val second = repo.addUsageSessions(listOf(usageCommand()))

        assertEquals(1, first)
        assertEquals(0, second)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.eventDao.rows.size)
    }

    @Test
    fun `usage dedupe keys differ by package`() = runTest {
        val (repo, fakes) = repository()

        val imported = repo.addUsageSessions(
            listOf(
                usageCommand(packageName = "tv.danmaku.bili"),
                usageCommand(packageName = "com.bilibili.app.in"),
            ),
        )

        assertEquals(2, imported)
        assertEquals(2, fakes.sessionDao.rows.size)
    }

    @Test
    fun `invalid usage commands are skipped without blocking the batch`() = runTest {
        val (repo, fakes) = repository()

        val imported = repo.addUsageSessions(
            listOf(
                usageCommand(startMs = now.get(), endMs = now.get() - 1L), // inverted
                usageCommand(startMs = now.get() + 10_000_000L, endMs = now.get() + 20_000_000L), // future
                usageCommand(), // valid
            ),
        )

        assertEquals(1, imported)
        assertEquals(1, fakes.sessionDao.rows.size)
    }

    @Test
    fun `usage dedupe key distinguishes package and time`() {
        val base = DedupeKeys.usageSession("tv.danmaku.bili", 100L, 200L)
        assertTrue(base != DedupeKeys.usageSession("tv.danmaku.bili", 100L, 201L))
        assertTrue(base != DedupeKeys.usageSession("com.bilibili.app.in", 100L, 200L))
        assertEquals(base, DedupeKeys.usageSession("tv.danmaku.bili", 100L, 200L))
    }

    private fun a11yCommand(
        title: String = "【实测】自动识别的视频标题",
        startMs: Long = now.get() - 1_800_000L,
        endMs: Long = now.get() - 900_000L,
    ) = AccessibilitySessionCommand(
        packageName = "tv.danmaku.bili",
        title = title,
        startedAtMs = startMs,
        endedAtMs = endMs,
    )

    @Test
    fun `addAccessibilitySession stores title content with accessibility provenance`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addAccessibilitySession(a11yCommand())

        assertTrue(result.isSuccess)
        val session = result.getOrThrow()
        assertEquals(EventSource.ACCESSIBILITY, session.source)
        assertEquals("【实测】自动识别的视频标题", session.content?.title)
        assertEquals(null, session.content?.platformContentId)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.contentDao.rows.size)
        assertEquals(false, fakes.contentDao.rows.values.single().userVerified)
        assertEquals(EventSource.ACCESSIBILITY.name, fakes.contentDao.rows.values.single().metadataSource)
    }

    @Test
    fun `same title across accessibility sessions shares one content identity`() = runTest {
        val (repo, fakes) = repository()

        val first = repo.addAccessibilitySession(a11yCommand(startMs = 100L, endMs = 200L))
        val second = repo.addAccessibilitySession(a11yCommand(startMs = 500L, endMs = 600L))

        assertTrue(first.isSuccess && second.isSuccess)
        assertEquals(first.getOrThrow().content, second.getOrThrow().content)
        assertEquals(2, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.contentDao.rows.size)
    }

    @Test
    fun `accessibility re-submission is idempotent`() = runTest {
        val (repo, fakes) = repository()

        val first = repo.addAccessibilitySession(a11yCommand())
        val second = repo.addAccessibilitySession(a11yCommand())

        assertTrue(first.isSuccess && second.isSuccess)
        assertEquals(first.getOrThrow().id, second.getOrThrow().id)
        assertEquals(1, fakes.sessionDao.rows.size)
        assertEquals(1, fakes.eventDao.rows.size)
    }

    @Test
    fun `accessibility session with matching title inherits enriched creator`() = runTest {
        val (repo, fakes) = repository()

        // Share import enriched the content identity via the platform API.
        repo.addManualSession(command(title = "  测试视频 "))
        now.addAndGet(10_000_000L)
        val observed = repo.addAccessibilitySession(a11yCommand(title = "测试视频"))

        assertTrue(observed.isSuccess)
        val content = observed.getOrThrow().content
        assertEquals("测试UP主", content?.creatorName)
        assertEquals("BV1xx411c7mD", content?.platformContentId)
        assertEquals(1, fakes.contentDao.rows.size)
    }

    @Test
    fun `observed sessions borrow creator from enriched entry at read time`() = runTest {
        val (repo, fakes) = repository()

        // History written before title merging existed: two separate rows.
        val observed = repo.addAccessibilitySession(a11yCommand(title = "测试视频"))
        now.addAndGet(10_000_000L)
        repo.addManualSession(command()) // same default title, enriched with creator

        val sessions = repo.observeAllSessions().first()
        val bareObserved = sessions.first {
            it.id == observed.getOrThrow().id
        }
        assertEquals("测试UP主", bareObserved.content?.creatorName)
        assertTrue(fakes.contentDao.rows.size >= 2)
    }

    @Test
    fun `blank title accessibility session is rejected`() = runTest {
        val (repo, fakes) = repository()

        val result = repo.addAccessibilitySession(a11yCommand(title = "   "))

        assertTrue(result.isFailure)
        assertEquals(0, fakes.sessionDao.rows.size)
        assertEquals(0, fakes.contentDao.rows.size)
    }
}

private class Fakes {
    val sessionDao = AppSessionDaoFake()
    val contentDao = ContentItemDaoFake()
    val eventDao = BehaviorEventDaoFake()
}

private class AppSessionDaoFake : AppSessionDao {
    val rows = LinkedHashMap<String, AppSessionEntity>()
    private val flow = MutableStateFlow<List<AppSessionEntity>>(emptyList())

    private fun publish() {
        flow.value = rows.values.sortedByDescending { it.startedAt }
    }

    override suspend fun upsert(session: AppSessionEntity) {
        rows[session.id] = session
        publish()
    }

    override suspend fun upsertAll(sessions: List<AppSessionEntity>) {
        sessions.forEach { rows[it.id] = it }
        publish()
    }

    override fun observeAll(): Flow<List<AppSessionEntity>> = flow

    override fun observeOverlapping(fromMs: Long, toMs: Long): Flow<List<AppSessionEntity>> =
        MutableStateFlow(
            rows.values
                .filter { it.startedAt < toMs && (it.endedAt ?: it.startedAt) >= fromMs }
                .sortedBy { it.startedAt },
        )

    override suspend fun getById(id: String): AppSessionEntity? = rows[id]

    override suspend fun deleteById(id: String) {
        rows.remove(id)
        publish()
    }

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }
}

private class ContentItemDaoFake : ContentItemDao {
    val rows = LinkedHashMap<String, ContentItemEntity>()
    private val flow = MutableStateFlow<List<ContentItemEntity>>(emptyList())

    private fun publish() {
        flow.value = rows.values.sortedByDescending { it.lastSeenAt ?: 0L }
    }

    override suspend fun upsert(content: ContentItemEntity) {
        rows[content.id] = content
        publish()
    }

    override suspend fun getById(id: String): ContentItemEntity? = rows[id]

    override suspend fun getByPlatformContentId(platformContentId: String): ContentItemEntity? =
        rows.values.firstOrNull { it.platformContentId == platformContentId }

    override suspend fun getByTitle(platform: String, title: String): ContentItemEntity? =
        rows.values
            .filter { it.platform == platform && it.title?.trim() == title }
            .sortedWith(compareByDescending<ContentItemEntity> { !it.creatorName.isNullOrBlank() }.thenByDescending { it.lastSeenAt ?: 0L })
            .firstOrNull()

    override fun observeAll(): Flow<List<ContentItemEntity>> = flow

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }
}

private class BehaviorEventDaoFake : BehaviorEventDao {
    val rows = LinkedHashMap<String, BehaviorEventEntity>()
    private val flow = MutableStateFlow<List<BehaviorEventEntity>>(emptyList())

    private fun publish() {
        flow.value = rows.values.sortedBy { it.occurredAt }
    }

    override suspend fun insert(event: BehaviorEventEntity): Long {
        if (event.dedupeKey != null && rows.values.any { it.dedupeKey == event.dedupeKey }) {
            return -1L
        }
        rows[event.id] = event
        publish()
        return 1L
    }

    override suspend fun insertAll(events: List<BehaviorEventEntity>): List<Long> =
        events.map { insert(it) }

    override fun observeAll(): Flow<List<BehaviorEventEntity>> = flow

    override suspend fun deleteAll() {
        rows.clear()
        publish()
    }
}
