# Contributing to Trace

All contributors and AI agents must follow [AGENTS.md](AGENTS.md). These requirements apply to implementation, tests, documentation, and refactoring.

## Before implementation
- Read the relevant product and engineering docs.
- Inspect the current branch, working tree, and call sites.
- State acceptance criteria and a focused plan for non-trivial changes.
- Check official documentation for version-sensitive APIs and policies.
- Do not assume a feature is implemented because it appears in the PRD.

## Branches and commits
- Use a focused branch such as `feat/usage-session-model`, `fix/session-boundary`, or `docs/privacy-disclosure`.
- Use conventional commit prefixes: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `build:`, `ci:`.
- Never force-push, commit secrets, or make unrelated changes.
- Prefer pull requests for reviewable changes. Do not self-merge unless explicitly authorized.

## Required checks
For each change, run the relevant tests and checks. At minimum for app code, run unit tests, lint, and debug build where the environment permits. Add regression tests for bug fixes and edge-case tests for domain logic. Record commands and outcomes in the PR.

## Privacy review
Any change that collects, persists, exports, uploads, or deletes personal behavior data must explain:
- What data is collected and why.
- Whether processing is local or remote.
- How permission is granted and revoked.
- Retention, export, and deletion behavior.
- Risks and fallback behavior.

## Pull request checklist
- [ ] Scope is focused and acceptance criteria are met.
- [ ] Tests cover new behavior and relevant edge cases.
- [ ] Build/lint/test outcomes are truthful and recorded.
- [ ] Documentation and user-facing strings are updated.
- [ ] No secrets, personal data, or unrelated generated files are included.
- [ ] Accessibility, empty, loading, error, and partial-data states are considered.
- [ ] Limitations and unverified device behavior are stated.
