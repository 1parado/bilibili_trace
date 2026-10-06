# Coding Agent Task Template

Copy this template into each substantial Agent task. The repository's `AGENTS.md` remains authoritative.

## Task
[Describe one outcome, not a vague goal.]

## User-visible acceptance criteria
- [ ] [Observable behavior]
- [ ] [Error/empty/permission state]
- [ ] [Privacy/data semantics]

## Scope boundaries
- In scope:
- Out of scope:
- Must preserve:

## Required agent procedure
1. Read `AGENTS.md` and the relevant product/architecture/privacy docs.
2. Inspect repository state and existing implementation before editing.
3. Check official documentation for version-sensitive platform behavior.
4. Propose a focused plan and list the tests before implementation.
5. Implement the smallest coherent vertical slice; do not fabricate data or functionality.
6. Add tests for normal cases, edge cases, and regression risks.
7. Run relevant unit tests, lint, and build; inspect actual results.
8. Review the diff for privacy, architecture, secrets, unrelated edits, and inaccurate docs.
9. Update documentation and status only for work that is actually complete.
10. Report changed files, design trade-offs, exact commands/results, limitations, and unverified device behavior.

## Constraints
- Do not commit directly to `main`.
- Do not self-merge a pull request.
- Do not add backend, analytics SDK, remote AI, or new dependencies without a concrete approved need.
- Do not claim a device/API behavior works without real-device evidence.
- If an acceptance criterion cannot be met, explain why and offer the smallest safe alternative.
