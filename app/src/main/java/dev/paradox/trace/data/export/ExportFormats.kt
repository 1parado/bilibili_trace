package dev.paradox.trace.data.export

import dev.paradox.trace.domain.model.ContentSession

/**
 * Deterministic export formatting. Times stay as UTC epoch milliseconds and
 * the export carries its schema version so future imports can evolve.
 */
object ExportFormats {

    const val SCHEMA_VERSION: Int = 1
    const val CSV_HEADER: String =
        "id,platform,started_at_utc_ms,ended_at_utc_ms,duration_ms,source,completeness,content_platform_id,title,creator_name"

    fun csv(sessions: List<ContentSession>): String = buildString {
        appendLine(CSV_HEADER)
        sessions.forEach { session ->
            appendLine(
                listOf(
                    session.id,
                    session.platform,
                    session.interval.startInclusiveMs.toString(),
                    session.interval.endExclusiveMs.toString(),
                    session.interval.durationMs.toString(),
                    session.source.name,
                    session.completeness.name,
                    session.content?.platformContentId,
                    session.content?.title,
                    session.content?.creatorName,
                ).joinToString(separator = ",") { escapeCsv(it) },
            )
        }
    }

    fun json(sessions: List<ContentSession>, exportedAtMs: Long): String = buildString {
        append("{")
        append("\"schemaVersion\":$SCHEMA_VERSION,")
        append("\"exportedAtUtcMs\":$exportedAtMs,")
        append("\"timezoneNote\":\"all timestamps are UTC epoch milliseconds\",")
        append("\"sessions\":[")
        append(
            sessions.joinToString(separator = ",") { session ->
                buildString {
                    append("{")
                    append("\"id\":${jsonString(session.id)},")
                    append("\"platform\":${jsonString(session.platform)},")
                    append("\"startedAtUtcMs\":${session.interval.startInclusiveMs},")
                    append("\"endedAtUtcMs\":${session.interval.endExclusiveMs},")
                    append("\"durationMs\":${session.interval.durationMs},")
                    append("\"source\":${jsonString(session.source.name)},")
                    append("\"completeness\":${jsonString(session.completeness.name)},")
                    append("\"title\":${session.content?.title?.let(::jsonString) ?: "null"},")
                    append("\"creatorName\":${session.content?.creatorName?.let(::jsonString) ?: "null"}")
                    append("}")
                }
            },
        )
        append("]}")
    }

    private fun escapeCsv(raw: String?): String {
        val value = raw ?: return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun jsonString(raw: String): String = buildString {
        append('"')
        raw.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
}
