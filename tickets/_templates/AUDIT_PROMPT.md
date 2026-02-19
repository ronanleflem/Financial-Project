# FILE: tickets/_templates/AUDIT_PROMPT.md
# Ticket Audit Prompt (Spring + BMAD)

## PM gate
- [ ] Goal is clear and testable.
- [ ] Scope and non-goals are explicit.
- [ ] DoD is measurable.

## Architect gate
- [ ] Impacted controllers/services/repos/DTOs identified.
- [ ] API contract and error mapping are coherent.
- [ ] Risks and rollback strategy documented.

## Dev gate
- [ ] Steps are small and reviewable.
- [ ] Tests and validation commands are explicit.
- [ ] Compatibility constraints are explicit.

## Reviewer gate
- [ ] Review criteria are explicit and blocking.
- [ ] Regression risks are identified.
- [ ] Acceptance can be decided from evidence.

## Cross-repo gate (if applicable)
- [ ] `Cross-Repo Initiative` is set (`INIT-xxx`).
- [ ] External dependencies are explicit.
- [ ] Contract/version reference is explicit and testable.
- [ ] Scope remains local to Spring repo.

## Context7 check (required only if needed)
- [ ] New or uncertain external API/library/framework involved.
- [ ] Version-specific behavior may affect implementation.
- [ ] If no, Context7 is intentionally skipped.
