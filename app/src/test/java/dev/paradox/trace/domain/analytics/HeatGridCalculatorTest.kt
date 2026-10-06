package dev.paradox.trace.domain.analytics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatGridCalculatorTest {

    @Test
    fun `grid spans whole weeks aligned to monday`() {
        // 2026-10-06 is a Tuesday.
        val grid = HeatGridCalculator.buildGrid(LocalDate.of(2026, 10, 6), weeks = 2)
        assertEquals(2, grid.size)
        assertEquals(7, grid[0].size)
        // First cell is Monday two weeks back: 2026-09-28.
        assertEquals("2026-09-28", grid[0][0])
        // Last column ends on Sunday 2026-10-11.
        assertEquals("2026-10-11", grid[1][6])
        // Cells after the end date are null: 2026-10-07..10-10.
        assertNull(grid[1][2])
        assertNull(grid[1][5])
    }

    @Test
    fun `levelFor maps ratios to five buckets`() {
        assertEquals(0, HeatGridCalculator.levelFor(0L, 100L))
        assertEquals(1, HeatGridCalculator.levelFor(10L, 100L))
        assertEquals(2, HeatGridCalculator.levelFor(40L, 100L))
        assertEquals(3, HeatGridCalculator.levelFor(60L, 100L))
        assertEquals(4, HeatGridCalculator.levelFor(90L, 100L))
    }

    @Test
    fun `levelFor is zero without any data`() {
        assertEquals(0, HeatGridCalculator.levelFor(100L, 0L))
    }

    @Test
    fun `grid requires positive weeks`() {
        var thrown = false
        try {
            HeatGridCalculator.buildGrid(LocalDate.of(2026, 10, 6), 0)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
