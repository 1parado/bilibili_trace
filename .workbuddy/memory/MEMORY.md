# bilibili_trace 项目备注

## Git 工作流约定
- 小 fix/chore 类改动：直接提交并推送到 main，**不用开 PR**（用户 2026-10-10 明确要求）。非琐碎功能仍走分支 + PR（AGENTS.md 规范）。

## 仓库事实
- README 顶部 logo 用 `docs/assets/bilibili-trace-logo.svg`，用户嫌 420px 太大，已定为 `width="150"`。
- 启动图标为纯矢量自适应图标（蓝底 #1680DB + 安全区内白色前景 + monochrome），README「项目图标」小节有说明。
- CI（.github/workflows/android-ci.yml）跑 `testDebugUnitTest lintDebug assembleDebug`，lint 零 error 是硬门槛；曾因 `windowLightNavigationBar`（API 27）在 minSdk 26 下报 NewApi 挂掉，已用 values-v27 修复。
