# FILE: docs/AI_WORKFLOW.md
# AI Workflow Guide (Spring)

This project uses AI-friendly tickets to ensure clear scope, testability, and predictable delivery.

## Workflow (BMAD)
1. PM stage: clarify goal, scope, non-goals, DoD, and risks.
2. Architect stage: validate technical approach, dependencies, and rollback strategy.
3. Dev stage: implement in small reviewable steps with tests.
4. Reviewer stage: run review gate before merge.
5. Done only if all gates pass and validation commands are green.

## Cross-repo workflow
Use this when a feature spans Spring + other repos (Python/Angular):
1. Create one initiative in coordination repo (`INIT-xxx`).
2. Build one context pack referencing source docs from each repo (with commit SHA).
3. Generate one local ticket per repo (no ticket duplication).
4. Link all local tickets to the same `INIT-xxx`.
5. Track dependencies explicitly (`blocked_by`, `unblocks`) at ticket level.

## Context7 policy (required only when necessary)
Use Context7 only when at least one condition is true:
- New library/framework/API not already mastered by the team.
- Version-specific behavior can change implementation details.
- Uncertain or conflicting documentation in local/project docs.
- Architecture decision depends on external official documentation.

## Spring rules
- Tests mandatory for behavior changes.
- No silent breaking API changes.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless stated otherwise.

## Ticket states
- Active: currently in progress.
- Review: waiting for review gate decision.
- Done: validated, tests passing, DoD met.
- Blocked: waiting on missing dependency or product decision.
