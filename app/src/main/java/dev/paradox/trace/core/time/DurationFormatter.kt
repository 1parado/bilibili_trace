package dev.paradox.trace.core.time

import java.util.Locale

/**
 * Formats a non-negative duration for display.
 *
 * This is presentation-only: callers must not use the formatted value in calculations.
 */
object DurationFormatter {
    fun format(durationMs: Long, locale: Locale = Locale.getDefault()): String {
        require(durationMs >= 0) { "durationMs must be non-negative" }

        val totalSeconds = durationMs / MILLIS_PER_SECOND
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE

        return when {
            hours > 0 -> String.format(locale, "%d小时 %02d分", hours, minutes)
            minutes > 0 -> String.format(locale, "%d分 %02d秒", minutes, seconds)
            else -> String.format(locale, "%d秒", seconds)
        }
    }

    private const val MILLIS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
}
