package dev.paradox.trace.domain.collection

/**
 * Pure extractor: a11y node text candidates -> the most plausible video title.
 * Heuristics are deterministic and unit-tested; a wrong guess is metadata
 * with [MIN_TITLE_LENGTH] guardrails, never a fabricated statistic.
 */
object VideoTitleExtractor {

    const val MIN_TITLE_LENGTH = 4
    private const val MAX_TEXT_LENGTH = 200

    /** One text-bearing node visited in document order. */
    data class Candidate(
        val viewIdResourceName: String?,
        val text: String?,
        val className: String?,
    )

    /** Common Bilibili page controls that must never become a "title". */
    private val UI_LABELS = setOf(
        "点赞", "投币", "收藏", "分享", "弹幕", "评论", "选集", "关注", "追番", "倍速",
        "清晰度", "播放", "暂停", "稍后再看", "下载", "举报", "稿件投诉", "简介", "充电",
        "合集", "三连", "发送", "详情",
    )

    private val TIME_PATTERN = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")

    /**
     * Preference order: a node whose view id is named like a title, otherwise
     * the longest plausible text. Returns null when nothing plausible exists.
     */
    fun extract(candidates: List<Candidate>): String? {
        val namedTitle = candidates.firstOrNull { candidate ->
            val text = candidate.text?.trim()
            text != null && text.length >= MIN_TITLE_LENGTH &&
                candidate.viewIdResourceName
                    ?.substringAfterLast('/')
                    ?.contains("title", ignoreCase = true) == true
        }?.text?.trim()
        if (namedTitle != null) return namedTitle.take(MAX_TEXT_LENGTH)

        return candidates.asSequence()
            .mapNotNull { it.text?.trim() }
            .filter { it.length >= MIN_TITLE_LENGTH }
            .filterNot { it in UI_LABELS }
            .filterNot { TIME_PATTERN.matches(it) }
            .filterNot { it.contains("://") }
            .maxByOrNull { it.length }
            ?.take(MAX_TEXT_LENGTH)
    }
}
