# FILE: docs/GENERATE_TICKET_FROM_EIC.md
# GENERATE_TICKET_FROM_EIC.md (Spring local translator)

Purpose: convert an external contract (EIC) into one Spring local ticket.

This file does NOT orchestrate multiple repositories.
Cross-repo orchestration is handled only by the coordination repository.

Use:
- `tickets/_templates/TICKET_TEMPLATE.md`
- `tickets/_templates/AUDIT_PROMPT.md`

Requirements:
- Output one Spring ticket in `tickets/active/`.
- Keep scope Spring-only.
- Include: BMAD Stage, Cross-Repo Initiative, Upstream Dependencies, Contract Version, Context7 Decision.
- Do not generate code.
- Ticket language: French.
