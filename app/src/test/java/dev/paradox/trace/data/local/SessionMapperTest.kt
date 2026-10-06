package dev.paradox.trace.data.local

import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.model.TimeInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionMapperTest {

    @Test
    fun `round trip preserves session fields`() {
        val entity = SessionMapper.toEntity(
            sessionId = "s1",
            platform = "bilibili",
            packageName = "tv.danmaku.bili",
            interval = TimeInterval(100L, 200L),
            source = EventSource.USER_INPUT,
            completeness = SessionCompleteness.COMPLETE,
            contentLocalId = "bilibili:BV1",
            nowMs = 999L,
        )
        val domain = SessionMapper.toDomain(entity, null)

        assertEquals("s1", domain.id)
        assertEquals("bilibili", domain.platform)
        assertEquals(TimeInterval(100L, 200L), domain.interval)
        assertEquals(EventSource.USER_INPUT, domain.source)
        assertEquals(SessionCompleteness.COMPLETE, domain.completeness)
    }

    @Test
    fun `unknown persisted enum names degrade instead of crashing`() {
        assertEquals(EventSource.USER_INPUT, SessionMapper.parseSource("SOMETHING_NEW"))
        assertEquals(
            SessionCompleteness.PARTIAL,
            SessionMapper.parseCompleteness("SOMETHING_NEW"),
        )
    }

    @Test
    fun `null ended_at maps to zero-length interval`() {
        val entity = SessionMapper.toEntity(
            sessionId = "s2",
            platform = "bilibili",
            packageName = "tv.danmaku.bili",
            interval = TimeInterval(100L, 100L),
            source = EventSource.USER_INPUT,
            completeness = SessionCompleteness.PARTIAL,
            contentLocalId = null,
            nowMs = 999L,
        ).copy(endedAt = null)

        assertEquals(TimeInterval(100L, 100L), SessionMapper.toDomain(entity, null).interval)
    }
}
