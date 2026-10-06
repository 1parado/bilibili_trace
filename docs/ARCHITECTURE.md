# 技术架构

## 1. 架构原则

- Android 原生、离线优先、模块边界清晰。
- 先做单体应用；MVP 不引入后端、微服务或消息队列。
- 采集层只负责观测，领域层负责标准化，分析层负责可解释计算，UI 层负责呈现。
- 每个数据点都保留来源和置信度。
- 可选能力失败不得导致基础时间统计不可用。

## 2. 逻辑分层

```text
Android Platform
 ├─ UsageStatsManager adapter
 ├─ Optional AccessibilityService adapter
 ├─ ACTION_SEND / share-intent adapter
 └─ User input adapter
          │
          ▼
Collection & Normalization
 ├─ Permission/status checks
 ├─ Event normalization
 ├─ Sessionization and deduplication
 └─ Confidence/evidence metadata
          │
          ▼
Domain Layer
 ├─ App session
 ├─ Content item / creator / topic
 ├─ Exposure interval
 ├─ Learning record
 └─ Analytics definitions
          │
          ▼
Local Persistence (Room / SQLite)
 ├─ Raw normalized events
 ├─ Derived sessions and exposures
 ├─ User corrections
 └─ Daily aggregates (rebuildable)
          │
          ▼
Use Cases
 ├─ Timeline
 ├─ Time analytics
 ├─ Interest trends
 ├─ Learning review
 ├─ Export / delete
 └─ Weekly report
          │
          ▼
Jetpack Compose UI
```

## 3. 建议模块（按规模渐进拆分）

MVP 可以先采用一个 Android app module，并在包结构上分层。只有在依赖或团队规模需要时再拆 Gradle module。

- `platform.usage`: UsageStats 权限、事件读取与应用会话。
- `platform.accessibility`: 可选的无障碍节点观察和内容候选提取。
- `platform.share`: 分享 Intent 与链接输入。
- `data.local`: Room entities、DAO、migration、repository 实现。
- `domain.model`: 事件、会话、内容、学习记录等类型。
- `domain.usecase`: 时间线、纠错、统计、导出与删除用例。
- `domain.analytics`: 纯函数指标和趋势规则。
- `feature.today`, `feature.timeline`, `feature.insights`, `feature.learning`, `feature.settings`: Compose 页面。
- `core.time`, `core.privacy`, `core.result`: 时间、隐私和结果类型。

## 4. 数据流

1. 平台适配器生成候选观测。
2. Normalizer 将平台事件转换为统一领域事件，并记录 source、时间和置信度。
3. Deduplicator 根据来源事件 ID、时间窗口和字段指纹消除重复。
4. Sessionizer 生成应用使用会话；内容识别生成独立的内容曝光区间。
5. Repository 事务性保存事件与派生记录。
6. Analytics 从明确的查询范围计算指标，避免把 UI 临时状态当作事实。
7. UI 展示数据口径、覆盖率与异常状态。
8. 用户纠错以明确的 correction 记录或更新字段保存，避免无痕覆盖采集原始事实。

## 5. 关键设计决定

### 本地数据库
Room/SQLite 是 MVP 的主存储。日汇总可缓存，但必须能从底层事件重建，防止汇总和明细不一致。

### 异步工作
使用 Coroutines/Flow 处理本地异步任务。WorkManager 只用于确有必要的可延迟工作（例如用户触发的大型导出或汇总）；不要假定它能保证实时记录前台切换。

### 服务端
MVP 不需要服务端。只有用户验证了跨设备同步、远程备份或共享报告的明确需求后，再评估后端。届时应优先设计端到端数据最小化、加密、账号删除和冲突解决，而不是直接引入微服务。

### AI
MVP 先用确定性规则生成洞察。未来 AI 仅作为语言表达层，输入已聚合且经用户授权的数据，输出必须关联证据 ID 和计算结果。不要让模型直接访问完整本地数据库。

## 6. 可靠性

- 权限撤销、设备重启、应用升级、事件重复/乱序均为预期情况。
- 对时间线计算设置重算窗口，允许晚到事件修正最近的会话。
- 数据库写入采用事务；派生数据支持幂等重建。
- 内容识别异常必须有降级路径，不能阻塞 UsageStats 数据流。
- 使用状态流展示“权限未授予”“无数据”“部分识别”“采集中断”，不要静默显示零值。

## 7. 安全边界

- 不存储 B 站 Cookie、密码或账号令牌。
- 不依赖未公开接口作为基础功能的唯一数据来源。
- 无障碍服务仅观察用户明确选择的目标应用和最少必要的可访问字段。
- 默认不上传数据；未来远程功能应单独开关并明确展示发送范围。
