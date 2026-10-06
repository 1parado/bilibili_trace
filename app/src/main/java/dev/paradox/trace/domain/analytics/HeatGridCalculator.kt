package dev.paradox.trace.domain.analytics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds a GitHub-style calendar grid (weeks as columns, Monday-first rows)
 * over the trailing [weeks] weeks ending at [endDate]. Pure and testable.
 */
object HeatGridCalculator {

    private val DATE_KEY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE

    /** grid[weekIndex][dayOfWeekIndex 0=Mon..6=Sun] as date keys; null = outside range. */
    fun buildGrid(
        endDate: LocalDate,
        weeks: Int,
    ): List<List<String?>> {
        require(weeks > 0) { "weeks must be positive" }
        val lastDayOffset = endDate.dayOfWeek.value - DayOfWeek.MONDAY.value
        val gridEnd = endDate.plusDays((6 - lastDayOffset).toLong())
        val gridStart = gridEnd.minusDays((weeks * 7L) - 1)

        val grid = MutableList(weeks) { MutableList<String?>(7) { null } }
        var cursor = gridStart
        var weekIndex = 0
        while (!cursor.isAfter(gridEnd)) {
            val dayIndex = cursor.dayOfWeek.value - DayOfWeek.MONDAY.value
            // Dates after the anchor date stay null (rendered as invisible) so
            // the grid never implies future data.
            if (!cursor.isAfter(endDate)) {
                grid[weekIndex][dayIndex] = DATE_KEY_FORMAT.format(cursor)
            }
            if (dayIndex == 6) weekIndex++
            cursor = cursor.plusDays(1)
        }
        return grid
    }

    /** Relative level 0..4 for a daily total given the window maximum. */
    fun levelFor(totalMs: Long, maxMs: Long): Int {
        if (totalMs <= 0L || maxMs <= 0L) return 0
        val ratio = totalMs.toDouble() / maxMs.toDouble()
        return when {
            ratio <= 0.25 -> 1
            ratio <= 0.50 -> 2
            ratio <= 0.75 -> 3
            else -> 4
        }.coerceIn(1, 4)
    }
}
