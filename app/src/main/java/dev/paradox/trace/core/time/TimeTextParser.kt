package dev.paradox.trace.core.time

/**
 * Parses user-entered clock text (`HH:mm`, 1-2 digit hours tolerated) into
 * minutes-of-day, or null when the input is not a valid wall-clock time.
 */
object TimeTextParser {

    private val PATTERN = Regex("^(\\d{1,2}):(\\d{1,2})$")

    /** Returns total minutes since midnight, e.g. "9:05" -> 545, or null. */
    fun parseMinutesOfDay(input: String): Int? {
        val match = PATTERN.matchEntire(input.trim()) ?: return null
        val hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}
