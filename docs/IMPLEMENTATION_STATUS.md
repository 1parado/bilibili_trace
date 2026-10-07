# Implementation Status

This file tracks verified repository state. Update it only after inspecting code and actual build/test results.

## Repository bootstrap
- [x] Product and engineering documentation
- [x] Mandatory Agent engineering contract
- [x] Kotlin / Compose app module skeleton
- [x] Version catalog and Gradle build configuration
- [x] GitHub Actions build/lint/unit-test workflow completing successfully
- [x] Adaptive vector launcher icon (safe-zone + monochrome)
- [ ] Gradle Wrapper committed and verified
- [ ] Local Android Studio sync and debug install verified on a real device/emulator

## Product functionality
- [x] Pure domain time engine: interval merge/union, daily bucketing (midnight/timezone aware), coalescing — unit tested
- [x] Room persistence v1 (app_sessions / content_items / behavior_events) with schema export; DAO/migration tests pending on-device
- [x] Session repository with deterministic dedupe keys and idempotent manual submissions — unit tested (fake DAOs)
- [x] Manual session recording UI with validation, localized errors, and anonymous BV metadata fetch (no cookies)
- [x] Today dashboard: today/7-day stats (union-based), goal progress, recent sessions, delete, snackbar on save failure
- [x] Timeline: 24h band + day navigation (29 days) + per-day list + five-week heat grid
- [x] Settings: daily goal (DataStore), CSV/JSON export with schema versioning, confirmed delete-all
- [x] Share-intent capture for Bilibili URLs (ACTION_SEND → BV 解析 → 元数据预填)
- [x] Per-content ranking aggregates
- [x] UsageStats collection adapter: optional usage-access grant, package-level foreground
      sessions for Bilibili app family, synced on app open + manual sync + WorkManager 30-min
      backfill; imports are idempotent via package+interval dedupe keys — planner/syncer/repository
      unit tested (fake source & DAOs); on-device behavior still requires manual verification
- [x] Accessibility recognition (title-level): scoped service reads only the video page title
      of Bilibili packages, extraction is a pure unit-tested heuristic (view-id preference,
      longest-plausible fallback, label/time/url exclusion); same title shares one content
      identity with ACCESSIBILITY provenance and userVerified=false; no automation, no screen
      text storage; on-device extraction accuracy on real app versions still needs verification
- [x] Manual recording entry removed by product decision; share-intent quick save retained
- [ ] Weekly report based on traceable metrics

## Current quality status
Spiral 3 complete: share-intent import, content ranking, suggested end times, delete confirmations, accessibility semantics for the timeline band. Automatic usage recording (UsageStatsManager) is implemented behind pure domain interfaces: package-level foreground time only — no screen text, no content titles; sessions surface as「哔哩哔哩 · 自动记录」and are semantically distinct from manual/share entries. Still-open sessions at sync time are intentionally skipped (captured on the next sync) to avoid artificial interval boundaries. CI passes build + lint + unit tests on every push (90+ unit tests). Known remaining gaps: instrumentation tests (DAO, migration, Compose UI) require a device/emulator run and are not yet authored; Room schema JSON files are generated in CI but not committed; usage-access behavior on OEM ROMs (battery/background limits) needs manual device verification; weekly reports are next on the roadmap.
