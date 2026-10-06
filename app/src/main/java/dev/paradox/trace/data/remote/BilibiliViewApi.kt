package dev.paradox.trace.data.remote

import dev.paradox.trace.domain.model.BilibiliContentPreview
import dev.paradox.trace.domain.remote.BilibiliMetadataService
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * HTTP access to the anonymous view endpoint. Network work is confined to
 * IO dispatcher; parsing lives in [BilibiliViewParser] so it stays testable.
 */
class BilibiliViewApi(
    private val baseUrl: String = DEFAULT_BASE_URL,
) : BilibiliMetadataService {

    override suspend fun fetchByBvid(bvid: String): Result<BilibiliContentPreview> = runCatching {
        val normalized = BilibiliViewParser.normalizeBvid(bvid)
            ?: throw IOException("invalid bvid: $bvid")
        val body = withContext(Dispatchers.IO) { fetch(normalized) }
        BilibiliViewParser.parse(body)
            ?: throw IOException("unparsable response for $normalized")
    }

    private fun fetch(bvid: String): String {
        val connection = URL("$baseUrl/x/web-interface/view?bvid=${URLEncoder.encode(bvid, "UTF-8")}")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Referer", "https://www.bilibili.com/")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("view api responded $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_BASE_URL = "https://api.bilibili.com"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}

/** Pure JSON response parsing; org.json is provided on-device and via test dep on JVM. */
object BilibiliViewParser {

    private val BVID_PATTERN = Regex("BV[0-9A-Za-z]{10}")

    /** Extracts the first well-formed BV id from raw user input (URL tolerated). */
    fun normalizeBvid(input: String): String? = BVID_PATTERN.find(input)?.value

    /** Returns null for error codes or unexpected shapes; never throws. */
    fun parse(raw: String): BilibiliContentPreview? {
        return try {
            val root = JSONObject(raw)
            val code = root.optInt("code", -1)
            if (code != 0) return null
            val data = root.optJSONObject("data") ?: return null
            val bvid = data.optString("bvid", "")
            val title = data.optString("title", "")
            val duration = data.optLong("duration", 0L)
            val ownerName = data.optJSONObject("owner")?.optString("name", "") ?: ""
            if (bvid.isEmpty() || title.isEmpty() || duration <= 0L) return null
            BilibiliContentPreview(
                bvid = bvid,
                title = title,
                creatorName = ownerName,
                durationSec = duration,
            )
        } catch (e: Exception) {
            null
        }
    }
}
