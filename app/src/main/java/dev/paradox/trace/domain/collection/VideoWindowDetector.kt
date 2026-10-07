package dev.paradox.trace.domain.collection

/**
 * Pure detector for Bilibili in-app video playback windows. The service only
 * treats these windows as content-bearing; every other window closes any
 * open observation. Kept heuristic and version-tolerant: class-name fragments
 * are matched case-insensitively so minor app updates do not break detection.
 */
object VideoWindowDetector {

    /** Class-name fragments observed for Bilibili video detail/player activities. */
    private val VIDEO_WINDOW_MARKERS = listOf("videodetail", "videoplayback", "storyvideo")

    fun isVideoWindow(windowClassName: String?): Boolean {
        val className = windowClassName?.lowercase() ?: return false
        return VIDEO_WINDOW_MARKERS.any { className.contains(it) }
    }
}
