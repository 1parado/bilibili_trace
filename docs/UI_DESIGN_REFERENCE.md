# UI 设计参考：移植「知时」设计语言

> 来源：[1parado/zhishi](https://github.com/1parado/zhishi)（知时 · 注意力与健康浏览器扩展），其令牌体系移植自 @paradox/ui（shadcn 体系）。本文把它映射到 Trace 的 Compose Material 3 实现。

## 1. 设计目标

安静、克制、低信息噪音。对应 [AGENTS.md](../AGENTS.md) 的 "calm, legible, light-first, low-noise"：

1. 一屏一个主问题，其余信息渐进披露（默认 7 行 + "查看更多"）。
2. 每行只显示当前排序依据的那个维度，另一维度仍在后台记录，切换控件即切换展示。
3. 大量留白、最轻的边框与阴影、几乎无动效（仅开关与进度有过渡）。
4. 空态与"数据不足"显式呈现，不用灰色零值冒充。

## 2. 色彩令牌（浅色 / 深色）

浅色：`--background` `hsl(0 0% 100%)`，`--foreground` `hsl(222.2 84% 4.9%)`；深色反转（`--background` `hsl(222.2 84% 4.9%)`，`--foreground` `hsl(210 40% 98%)`）。

| 令牌 | 浅色 | 深色 | 用途 |
| --- | --- | --- | --- |
| `card` | `hsl(0 0% 100%)` | `hsl(222.2 84% 4.9%)` | 卡片表面 |
| `primary` | `hsl(222.2 47.4% 11.2%)`（近黑） | `hsl(210 40% 98%)`（近白） | 主按钮/强调，**不用品牌蓝大色块** |
| `secondary` / `muted` / `accent` | `hsl(210 40% 96.1%)` | `hsl(217.2 32.6% 17.5%)` | 次级表面、分段控件底 |
| `muted-foreground` | `hsl(215.4 16.3% 46.9%)` | `hsl(215 20.2% 65.1%)` | 说明文字 |
| `border` / `input` | `hsl(214.3 31.8% 91.4%)` | `hsl(217.2 32.6% 17.5%)` | 最轻边框 |
| `destructive` | `hsl(0 84.2% 60.2%)` | `hsl(0 62.8% 30.6%)` | 危险操作 |
| `success` | `hsl(142.1 76.2% 36.3%)` | `hsl(142.1 70.6% 45.3%)` | 达标/正常 |
| `warning` | `hsl(37.7 92.1% 50.2%)` | `hsl(32.1 94.6% 43.7%)` | 接近限额 |

**图表色板 `chart-1..5`**（浅色）：`hsl(221 83% 53%)`、`hsl(262 83% 58%)`、`hsl(173 80% 36%)`、`hsl(32 95% 44%)`、`hsl(340 75% 50%)`；深色各提亮约 15%（如 chart-1 → `hsl(217 91% 70%)`）。热力图梯度用 `color-mix(in srgb, <语义色> 85%, var(--card))` 混入卡片色让梯度柔和，避免纯色过硬——Compose 中等效做法是 `card` 与语义色按 85/15 `lerp`。

### Compose 落地映射

- 令牌 → `MaterialTheme.colorScheme`：`background`→background/surface，`card`→surfaceContainer，`primary`→primary，`muted`→surfaceVariant，`border`→outlineVariant，`muted-foreground`→onSurfaceVariant。
- **关闭 dynamic color**（`dynamicColorScheme` 不用），保证跨设备品牌一致。
- 主色为近黑/近白的中性色，品牌蓝 `#1680DB` 仅用于图表与图标点缀；**不引入紫色作为主色**（chart-2 的紫色只出现在图表序列中）。
- 深色模式跟随系统（`isSystemInDarkTheme()`），提交前人工核对深色对比度。

## 3. 形状、阴影、字体

| 项 | 值 | Compose 等效 |
| --- | --- | --- |
| 圆角 | `--radius: 0.5rem`（8dp） | `MaterialTheme.shapes.medium` |
| 阴影 xs | `0 1px 2px rgb(0 0 0 / 4%)` | `shadowElevation` 0–1dp 或仅描边 |
| 阴影 sm | `0 1px 3px 6% + 0 1px 2px 4%` | 1–2dp |
| 阴影 md | `0 4px 12px 8%` | 4dp，仅弹层 |
| 字体 | `system-ui / Segoe UI / Microsoft YaHei` | 默认 FontFamily.SansSerif |
| 数字 | `ui-monospace` + 等宽数字 | `FontFamily.Monospace` 或 `FontFeatureSettings "tnum"`，统计大数字必须用，防跳动 |

深色模式阴影加大黑度（xs 30% / sm 40% / md 50%）保证可见。

## 4. 组件模式

| 知时模式 | 说明 | Trace 对应 |
| --- | --- | --- |
| stat-card 大数字卡 | 标题小字 + 超大等宽数字 + 环比小字，卡片可点击下钻 | 概览页"今日观看 / 本周 / 连续天数" |
| segmented control | 今日 / 近 7 天切换，同屏只保留一组 | 所有统计卡的时间范围切换 |
| 排行列表 | 默认 7 行，"查看更多"展开；行内图标 + 名称 + 单一数值列 | UP主排行、分区排行 |
| 24h 时间线色带 | 每段使用一条色带，悬停看起止与时长，图例可单选 | 观看时间线页 |
| 年度热力图 | 月份轴 + 星期轴 + 图例 + 悬浮 tip | 观看热力图 |
| chip-row | 多选筛选 chips | 内容类型/分区筛选 |
| 空态卡 | 居中说明 + 一个主行动按钮 | 首次启动 / 数据不足 |

## 5. 低噪音清单（评审用）

- [ ] 同一屏没有两个控件做同一件事。
- [ ] 数值列只出现当前排序维度。
- [ ] 默认折叠到 7 行以内，展开是显式动作。
- [ ] 没有装饰性动效；过渡只出现在开关与进度。
- [ ] 说明文字用 `muted-foreground`，不与正文抢注意力。
- [ ] 所有可点击卡片有下钻目标，没有"点了没反应"的卡。
- [ ] 空态、部分数据、未知状态三种文案互不复用。
