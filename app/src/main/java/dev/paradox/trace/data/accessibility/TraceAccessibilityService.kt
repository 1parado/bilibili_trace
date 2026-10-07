package dev.paradox.trace.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.paradox.trace.TraceApplication
import dev.paradox.trace.domain.collection.BilibiliPackages
import dev.paradox.trace.domain.collection.VideoTitleExtractor
import dev.paradox.trace.domain.collection.VideoWindowDetector
import dev.paradox.trace.domain.repository.AccessibilitySessionCommand
import dev.paradox.trace.domain.repository.SessionRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Accessibility observation of Bilibili video playback.
 *
 * Scope, by design and by AGENTS.md contract:
 * - only windows whose class matches a Bilibili video detail/player activity;
 *   every other window closes the open observation immediately;
 * - only title-level text candidates are extracted; no screen text, input,
 *   passwords, or screenshots are ever stored;
 * - nothing is automated (no clicks, likes, follows, coins);
 * - the user can stop the service at any time from system settings, which
 *   also discards any open (unfinished) observation.
 */
class TraceAccessibilityService : AccessibilityService() {

    private val sessionRepository: SessionRepository by lazy {
        (application as TraceApplication).sessionRepository
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var sessionStartMs: Long? = null
    private var currentTitle: String? = null
    private var currentPackageName: String? = null
    private var lastTitleScanAtMs: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val now = System.currentTimeMillis()
        val packageName = event.packageName?.toString()
        val isVideoWindow = packageName in BilibiliPackages.APP_PACKAGES &&
            VideoWindowDetector.isVideoWindow(event.className?.toString())

        // Leaving the video page (or the app) closes the open observation.
        if (!isVideoWindow) {
            closeSession(now)
            return
        }

        // Titles load asynchronously after navigation; re-scan throttled.
        if (now - lastTitleScanAtMs < TITLE_SCAN_THROTTLE_MS) return
        lastTitleScanAtMs = now

        val title = VideoTitleExtractor.extract(collectCandidates(rootInActiveWindow)) ?: return

        val start = sessionStartMs
        when {
            start == null -> beginSession(now, title, packageName)
            title != currentTitle -> {
                closeSession(now)
                beginSession(now, title, packageName)
            }
            now - start > MAX_SESSION_MS -> {
                // Watchdog against a stuck state: split into capped chunks.
                closeSession(start + MAX_SESSION_MS)
                beginSession(now, title, packageName)
            }
        }
    }

    override fun onInterrupt() {
        closeSession(System.currentTimeMillis())
    }

    override fun onDestroy() {
        closeSession(System.currentTimeMillis())
        scope.cancel()
        super.onDestroy()
    }

    private fun beginSession(startMs: Long, title: String, packageName: String?) {
        sessionStartMs = startMs
        currentTitle = title
        currentPackageName = packageName
    }

    private fun closeSession(endMs: Long) {
        val start = sessionStartMs ?: return
        val title = currentTitle
        val packageName = currentPackageName
        sessionStartMs = null
        currentTitle = null
        currentPackageName = null
        if (title == null || packageName == null) return
        val end = minOf(endMs, start + MAX_SESSION_MS)
        if (end - start < MIN_SESSION_MS) return

        scope.launch {
            try {
                sessionRepository.addAccessibilitySession(
                    AccessibilitySessionCommand(
                        packageName = packageName,
                        title = title,
                        startedAtMs = start,
                        endedAtMs = end,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: RuntimeException) {
                // Persisting an observation must never crash the service host.
            }
        }
    }

    private fun collectCandidates(root: AccessibilityNodeInfo?): List<VideoTitleExtractor.Candidate> {
        if (root == null) return emptyList()
        val candidates = mutableListOf<VideoTitleExtractor.Candidate>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && candidates.size + queue.size <= MAX_VISITED_NODES) {
            val node = queue.removeFirst()
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) {
                candidates += VideoTitleExtractor.Candidate(
                    viewIdResourceName = node.viewIdResourceName,
                    text = text,
                    className = node.className?.toString(),
                )
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                if (candidates.size + queue.size < MAX_VISITED_NODES) queue.add(child)
            }
        }
        return candidates
    }

    private companion object {
        const val TITLE_SCAN_THROTTLE_MS = 5_000L
        const val MIN_SESSION_MS = 5_000L
        const val MAX_SESSION_MS = 4L * 60 * 60 * 1000
        const val MAX_VISITED_NODES = 300
    }
}
