<p align="center">
  <img src="docs/assets/bilibili-trace-logo.svg" alt="bilibili-trace project logo" width="150" />
</p>

<h1 align="center">Trace — 个人数字行为分析器</h1>

<p align="center">
  Android-first · Local-first · Privacy-first
</p>

Trace 是一款 Android 优先、隐私优先的个人内容消费行为分析工具。首个数据源为哔哩哔哩（Bilibili），关注时间管理、行为分析和学习复盘。

> 产品边界：Trace 不复制 B 站已有的历史记录、收藏或关注管理。它关注跨时间的行为轨迹、时间分配、兴趣变化，以及“看完之后做了什么”。

## 项目图标

项目 Logo 源文件：[`docs/assets/bilibili-trace-logo.svg`](docs/assets/bilibili-trace-logo.svg)。

Android 启动器使用纯矢量自适应图标（Adaptive Icon，`mipmap-anydpi-v26`），无需位图资源：

- **背景层**：B 站品牌蓝（`#1680DB`）全出血底色；
- **前景层**：白色吉祥物（电视 + 挂钟）矢量，全部绘制在中心 66dp 安全区内，圆形 / 圆角方形 / 方圆形等各类启动器蒙版均不会裁切；
- **主题图标（monochrome）**：声明于 Android 13+，支持桌面长按切换单色主题图标。

minSdk 为 26，`mipmap-anydpi-v26` 已覆盖全部支持版本，因此不提供 legacy PNG 图标。

## Repository status

Trace 现已包含**可用的本地 MVP**：手动记录、分享导入、统计与时间线、目标与数据导出。自动采集（UsageStats / 无障碍识别）仍按路线图分阶段实现，以测试结果为准。

## Current features

- **记录观看**：手动录入（标题 / BV 号 / UP主 / 起止时间），支持从 B 站 App 分享链接自动识别 BV 号并预填元数据
- **今日概览**：今日时长、近 7 天合计、每日目标进度、最近记录、内容排行
- **时间线**：24 小时观看色带 + 日期回溯（近 30 天）+ 近 5 周热力图
- **设置**：每日观看目标（DataStore 本地存储）、CSV / JSON 导出（带 schema 版本）、一键清除全部数据
- **隐私**：本地优先，无账号、无云端、无遥测；内容元数据仅存本机；非官方接口仅使用匿名视频详情端点（见 [Bilibili API reference](docs/BILIBILI_API_REFERENCE.md)）

## 下载安装

前往 [Releases](https://github.com/1parado/bilibili_trace/releases) 下载最新的 `trace-vX.Y.Z.apk` 直接安装即可。当前发布为 **debug 签名**构建（未配置发布签名密钥），不影响功能使用；正式签名版本将在签名流程就绪后提供。

## 使用流程

1. 在 B 站看完视频后，通过系统分享菜单把视频链接分享给 Trace（或手动记录）；
2. Trace 自动获取标题、UP主与时长，确认起止时间后保存；
3. 在「今日」查看统计与排行，在「时间线」回顾全天与近 5 周轨迹；
4. 在「设置」设定每日目标、导出或清除数据。

所有时间戳以 UTC 毫秒存储，按本地时区展示；重叠时段在统计中只计一次。

## Start here

1. Read [AGENTS.md](AGENTS.md) before asking an AI coding agent to change code.
2. Read [PRD](docs/PRD.md), [architecture](docs/ARCHITECTURE.md), [data model](docs/DATA_MODEL.md), [collection and privacy](docs/ANDROID_DATA_COLLECTION.md), [analytics](docs/ANALYTICS.md), and [roadmap](docs/ROADMAP.md).
3. Follow [development workflow](docs/DEVELOPMENT_WORKFLOW.md) and [quality gates](docs/QUALITY_GATES.md).
4. Open the `app/` project in Android Studio, or use the Gradle commands documented below.

## Initial Android stack

- Kotlin 2.2 + Jetpack Compose + Material 3（设计令牌见 [UI design reference](docs/UI_DESIGN_REFERENCE.md)）
- Room（本地持久化，schema 导出）+ DataStore（偏好）
- JUnit 单元测试（时间引擎、聚合、导出、解析器）
- GitHub Actions：build + lint + unit test 每次推送验证

The app is local-first. No backend, account system, remote AI, or telemetry is involved; the only network call is the optional anonymous Bilibili view metadata lookup.

## Build and test

The CI workflow provisions JDK and Android SDK and runs Gradle tasks. For local development, use Android Studio's Gradle sync and run configuration. If using a local Gradle installation, run:

```powershell
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

A Gradle Wrapper should be added and verified as part of the first local Android Studio bootstrap; do not generate or commit an unverified wrapper binary. Once the wrapper is present, prefer `./gradlew` (Windows: `gradlew.bat`) over a globally installed Gradle.

## Privacy and data semantics

- App foreground time is not the same as actual video watch time.
- Accessibility-based content recognition is optional and must be separately disclosed and enabled.
- Personal behavior data stays local by default.
- Never commit secrets, cookies, credentials, real user behavior logs, or screenshots containing personal data.

## Documentation

- [PRD](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Data model](docs/DATA_MODEL.md)
- [Android data collection and privacy](docs/ANDROID_DATA_COLLECTION.md)
- [Analytics definitions](docs/ANALYTICS.md)
- [Bilibili API reference](docs/BILIBILI_API_REFERENCE.md)
- [UI design reference](docs/UI_DESIGN_REFERENCE.md)
- [MVP validation](docs/MVP_VALIDATION.md)
- [Roadmap](docs/ROADMAP.md)
- [Development workflow](docs/DEVELOPMENT_WORKFLOW.md)
- [Quality gates](docs/QUALITY_GATES.md)
- [Agent engineering contract](AGENTS.md)
