package dev.paradox.trace.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BilibiliViewParserTest {

    @Test
    fun `normalizeBvid extracts id from url or bare input`() {
        assertEquals("BV1xx411c7mD", BilibiliViewParser.normalizeBvid("BV1xx411c7mD"))
        assertEquals(
            "BV1xx411c7mD",
            BilibiliViewParser.normalizeBvid("https://www.bilibili.com/video/BV1xx411c7mD?p=1"),
        )
        assertEquals("BV1xx411c7mD", BilibiliViewParser.normalizeBvid("  BV1xx411c7mD  "))
    }

    @Test
    fun `normalizeBvid rejects malformed input`() {
        assertNull(BilibiliViewParser.normalizeBvid("BV123"))
        assertNull(BilibiliViewParser.normalizeBvid("hello world"))
        assertNull(BilibiliViewParser.normalizeBvid(""))
    }

    @Test
    fun `parse extracts title creator and duration`() {
        val json = """
            {"code":0,"message":"0","data":{
              "bvid":"BV1xx411c7mD","title":"测试视频",
              "duration":621,
              "owner":{"mid":2,"name":"测试UP主","face":"x"}
            }}
        """.trimIndent()
        val preview = BilibiliViewParser.parse(json)
        assertEquals("BV1xx411c7mD", preview?.bvid)
        assertEquals("测试视频", preview?.title)
        assertEquals("测试UP主", preview?.creatorName)
        assertEquals(621L, preview?.durationSec)
    }

    @Test
    fun `parse returns null for error code and garbage`() {
        assertNull(BilibiliViewParser.parse("""{"code":-400,"message":"fail"}"""))
        assertNull(BilibiliViewParser.parse("not json"))
        assertNull(BilibiliViewParser.parse("""{"code":0,"data":{}}"""))
    }
}
