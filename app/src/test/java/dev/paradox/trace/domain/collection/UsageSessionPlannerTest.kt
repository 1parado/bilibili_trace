package dev.paradox.trace.domain.collection

import dev.paradox.trace.domain.collection.ForegroundUsageSource.ForegroundEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageSessionPlannerTest {

    private val targets = setOf("tv.danmaku.bili")

    private fun event(
        pkg: String = "tv.danmaku.bili",
        foreground: Boolean,
        atMs: Long,
    ) = ForegroundEvent(pkg, foreground, atMs)

    @Test
    fun `builds intervals from resume and pause pairs`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 61_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(1, planned.size)
        assertEquals(1_000L, planned[0].interval.startInclusiveMs)
        assertEquals(61_000L, planned[0].interval.endExclusiveMs)
    }

    @Test
    fun `multiple intervals per package stay distinct`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 2_000L),
                event(foreground = true, atMs = 10_000L),
                event(foreground = false, atMs = 70_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(listOf(1_000L to 2_000L, 10_000L to 70_000L), planned.map { it.interval.startInclusiveMs to it.interval.endExclusiveMs })
    }

    @Test
    fun `overlapping intervals merge so covered time counts once`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                // Second resume without an observed pause: same open session.
                event(foreground = true, atMs = 5_000L),
                event(foreground = false, atMs = 61_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(1, planned.size)
        assertEquals(60_000L, planned[0].interval.durationMs)
    }

    @Test
    fun `pause without resume is ignored`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = false, atMs = 5_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertTrue(planned.isEmpty())
    }

    @Test
    fun `session still open at window end is skipped`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 61_000L),
                event(foreground = true, atMs = 100_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(1, planned.size)
        assertEquals(1_000L, planned[0].interval.startInclusiveMs)
        assertEquals(61_000L, planned[0].interval.endExclusiveMs)
    }

    @Test
    fun `intervals below minimum duration are dropped as noise`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 2_000L), // 1s flicker
                event(foreground = true, atMs = 10_000L),
                event(foreground = false, atMs = 70_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
            minDurationMs = 5_000L,
        )

        assertEquals(1, planned.size)
        assertEquals(10_000L, planned[0].interval.startInclusiveMs)
    }

    @Test
    fun `non-target packages are filtered out`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(pkg = "com.other.app", foreground = true, atMs = 1_000L),
                event(pkg = "com.other.app", foreground = false, atMs = 60_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertTrue(planned.isEmpty())
    }

    @Test
    fun `events outside the window are ignored`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = -5_000L),
                event(foreground = false, atMs = 2_000L),
                event(foreground = true, atMs = 999_000L),
                event(foreground = false, atMs = 1_100_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertTrue(planned.isEmpty())
    }

    @Test
    fun `adjacent intervals of the same package merge`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 61_000L),
                // System reports a second session starting exactly at the boundary.
                event(foreground = true, atMs = 61_000L),
                event(foreground = false, atMs = 121_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(1, planned.size)
        assertEquals(1_000L, planned[0].interval.startInclusiveMs)
        assertEquals(121_000L, planned[0].interval.endExclusiveMs)
    }

    @Test
    fun `output is sorted by interval start`() {
        val planned = UsageSessionPlanner.plan(
            events = listOf(
                event(foreground = true, atMs = 100_000L),
                event(foreground = false, atMs = 160_000L),
                event(foreground = true, atMs = 1_000L),
                event(foreground = false, atMs = 61_000L),
            ),
            targetPackages = targets,
            windowStartMs = 0L,
            windowEndMs = 1_000_000L,
        )

        assertEquals(listOf(1_000L, 100_000L), planned.map { it.interval.startInclusiveMs })
    }
}
