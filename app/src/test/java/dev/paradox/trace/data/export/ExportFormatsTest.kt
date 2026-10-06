package dev.paradox.trace.data.export

import dev.paradox.trace.domain.model.ContentRef
import dev.paradox.trace.domain.model.ContentSession
import dev.paradox.trace.domain.model.EventSource
import dev.paradox.trace.domain.model.SessionCompleteness
import dev.paradox.trace.domain.model.TimeInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportFormatsTest {

    private fun session(id: String, title: String?, start: Long, end: Long) = ContentSession(
        id = id,
        platform = "bilibili",
        interval = TimeInterval(start, end),
        source = EventSource.USER_INPUT,
        completeness = SessionCompleteness.COMPLETE,
        content = ContentRef(
            platform = "bilibili",
            platformContentId = "BV1",
            title = title,
            creatorName = "UP主",
        ),
    )

    @Test
    fun `csv escapes commas quotes and newlines`() {
        val csv = ExportFormats.csv(listOf(session("s1", "标题, 含\"引号\"", 100L, 200L)))
        val lines = csv.trim().lines()
        assertEquals(ExportFormats.CSV_HEADER, lines.first())
        assertTrue(lines[1].contains("\"标题, 含\"\"引号\"\"\""))
        assertTrue(lines[1].contains(",100,200,"))
    }

    @Test
    fun `csv omits null fields as empty`() {
        val csv = ExportFormats.csv(listOf(session("s1", null, 100L, 200L)))
        assertTrue(csv.trim().lines()[1].endsWith(",,\"UP主\""))
    }

    @Test
    fun `json escapes special characters and includes schema version`() {
        val json = ExportFormats.json(
            listOf(session("s1", "含\"引号\"\n换行", 100L, 200L)),
            exportedAtMs = 999L,
        )
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"exportedAtUtcMs\":999"))
        assertTrue(json.contains("含\\\"引号\\\"\\n换行"))
        assertTrue(json.contains("\"startedAtUtcMs\":100"))
    }

    @Test
    fun `json of empty list is valid shape`() {
        val json = ExportFormats.json(emptyList(), 1L)
        assertEquals(
            "{\"schemaVersion\":1,\"exportedAtUtcMs\":1,\"timezoneNote\":\"all timestamps are UTC epoch milliseconds\",\"sessions\":[]}",
            json,
        )
    }
}
