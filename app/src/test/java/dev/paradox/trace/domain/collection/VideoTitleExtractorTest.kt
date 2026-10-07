package dev.paradox.trace.domain.collection

import dev.paradox.trace.domain.collection.VideoTitleExtractor.Candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoTitleExtractorTest {

    private fun candidate(
        text: String?,
        viewId: String? = null,
        className: String? = "android.widget.TextView",
    ) = Candidate(viewId, text, className)

    @Test
    fun `prefers a node whose view id names it as title`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("点赞", viewId = "tv.danmaku.bili:id/like_count"),
                candidate("一个很长的评论区评论内容", viewId = "tv.danmaku.bili:id/comment_text"),
                candidate("【实测】某视频标题", viewId = "tv.danmaku.bili:id/title"),
            ),
        )

        assertEquals("【实测】某视频标题", title)
    }

    @Test
    fun `falls back to the longest plausible text`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("弹幕"),
                candidate("UP主的名字"),
                candidate("【实测】这是一条足够长的视频标题"),
            ),
        )

        assertEquals("【实测】这是一条足够长的视频标题", title)
    }

    @Test
    fun `excludes ui control labels and timestamps`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("点赞"),
                candidate("12:34"),
                candidate("01:23:45"),
                candidate("正经视频标题内容"),
            ),
        )

        assertEquals("正经视频标题内容", title)
    }

    @Test
    fun `excludes urls`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("https://www.bilibili.com/video/BV1xx411c7mD"),
                candidate("一个真实的视频标题哦"),
            ),
        )

        assertEquals("一个真实的视频标题哦", title)
    }

    @Test
    fun `text below minimum length is ignored`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("赞"),
                candidate("abc"),
            ),
        )

        assertNull(title)
    }

    @Test
    fun `empty input yields null`() {
        assertNull(VideoTitleExtractor.extract(emptyList()))
        assertNull(VideoTitleExtractor.extract(listOf(candidate(null), candidate("   "))))
    }

    @Test
    fun `named title view id wins even when shorter text exists`() {
        val title = VideoTitleExtractor.extract(
            listOf(
                candidate("超长评论区内容超长评论区内容超长评论区内容超长评论区内容"),
                candidate("短标题啦", viewId = "tv.danmaku.bili:id/video_title"),
            ),
        )

        assertEquals("短标题啦", title)
    }
}
