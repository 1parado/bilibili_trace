package dev.paradox.trace.domain.model

/** Minimal video metadata returned by the anonymous view API. */
data class BilibiliContentPreview(
    val bvid: String,
    val title: String,
    val creatorName: String,
    val durationSec: Long,
)
