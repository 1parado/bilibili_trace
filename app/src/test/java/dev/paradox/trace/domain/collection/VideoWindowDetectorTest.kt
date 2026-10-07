package dev.paradox.trace.domain.collection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoWindowDetectorTest {

    @Test
    fun `main video details activity is a video window`() {
        assertTrue(
            VideoWindowDetector.isVideoWindow(
                "com.bilibili.video.videodetail.VideoDetailsActivity",
            ),
        )
    }

    @Test
    fun `detection is case insensitive`() {
        assertTrue(VideoWindowDetector.isVideoWindow("com.example.app.VideoDetailActivity"))
        assertTrue(VideoWindowDetector.isVideoWindow("com.example.app.VIDEOPLAYBACKFragment"))
    }

    @Test
    fun `non-video windows are rejected`() {
        assertFalse(VideoWindowDetector.isVideoWindow("com.bilibili.app.home.HomeActivity"))
        assertFalse(VideoWindowDetector.isVideoWindow("android.widget.TextView"))
    }

    @Test
    fun `null class name is rejected`() {
        assertFalse(VideoWindowDetector.isVideoWindow(null))
    }
}
