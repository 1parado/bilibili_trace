# Bilibili 接口参考（可选数据源）

> 来源：提炼自第三方客户端 [yearzen1/bilibili-pure-apk](https://github.com/yearzen1/bilibili-pure-apk)（2026-10 快照）。这些是**非官方 Web 接口**，随时可能变动或加强风控。

## 1. 定位与边界（必须先读）

遵循 [AGENTS.md](../AGENTS.md) 与 [ANDROID_DATA_COLLECTION.md](ANDROID_DATA_COLLECTION.md)：

1. **非官方 API 不得成为关键路径依赖。** Trace 的主路径始终是本地优先（UsageStats / 可选的 Accessibility 采集）；本页接口只作为**用户显式启用的可选数据源**，用于拉取用户自己的历史与收藏做本地分析。
2. 登录采用**扫码**，禁止让用户手动粘贴 Cookie；禁止把登录态当作行为数据收集、打印或导出。
3. 只读优先：MVP 阶段仅使用只读接口；任何写操作（点赞/投币/评论）不属于 Trace 的目标，不做。
4. 拉取的数据落本地 Room，遵循与其它来源相同的去标识与可删除语义；用户登出或清除数据时必须连同源事件、派生记录、缓存一起删除。
5. 请求必须客户端限速（建议 ≤ 1 req/s，翻页加抖动），降低风控与封禁风险。

## 2. 接口与 Trace 指标概念的映射

| 接口字段 | Trace 概念 | 注意 |
| --- | --- | --- |
| `view_at`（秒级 Unix） | 观看事件时间戳 | 转成 UTC epoch ms 落库；区分秒/毫秒，避免数量级错误 |
| `progress` / `duration` | **估算观看时长** | 仅是播放器上报进度，不等于前台时长，更不等于实际观看；作为"估算观看"单独分类 |
| `title` / `pic` / `owner` | 内容元数据（本地展示索引） | 默认不写入日志；用户可清除；分析聚合只依赖 bvid/分区等标识 |
| `bvid` / `cid` / `page` | 分 P 进度关联键 | 多 P 视频按 `(bvid, cid)` 区分 |
| 分区/标签（来自 view 接口） | 兴趣分类维度 | 属派生维度，需记录来源版本 |

**禁止**：把「无历史记录」当成「没看」；拉取不完整时必须显示部分数据标记。

## 3. 只读接口清单（MVP 候选）

Base URL：`https://api.bilibili.com`（登录接口在 `https://passport.bilibili.com`）

| 接口 | 用途 | 鉴权 | 关键参数 |
| --- | --- | --- | --- |
| `GET /x/v2/history` | 观看历史（核心数据源） | Cookie | `pn` 页码、`ps` 每页（建议 ≤ 20） |
| `GET /x/web-interface/history/search` | 按标题/UP主 搜索历史 | Cookie | `keyword`、`pn`、`business=all` |
| `GET /x/web-interface/view` | 视频详情（分区、标签、分 P） | 可匿名 | `bvid` |
| `GET /x/v3/fav/folder/created/list-all` | 收藏夹列表 | Cookie | `up_mid`（自己的 mid） |
| `GET /x/v3/fav/resource/list` | 收藏夹内容分页 | Cookie | `media_id`、`pn`、`ps=20` |
| `GET /x/web-interface/search/type` | 站内搜索（补充元数据） | Cookie + WBI | `search_type=video`、`keyword`、`order`、`page` |

### HistoryItem 核心字段（`/x/v2/history` 返回项）

```text
bvid, aid, cid              // 内容标识
title, pic, owner{mid,name} // 元数据（本地展示用）
duration                    // 视频总时长（秒）
progress                    // 观看到的位置（秒；-1 表示看完）
view_at                     // 最后观看时间（秒级 Unix）
page { page, part }         // 分 P 信息
videos                      // 分 P 总数
```

## 4. 登录：扫码方案

参考实现流程（`passport.bilibili.com`）：

1. `GET /x/passport-login/web/qrcode/generate` → 得到 `qrcode_key` 与二维码 `url`；
2. 本地渲染二维码，用户用 B 站 App 扫码确认；
3. 轮询 `GET /x/passport-login/web/qrcode/poll?qrcode_key=...`（建议 1–2s 间隔），返回码 `0` 成功后从 `Set-Cookie` 取 `SESSDATA`、`bili_jct`（csrf）、`DedeUserID`。

**凭据存储要求**：仅存 `EncryptedSharedPreferences`（Android Keystore 包裹）；绝不进入日志、导出文件、备份（`allowBackup=false` 已保证）；登出即删；UI 明示"仅本机保存"。

## 5. 请求规范

- **Headers**（缺一易被风控）：
  - `User-Agent`：桌面 Chrome UA 即可；
  - `Referer: https://www.bilibili.com/`；
  - `Cookie`：业务接口带 `SESSDATA=...; buvid3=<本地生成随机值>`；写操作才需要 `bili_jct`。
- **WBI 签名**（`/x/space/wbi/arc/search`、`/wbi/*` 路径需要）：
  1. `GET /x/web-interface/nav` 取 `wbi_img.img_url` / `sub_url` 文件名作为 `img_key`、`sub_key`，缓存 24h；
  2. 按 WBI 算法生成 mixin key 重排表，对查询参数排序拼接后追加 mixin key 做 MD5，得到 `w_rid`，与 `wts` 一起并入查询串。
- **响应约定**：JSON 外层 `{ code, message, data }`；`code=0` 成功，`-101` 未登录，`-412` 风控拦截，`-352` 请求校验失败。

## 6. 失效与降级

| 情况 | 处理 |
| --- | --- |
| 接口 404 / 字段变更 | 功能整体降级为"仅本地采集"，UI 明示来源缺失，不得伪造数据 |
| `-412` / `-352` 风控 | 指数退避后重试一次，仍失败则暂停同步并告知用户，不自动反复重试 |
| 登录态过期（`-101`） | 标记会话失效，引导重新扫码，不清除已落库数据 |
| 拉取不完整 / 中断 | 记录同步游标与覆盖率，分析页显示部分数据标记 |

## 7. 测试要求

- 时间语义：`view_at` 秒→毫秒转换、跨日/跨时区切分、重复拉取去重（以 `(bvid, cid, view_at)` 幂等）。
- 失败路径：接口不可达、部分分页失败、登录态过期、空历史，均不得产生错误统计或崩溃。
- 单元测试覆盖 WBI mixin key 排重排与参数编码；接口层用可注入的假传输层测试，CI 不访问真实 B 站。
