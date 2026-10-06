package dev.paradox.trace.core.time

import dev.paradox.trace.domain.model.TimeInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalMathTest {

    @Test
    fun `merge returns empty list for empty input`() {
        assertEquals(emptyList<TimeInterval>(), IntervalMath.merge(emptyList()))
    }

    @Test
    fun `merge drops zero-length intervals`() {
        val result = IntervalMath.merge(listOf(TimeInterval(100, 100), TimeInterval(0, 50)))
        assertEquals(listOf(TimeInterval(0, 50)), result)
    }

    @Test
    fun `merge combines unsorted overlapping intervals`() {
        val result = IntervalMath.merge(
            listOf(
                TimeInterval(200, 300),
                TimeInterval(250, 400),
                TimeInterval(0, 100),
            ),
        )
        assertEquals(listOf(TimeInterval(0, 100), TimeInterval(200, 400)), result)
    }

    @Test
    fun `merge joins adjacent intervals without double counting`() {
        val result = IntervalMath.merge(
            listOf(TimeInterval(0, 100), TimeInterval(100, 200)),
        )
        assertEquals(listOf(TimeInterval(0, 200)), result)
    }

    @Test
    fun `merge keeps intervals separated by a 1ms gap`() {
        val result = IntervalMath.merge(
            listOf(TimeInterval(0, 100), TimeInterval(101, 200)),
        )
        assertEquals(listOf(TimeInterval(0, 100), TimeInterval(101, 200)), result)
    }

    @Test
    fun `merge absorbs interval contained in another`() {
        val result = IntervalMath.merge(
            listOf(TimeInterval(0, 1000), TimeInterval(100, 200), TimeInterval(900, 1000)),
        )
        assertEquals(listOf(TimeInterval(0, 1000)), result)
    }

    @Test
    fun `merge handles duplicate identical intervals`() {
        val result = IntervalMath.merge(
            listOf(TimeInterval(50, 100), TimeInterval(50, 100), TimeInterval(50, 100)),
        )
        assertEquals(listOf(TimeInterval(50, 100)), result)
    }

    @Test
    fun `unionDurationMs counts overlapping time once`() {
        val total = IntervalMath.unionDurationMs(
            listOf(
                TimeInterval(0, 100),
                TimeInterval(50, 150),
                TimeInterval(1000, 2000),
            ),
        )
        assertEquals(150L + 1000L, total)
    }

    @Test
    fun `unionDurationMs is zero for zero-length intervals`() {
        assertEquals(0L, IntervalMath.unionDurationMs(listOf(TimeInterval(5, 5))))
    }

    @Test
    fun `intersect returns overlap for intersecting intervals`() {
        assertEquals(
            TimeInterval(60, 100),
            IntervalMath.intersect(TimeInterval(0, 100), TimeInterval(60, 200)),
        )
    }

    @Test
    fun `intersect returns null for touching intervals`() {
        assertEquals(null, IntervalMath.intersect(TimeInterval(0, 100), TimeInterval(100, 200)))
    }
}
