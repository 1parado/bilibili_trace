# 数据模型与事件规范

## 1. 时间与口径约定

- 持久化时间使用 UTC epoch milliseconds。
- 展示时转换到设备当前本地时区；统计日按用户所选时区计算。
- 时间区间使用半开区间 `[startAt, endAt)`，避免相邻区间重复计时。
- 时长统一以毫秒存储或在计算中使用 Long，UI 再格式化。
- 不同时间概念不能混用：
  - **应用前台时长**：系统观测到目标应用处于前台的区间。
  - **内容可见区间**：识别到某条内容在界面上可见的估算区间。
  - **估算观看时长**：基于明确假设对可见区间进一步估算的指标，MVP 不应默认承诺精确值。
  - **学习投入/成果**：来自用户主动记录的笔记、复习、实践等行为。

## 2. 统一事件

建议 Kotlin 领域模型：

```kotlin
enum class EventSource {
    USAGE_STATS,
    ACCESSIBILITY,
    SHARE_INTENT,
    USER_INPUT,
    PLATFORM_API
}

enum class BehaviorEventType {
    APP_FOREGROUND,
    APP_BACKGROUND,
    CONTENT_DETECTED,
    CONTENT_CHANGED,
    CONTENT_SHARED,
    SESSION_ENDED,
    LEARNING_NOTE_CREATED,
    LEARNING_TASK_COMPLETED,
    USER_CORRECTION
}

data class BehaviorEvent(
    val id: String,
    val platform: String,
    val source: EventSource,
    val type: BehaviorEventType,
    val occurredAt: Long,
    val sessionId: String?,
    val contentId: String?,
    val confidence: Float?,
    val evidenceRef: String?,
    val dedupeKey: String?,
    val schemaVersion: Int = 1
)
```

说明：
- confidence 应限制在 0.0–1.0；对 UsageStats 这类系统事件，可使用 null 表示“不适用”，不要伪造概率。
- evidenceRef 只指向必要的结构化证据或最小化的本地记录；不得保存完整屏幕文本。
- source 是采集来源，不等于事实正确性。
- type 使用强类型枚举或 sealed class，不使用任意字符串。
- 所有事件应支持 schemaVersion 以便演进。

## 3. 核心实体

### app_sessions
- id: UUID / TEXT PRIMARY KEY
- platform: TEXT NOT NULL
- package_name: TEXT NOT NULL
- started_at: INTEGER NOT NULL
- ended_at: INTEGER NULL（活跃会话可为空）
- duration_ms: INTEGER NOT NULL DEFAULT 0
- source: TEXT NOT NULL
- completeness: TEXT NOT NULL（COMPLETE / PARTIAL / INTERRUPTED / ESTIMATED）
- created_at: INTEGER NOT NULL
- updated_at: INTEGER NOT NULL

### content_items
- id: TEXT PRIMARY KEY（优先使用规范化 URL / 平台内容 ID；不稳定时生成本地 ID）
- platform: TEXT NOT NULL
- platform_content_id: TEXT NULL
- canonical_url: TEXT NULL
- title: TEXT NULL
- creator_id: TEXT NULL
- creator_name: TEXT NULL
- user_topic: TEXT NULL
- metadata_source: TEXT NULL
- metadata_confidence: REAL NULL
- first_seen_at: INTEGER NULL
- last_seen_at: INTEGER NULL
- user_verified: INTEGER NOT NULL DEFAULT 0

### behavior_events
- id: TEXT PRIMARY KEY
- platform: TEXT NOT NULL
- source: TEXT NOT NULL
- event_type: TEXT NOT NULL
- occurred_at: INTEGER NOT NULL
- session_id: TEXT NULL
- content_id: TEXT NULL
- confidence: REAL NULL
- evidence_ref: TEXT NULL
- dedupe_key: TEXT NULL
- schema_version: INTEGER NOT NULL DEFAULT 1
- created_at: INTEGER NOT NULL

建议为 `occurred_at`、`session_id`、`content_id`、`dedupe_key` 建索引。dedupe_key 非空时可建立唯一约束，但要为来源生成稳定且合理的 key。

### content_exposures
- id: TEXT PRIMARY KEY
- session_id: TEXT NOT NULL
- content_id: TEXT NULL
- started_at: INTEGER NOT NULL
- ended_at: INTEGER NULL
- duration_ms: INTEGER NOT NULL DEFAULT 0
- estimation_method: TEXT NOT NULL
- confidence: REAL NULL
- user_confirmed: INTEGER NOT NULL DEFAULT 0

曝光区间表示识别到的界面状态区间，不等同于视频播放进度或注意力。

### learning_records
- id: TEXT PRIMARY KEY
- content_id: TEXT NULL
- goal_id: TEXT NULL
- record_type: TEXT NOT NULL（NOTE / REVIEW / PRACTICE / STATUS_CHANGE）
- body: TEXT NULL
- status: TEXT NULL
- due_at: INTEGER NULL
- completed_at: INTEGER NULL
- created_at: INTEGER NOT NULL
- updated_at: INTEGER NOT NULL

### learning_goals
- id: TEXT PRIMARY KEY
- title: TEXT NOT NULL
- description: TEXT NULL
- target_at: INTEGER NULL
- status: TEXT NOT NULL
- created_at: INTEGER NOT NULL
- updated_at: INTEGER NOT NULL

### creator_snapshots（可选）
- id: TEXT PRIMARY KEY
- creator_id: TEXT NOT NULL
- creator_name: TEXT NOT NULL
- snapshot_at: INTEGER NOT NULL
- source: TEXT NOT NULL
- is_followed: INTEGER NULL
- confidence: REAL NULL

关注状态只在用户明确授权且有合法、稳定来源时记录；不能为了生成“看过但未关注”对比而假设可获得关注列表。

### daily_summaries（可重建缓存）
- date_key: TEXT NOT NULL
- timezone_id: TEXT NOT NULL
- metric_key: TEXT NOT NULL
- metric_value: REAL NOT NULL
- definition_version: INTEGER NOT NULL
- generated_at: INTEGER NOT NULL
- PRIMARY KEY(date_key, timezone_id, metric_key)

## 4. 会话切分规则（初始建议）

- 按同一目标应用的系统事件构建区间。
- 若出现前台事件而无对应后台事件，按最近可确认边界与系统统计补齐，并将 completeness 标为 PARTIAL/ESTIMATED。
- 若应用切换间隔小于可配置阈值，可在产品层选择是否合并为同一“连续使用段”；原始区间仍保留。
- 不跨越明确的非目标应用前台区间计算 B 站前台时长。
- 设备重启或事件缺失后重建最近窗口，并记录修复来源。
- 时间重叠区间合并后再计时，避免重复累计。

阈值必须由实测决定，并可配置或版本化。

## 5. 去重策略

优先级：
1. 系统/平台原生事件 ID（若存在）。
2. 来源 + 事件类型 + 内容 ID + 规范化时间窗口形成的 dedupe key。
3. 对缺少稳定 ID 的可访问节点事件，使用短时间窗口内的字段指纹和状态变化判断。
4. 不能只按标题去重：同一视频可以被多次观看。
5. 不能仅依赖精确时间戳：同一事件可能被重复投递或有毫秒级抖动。

## 6. 数据更正与删除

- 将用户更正与自动采集结果区分；至少记录更正时间和更正字段。
- 删除内容时明确是否同时删除关联曝光和学习记录。
- “删除全部数据”应覆盖事件、会话、内容、学习记录、缓存和应用内导出临时文件。
- 任何缓存汇总都必须可从底层记录重建。
- 数据导出应带 schema/version、时间口径和来源说明。
