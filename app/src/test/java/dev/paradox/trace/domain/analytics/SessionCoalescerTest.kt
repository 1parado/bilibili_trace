package dev.paradox.trace.domain.analytics

import dev.paradox.trace.domain.model.TimeInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionCoalescerTest {

    @Test
    fun `coalesce returns empty list for empty input`() {
        assertEquals(emptyList<TimeInterval>(), SessionCoalescer.coalesce(emptyList(), 60_000L))
    }

    @Test
    fun `coalesce merges intervals with gap strictly below threshold`() {
        val result = SessionCoalescer.coalesce(
            listOf(TimeInterval(0, 100), TimeInterval(100 + 59_999, 200)),
            gapThresholdMs = 60_000L,
        )
        assertEquals(listOf(TimeInterval(0, 200)), result)
    }

    @Test
    fun `coalesce keeps intervals with gap equal to threshold`() {
        val result = SessionCoalescer.coalesce(
            listOf(TimeInterval(0, 100), TimeInterval(100 + 60_000, 200)),
            gapThresholdMs = 60_000L,
        )
        assertEquals(
            listOf(TimeInterval(0, 100), TimeInterval(100 + 60_000, 200)),
            result,
        )
    }

    @Test
    fun `coalesce with zero threshold degenerates to plain merge`() {
        val result = SessionCoalescer.coalesce(
            listOf(TimeInterval(0, 100), TimeInterval(100, 150), TimeInterval(200, 300)),
            gapThresholdMs = 0L,
        )
        assertEquals(listOf(TimeInterval(0, 150), TimeInterval(200, 300)), result)
    }

    @Test
    fun `coalesce merges a chain of nearby intervals`() {
        val result = SessionCoalescer.coalesce(
            listOf(
                TimeInterval(0, 100),
                TimeInterval(110, 200),
                TimeInterval(210, 300),
            ),
            gapThresholdMs = 20_000L,
        )
        assertEquals(listOf(TimeInterval(0, 300)), result)
    }

    @Test
    fun `coalesce rejects negative threshold`() {
        var thrown: IllegalArgumentException? = null
        try {
            SessionCoalescer.coalesce(emptyList(), gapThresholdMs = -1)
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertEquals(true, thrown != null)
    }
}
