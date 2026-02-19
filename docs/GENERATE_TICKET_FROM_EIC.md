# FILE: docs/GENERATE_TICKET_FROM_EIC.md
# GENERATE_TICKET_FROM_EIC.md (Spring + BMAD)

Use `tickets/_templates/TICKET_TEMPLATE.md` and `tickets/_templates/AUDIT_PROMPT.md`.

## Objectives
- Translate EIC into one Spring ticket.
- Keep scope Spring-only.
- Define tests and validation commands.

## Mandatory BMAD alignment
Set `BMAD Stage` to `PM` and include handover fields.

## Cross-repo rule
- Set `Cross-Repo Initiative` (`INIT-xxx`).
- Add explicit `Upstream Dependencies` and `Contract Version`.

## Context7 rule
Set `Context7 Decision` to Yes only if external docs are needed.

## Output format
FILE: tickets/active/<FILENAME>.md
<full ticket content>

Ticket must be in French and include all template sections.
