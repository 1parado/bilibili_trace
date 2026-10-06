package dev.paradox.trace.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Presentation-only time text helpers. Never reuse formatted text in math. */
object SessionTimeText {

    fun formatRange(startMs: Long, endMs: Long, zone: ZoneId): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(startMs), zone)) +
            " – " +
            formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(endMs), zone))
    }
}
