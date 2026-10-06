package dev.paradox.trace.data.local

import dev.paradox.trace.data.local.entity.AppSessionEntity
import dev.paradox.trace.data.local.entity.ContentItemEntity
import dev.paradox.trace.domain.model.ContentRef
import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.model.TimeInterval

/** Entity <-> domain mapping. Unknown persisted enum names must not crash. */
object SessionMapper {

    fun toDomain(session: AppSessionEntity, content: ContentItemEntity?): dev.paradox.trace.domain.model.ContentSession {
        val endedAt = session.endedAt
        return dev.paradox.trace.domain.model.ContentSession(
            id = session.id,
            platform = session.platform,
            interval = TimeInterval(
                startInclusiveMs = session.startedAt,
                endExclusiveMs = endedAt ?: session.startedAt,
            ),
            source = parseSource(session.source),
            completeness = parseCompleteness(session.completeness),
            content = content?.let(::toContentRef),
        )
    }

    fun toEntity(
        sessionId: String,
        platform: String,
        packageName: String,
        interval: TimeInterval,
        source: EventSource,
        completeness: SessionCompleteness,
        contentLocalId: String?,
        nowMs: Long,
    ): AppSessionEntity = AppSessionEntity(
        id = sessionId,
        platform = platform,
        packageName = packageName,
        startedAt = interval.startInclusiveMs,
        endedAt = interval.endExclusiveMs,
        durationMs = interval.durationMs,
        source = source.name,
        completeness = completeness.name,
        contentId = contentLocalId,
        createdAt = nowMs,
        updatedAt = nowMs,
    )

    fun toContentRef(content: ContentItemEntity): ContentRef = ContentRef(
        platform = content.platform,
        platformContentId = content.platformContentId,
        title = content.title,
        creatorName = content.creatorName,
    )

    fun parseSource(name: String): EventSource =
        EventSource.entries.firstOrNull { it.name == name } ?: EventSource.USER_INPUT

    fun parseCompleteness(name: String): SessionCompleteness =
        SessionCompleteness.entries.firstOrNull { it.name == name }
            ?: SessionCompleteness.PARTIAL
}
