package dev.paradox.trace.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyGoalTest {

    @Test
    fun `no goal returns null`() {
        assertNull(DailyGoal.remainingMinutes(0, 60_000L))
        assertNull(DailyGoal.remainingMinutes(-10, 60_000L))
    }

    @Test
    fun `remaining is floor of observed minutes`() {
        assertEquals(30, DailyGoal.remainingMinutes(90, 60_000L * 60 + 30_000L))
    }

    @Test
    fun `negative remaining means goal exceeded`() {
        assertEquals(-10, DailyGoal.remainingMinutes(50, 60_000L * 60))
    }
}
