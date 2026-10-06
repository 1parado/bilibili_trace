package dev.paradox.trace.domain.remote

import dev.paradox.trace.domain.model.BilibiliContentPreview

/**
 * Anonymous metadata lookup. No cookies, no login: only the public view
 * endpoint is used, per docs/BILIBILI_API_REFERENCE.md §1 and §3.
 */
interface BilibiliMetadataService {
    suspend fun fetchByBvid(bvid: String): Result<BilibiliContentPreview>
}
