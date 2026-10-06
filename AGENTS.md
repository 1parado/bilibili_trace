# AGENTS.md — Trace Agent Engineering Contract

This file is mandatory for every coding agent and every human contributor. Treat it as an engineering contract, not optional advice.

## 0. Mission and product truth
Build Trace: an Android-first, local-first personal content-consumption analytics app. The first supported target is Bilibili on Android. Product goals are time management, behavior analysis, and learning review.

Never represent a planned capability as implemented. App foreground time, content exposure intervals, estimated viewing time, and learning outcomes are distinct concepts. Trace must not copy Bilibili's history/favorites/following UI as its core value.

## 1. Mandatory startup protocol
Before changing files:
1. Read this file, README, and the relevant PRD/architecture/data-model/collection/privacy/roadmap docs.
2. Inspect the repository tree, current branch, git status, build files, existing conventions, tests, and relevant call sites. Never assume the repository is empty or the architecture is as expected.
3. Identify the user's goal, scope, constraints, risks, and acceptance criteria. If an ambiguity changes architecture, privacy, data semantics, or public behavior, ask before choosing.
4. Write a short implementation plan for non-trivial work. Include files to change, tests, and risks.
5. Check official documentation for version-sensitive Android APIs, Gradle, permissions, and Play policy. Do not invent APIs or copy stale examples.
6. Reuse existing dependencies and patterns. Do not install tools or add dependencies without a concrete need.

## 2. Non-negotiable engineering principles
- Correctness before speed; root-cause fixes before symptom patches.
- Small, reviewable, focused diffs. Avoid unrelated refactors.
- Keep behavior backwards-compatible unless the task explicitly authorizes a change.
- Prefer simple, typed, testable designs over speculative abstractions.
- No one-off helper abstractions without reuse or meaningful domain value.
- Do not silently swallow exceptions, suppress warnings to hide defects, or use TODOs as substitutes for required behavior.
- No fake data in production flows. Demo/sample data must be visibly labeled and isolated from real analytics.
- Do not claim tests/builds/device checks passed unless they were actually run and their results inspected.
- Do not commit generated secrets, credentials, local machine paths, personal data, or raw user behavior samples.
- No network, analytics SDK, crash reporting, or model API calls by default unless explicitly approved and documented.

## 3. Architecture boundaries
- Kotlin + Jetpack Compose for Android UI; Room for local persistence when introduced.
- Keep platform adapters, domain rules, persistence, and UI separated.
- Compose UI must be state-driven; use unidirectional data flow and lifecycle-aware collection.
- Domain calculations should be pure functions where practical and independent of Android framework classes.
- Platform-specific code belongs behind narrow interfaces/adapters.
- MVP is local-first and requires no backend, account, cloud sync, message queue, or remote LLM.
- Use coroutines/Flow for asynchronous work. Use WorkManager only for deferrable work, not as a real-time event guarantee.
- Use dependency injection only when justified by actual graph complexity; do not add a DI framework by default.
- Persist timestamps as UTC epoch milliseconds; render in the user's local timezone. Define interval boundaries and units explicitly.
- Schema changes require Room migrations and migration tests. Never destructive-migrate real user data in production builds.

## 4. Data correctness rules
- Keep observed facts, derived metrics, user corrections, and AI-generated wording distinguishable.
- Every metric must have a definition, time window, timezone, source, sample/coverage note, and test.
- Distinguish at least: app foreground time, detected content exposure, estimated watch time, and user-recorded learning activity.
- Merge overlapping intervals before summing. Handle duplicates, late events, missing boundaries, app restarts, timezone changes, and permission revocation.
- Do not interpret zero, unknown, unavailable, not authorized, and partial data as the same state.
- Every insight must be traceable to source events or a versioned formula. AI may not invent events, statistics, or learning outcomes.
- Recognition confidence is evidence metadata, not a guarantee of truth.

## 5. Privacy, permissions, and platform integrity
- Local-first by default. Collect the minimum data required for a user-enabled feature.
- Never collect or persist passwords, cookies, session tokens, private messages, payment content, keyboard input, or unrelated app screen text.
- AccessibilityService is optional, separately disclosed, explicitly enabled, scoped to the user-selected target app, and immediately stoppable/revocable.
- Never use accessibility to automate likes, follows, comments, coins, purchases, or to bypass access controls.
- Do not store full screen text or screenshots as default evidence. Prefer normalized fields and minimal local evidence references.
- Do not require users to paste platform cookies or credentials. Do not make unofficial/private APIs a critical-path dependency.
- Any future cloud/model processing requires a separate privacy review, clear disclosure, explicit consent, data minimization, and deletion semantics.
- Export and delete features must cover source events, derived records, aggregates, caches, and temporary files.
- Avoid diagnostic, addiction, personality, or moral judgments unsupported by evidence.

## 6. Kotlin, UI, and dependency standards
- Use Kotlin official style, meaningful names, explicit domain types, immutable UI state, and narrow interfaces.
- Avoid giant composables, god classes, duplicated business rules, magic numbers, and broad catch-all exception handlers.
- Put user-visible strings in Android resources; support accessibility semantics and scalable text.
- UI should be calm, legible, light-first, low-noise, and must not introduce purple as the primary brand color.
- Add dependencies only when existing platform/library facilities are insufficient. Pin versions through the version catalog; explain major dependency choices.
- Keep secrets out of source, build files, logs, screenshots, fixtures, and CI output.
- Use structured logging only where useful; never log content titles or behavior trails by default.

## 7. Required test strategy
- Unit tests for domain rules and edge cases; do not rely only on happy-path UI tests.
- Cover duplicate/out-of-order events, overlapping intervals, midnight boundaries, timezone changes, missing start/end, permission loss, empty data, and unknown/partial states as relevant.
- Compose UI tests for important user flows and accessibility semantics when UI behavior changes.
- Data persistence changes require DAO/repository tests; schema changes require migration tests.
- Platform behavior (UsageStats, Accessibility, OEM restrictions, battery) requires explicit manual/device test steps and must not be represented as unit-tested.
- Add regression tests for every fixed bug.
- Run formatting/lint, unit tests, and build tasks that exist in the repo. If a command cannot run, report the exact blocker and do not claim success.

## 8. Git and review workflow
- Work on a focused branch; never force-push or rewrite shared history.
- Never commit directly to main unless the user explicitly requests it and the workflow requires it.
- Keep each commit logically scoped with a conventional commit message.
- For non-trivial work, open a PR with problem, solution, scope, tests, risks, screenshots where useful, and rollout notes.
- Do not merge your own PR unless explicitly authorized.
- Review the final diff for secrets, unrelated edits, generated files, and misleading documentation.

## 9. Definition of Done
A task is not done until all applicable items are true:
- [ ] Acceptance criteria are explicit and satisfied.
- [ ] Implementation matches the architecture and privacy contract.
- [ ] Tests cover new behavior and meaningful edge cases.
- [ ] Formatting/lint/build/tests were run, with actual outcomes recorded.
- [ ] User-visible errors, empty states, loading states, permission-denied states, and partial data are handled.
- [ ] Documentation is updated for public behavior, setup, schema, permissions, or architecture changes.
- [ ] No secrets, unrelated files, silent fallbacks, or unsupported claims were introduced.
- [ ] Final response lists changed files, test commands/results, limitations, and follow-up risks.

## 10. Final response format
Report:
1. What changed and why.
2. Important design decisions and trade-offs.
3. Tests/build/device checks actually run and exact results.
4. Known limitations and unverified platform behavior.
5. Any required manual follow-up.
