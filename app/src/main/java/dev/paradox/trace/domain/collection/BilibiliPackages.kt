package dev.paradox.trace.domain.collection

/**
 * Identity of the one supported content platform and its app packages.
 * Package-level foreground time is the only signal captured at this stage;
 * per-video detail is a separate, later capability (docs/ANDROID_DATA_COLLECTION.md).
 */
object BilibiliPackages {

    const val PLATFORM = "bilibili"

    val APP_PACKAGES: Set<String> = setOf(
        "tv.danmaku.bili", // 主站
        "tv.danmaku.bilibilihd", // HD 版
        "com.bilibili.app.in", // 国际版（含港澳台地区）
    )
}
