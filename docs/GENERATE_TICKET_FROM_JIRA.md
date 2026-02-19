# FILE: docs/GENERATE_TICKET_FROM_JIRA.md
# GENERATE_TICKET_FROM_JIRA.md (Spring + BMAD)

Use `tickets/_templates/TICKET_TEMPLATE.md` and `tickets/_templates/AUDIT_PROMPT.md`.

## Mandatory classification
- Type A: Audit/Discovery only
- Type B: Implementation

## Mandatory BMAD flow
1. PM framing
2. Architect validation
3. Dev implementation
4. Reviewer gate

Set `BMAD Stage` to `PM` in generated ticket.

## Cross-repo rule
If ticket impacts multiple repositories:
- Set `Cross-Repo Initiative` (`INIT-xxx`).
- Keep this ticket Spring-only.
- Set `Upstream Dependencies` and `Contract Version`.

## Context7 rule
Set `Context7 Decision` to Yes only if external docs are needed.

## Output format
FILE: tickets/active/<FILENAME>.md
<full ticket content>

Ticket must be in French and include all template sections.
