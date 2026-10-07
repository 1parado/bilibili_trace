package dev.paradox.trace.data.repository

import dev.paradox.trace.data.local.DedupeKeys
import dev.paradox.trace.data.local.SessionMapper
import dev.paradox.trace.data.local.TransactionRunner
import dev.paradox.trace.data.local.dao.AppSessionDao
import dev.paradox.trace.data.local.dao.BehaviorEventDao
import dev.paradox.trace.data.local.dao.ContentItemDao
import dev.paradox.trace.data.local.entity.AppSessionEntity
import dev.paradox.trace.data.local.entity.BehaviorEventEntity
import dev.paradox.trace.data.local.entity.ContentItemEntity
import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.model.TimeInterval
import dev.paradox.trace.domain.repository.AccessibilitySessionCommand
import dev.paradox.trace.domain.repository.ManualSessionCommand
import dev.paradox.trace.domain.repository.SessionRepository
import dev.paradox.trace.domain.repository.UsageSessionCommand
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Room-backed [SessionRepository]. Manual entries are user-observed facts
 * (COMPLETE, USER_INPUT); dedupe keys make exact re-submissions idempotent.
 */
class RoomSessionRepository(
    private val appSessionDao: AppSessionDao,
    private val contentItemDao: ContentItemDao,
    private val behaviorEventDao: BehaviorEventDao,
    private val transactionRunner: TransactionRunner,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : SessionRepository {

    override fun observeAllSessions(): Flow<List<ContentSession>> =
        combine(
            appSessionDao.observeAll(),
            contentItemDao.observeAll(),
        ) { sessions, contents ->
            joinWithContent(sessions, contents)
        }

    override fun observeSessionsBetween(fromMs: Long, toMs: Long): Flow<List<ContentSession>> =
        combine(
            appSessionDao.observeOverlapping(fromMs, toMs),
            contentItemDao.observeAll(),
        ) { sessions, contents ->
            joinWithContent(sessions, contents)
        }

    override suspend fun addManualSession(command: ManualSessionCommand): Result<ContentSession> {
        val validationFailure = validate(command)
        if (validationFailure != null) return Result.failure(validationFailure)

        return transactionRunner.inTransaction {
            val now = nowMs()
            val contentLocalId = command.platformContentId?.let { "$PLATFORM_BILIBILI:$it" }
                ?: command.title?.let { "local:${UUID.randomUUID()}" }

            val contentItem = buildContentItem(command, contentLocalId, now)
            if (contentItem != null) contentItemDao.upsert(contentItem)

            val dedupeKey = DedupeKeys.manualSession(
                platform = command.platform,
                platformContentId = command.platformContentId,
                startedAtMs = command.startedAtMs,
                endedAtMs = command.endedAtMs,
            )
            val eventId = deterministicId(dedupeKey)
            val inserted = behaviorEventDao.insert(
                BehaviorEventEntity(
                    id = eventId,
                    platform = command.platform,
                    source = EventSource.USER_INPUT.name,
                    eventType = EVENT_SESSION_ENDED,
                    occurredAt = command.endedAtMs,
                    sessionId = eventId,
                    contentId = contentLocalId,
                    confidence = null,
                    evidenceRef = null,
                    dedupeKey = dedupeKey,
                    schemaVersion = SCHEMA_VERSION,
                    createdAt = now,
                ),
            )

            if (inserted == DUPLICATE_IGNORED) {
                val existing = appSessionDao.getById(eventId)
                if (existing != null) {
                    return@inTransaction Result.success(
                        SessionMapper.toDomain(existing, contentLocalId?.let { contentItemDao.getById(it) }),
                    )
                }
            }

            val session = SessionMapper.toEntity(
                sessionId = eventId,
                platform = command.platform,
                packageName = PACKAGE_NAME_FALLBACK,
                interval = TimeInterval(command.startedAtMs, command.endedAtMs),
                source = EventSource.USER_INPUT,
                completeness = SessionCompleteness.COMPLETE,
                contentLocalId = contentLocalId,
                nowMs = now,
            )
            appSessionDao.upsert(session)
            Result.success(SessionMapper.toDomain(session, contentItem))
        }
    }

    /**
     * Usage imports carry no content identity (package-level observation),
     * are persisted as USAGE_STATS/COMPLETE, and are idempotent via dedupe
     * keys. Invalid commands are skipped individually; one bad interval must
     * not block a whole import batch.
     */
    override suspend fun addUsageSessions(commands: List<UsageSessionCommand>): Int =
        transactionRunner.inTransaction {
            val now = nowMs()
            var imported = 0
            for (command in commands) {
                if (command.startedAtMs > command.endedAtMs) continue
                if (command.endedAtMs > now + FUTURE_SKEW_TOLERANCE_MS) continue

                val dedupeKey = DedupeKeys.usageSession(
                    packageName = command.packageName,
                    startedAtMs = command.startedAtMs,
                    endedAtMs = command.endedAtMs,
                )
                val sessionId = deterministicId(dedupeKey)
                val inserted = behaviorEventDao.insert(
                    BehaviorEventEntity(
                        id = sessionId,
                        platform = command.platform,
                        source = EventSource.USAGE_STATS.name,
                        eventType = EVENT_SESSION_ENDED,
                        occurredAt = command.endedAtMs,
                        sessionId = sessionId,
                        contentId = null,
                        confidence = null,
                        evidenceRef = null,
                        dedupeKey = dedupeKey,
                        schemaVersion = SCHEMA_VERSION,
                        createdAt = now,
                    ),
                )
                if (inserted == DUPLICATE_IGNORED) continue

                appSessionDao.upsert(
                    SessionMapper.toEntity(
                        sessionId = sessionId,
                        platform = command.platform,
                        packageName = command.packageName,
                        interval = TimeInterval(command.startedAtMs, command.endedAtMs),
                        source = EventSource.USAGE_STATS,
                        completeness = SessionCompleteness.COMPLETE,
                        contentLocalId = null,
                        nowMs = now,
                    ),
                )
                imported++
            }
            imported
        }

    /**
     * Accessibility observations carry title-level metadata (recognition
     * output, userVerified=false). Same title across sessions resolves to one
     * content identity via a deterministic local id. Idempotent like manual
     * entries; invalid commands fail without partial writes.
     */
    override suspend fun addAccessibilitySession(
        command: AccessibilitySessionCommand,
    ): Result<ContentSession> {
        val validationFailure = validateAccessibility(command)
        if (validationFailure != null) return Result.failure(validationFailure)

        return transactionRunner.inTransaction {
            val now = nowMs()
            val contentLocalId = "observed:${deterministicId(command.title)}"
            val existingContent = contentItemDao.getById(contentLocalId)
            val contentItem = ContentItemEntity(
                id = contentLocalId,
                platform = PLATFORM_BILIBILI,
                platformContentId = null,
                canonicalUrl = null,
                title = command.title,
                creatorId = null,
                creatorName = null,
                userTopic = null,
                metadataSource = EventSource.ACCESSIBILITY.name,
                metadataConfidence = null,
                firstSeenAt = existingContent?.firstSeenAt ?: now,
                lastSeenAt = now,
                userVerified = false,
            )
            contentItemDao.upsert(contentItem)

            val dedupeKey = DedupeKeys.accessibilitySession(
                packageName = command.packageName,
                title = command.title,
                startedAtMs = command.startedAtMs,
                endedAtMs = command.endedAtMs,
            )
            val sessionId = deterministicId(dedupeKey)
            val inserted = behaviorEventDao.insert(
                BehaviorEventEntity(
                    id = sessionId,
                    platform = PLATFORM_BILIBILI,
                    source = EventSource.ACCESSIBILITY.name,
                    eventType = EVENT_SESSION_ENDED,
                    occurredAt = command.endedAtMs,
                    sessionId = sessionId,
                    contentId = contentLocalId,
                    confidence = null,
                    evidenceRef = null,
                    dedupeKey = dedupeKey,
                    schemaVersion = SCHEMA_VERSION,
                    createdAt = now,
                ),
            )

            if (inserted == DUPLICATE_IGNORED) {
                val existing = appSessionDao.getById(sessionId)
                if (existing != null) {
                    return@inTransaction Result.success(
                        SessionMapper.toDomain(existing, contentItem),
                    )
                }
            }

            val session = SessionMapper.toEntity(
                sessionId = sessionId,
                platform = PLATFORM_BILIBILI,
                packageName = command.packageName,
                interval = TimeInterval(command.startedAtMs, command.endedAtMs),
                source = EventSource.ACCESSIBILITY,
                completeness = SessionCompleteness.COMPLETE,
                contentLocalId = contentLocalId,
                nowMs = now,
            )
            appSessionDao.upsert(session)
            Result.success(SessionMapper.toDomain(session, contentItem))
        }
    }

    override suspend fun deleteSession(id: String) {
        transactionRunner.inTransaction {
            appSessionDao.deleteById(id)
        }
    }

    override suspend fun deleteAllData() {
        transactionRunner.inTransaction {
            appSessionDao.deleteAll()
            contentItemDao.deleteAll()
            behaviorEventDao.deleteAll()
        }
    }

    private fun validate(command: ManualSessionCommand): Throwable? = when {
        command.startedAtMs > command.endedAtMs ->
            IllegalArgumentException("session start must not be after end")
        command.endedAtMs > nowMs() + FUTURE_SKEW_TOLERANCE_MS ->
            IllegalArgumentException("session end is unreasonably in the future")
        else -> null
    }

    private fun validateAccessibility(command: AccessibilitySessionCommand): Throwable? = when {
        command.title.isBlank() ->
            IllegalArgumentException("accessibility session requires a non-blank title")
        command.startedAtMs > command.endedAtMs ->
            IllegalArgumentException("session start must not be after end")
        command.endedAtMs > nowMs() + FUTURE_SKEW_TOLERANCE_MS ->
            IllegalArgumentException("session end is unreasonably in the future")
        else -> null
    }

    private fun buildContentItem(
        command: ManualSessionCommand,
        contentLocalId: String?,
        nowMs: Long,
    ): ContentItemEntity? {
        if (contentLocalId == null) return null
        return ContentItemEntity(
            id = contentLocalId,
            platform = command.platform,
            platformContentId = command.platformContentId,
            canonicalUrl = command.platformContentId?.let { "https://www.bilibili.com/video/$it" },
            title = command.title,
            creatorId = null,
            creatorName = command.creatorName,
            userTopic = null,
            metadataSource = command.metadataSource ?: METADATA_SOURCE_USER,
            metadataConfidence = null,
            firstSeenAt = nowMs,
            lastSeenAt = nowMs,
            userVerified = true,
        )
    }

    private fun joinWithContent(
        sessions: List<AppSessionEntity>,
        contents: List<ContentItemEntity>,
    ): List<ContentSession> {
        val contentById = contents.associateBy { it.id }
        return sessions.map { session ->
            SessionMapper.toDomain(session, session.contentId?.let { contentById[it] })
        }
    }

    private fun deterministicId(key: String): String =
        UUID.nameUUIDFromBytes(key.toByteArray()).toString()

    private companion object {
        const val EVENT_SESSION_ENDED = "SESSION_ENDED"
        const val METADATA_SOURCE_USER = "USER_INPUT"
        const val PACKAGE_NAME_FALLBACK = "tv.danmaku.bili"
        const val PLATFORM_BILIBILI = "bilibili"
        const val SCHEMA_VERSION = 1
        const val DUPLICATE_IGNORED = -1L
        const val FUTURE_SKEW_TOLERANCE_MS = 60_000L
    }
}
