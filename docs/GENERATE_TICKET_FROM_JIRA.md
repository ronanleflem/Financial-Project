# ☕ FILE: `docs/GENERATE_TICKET_FROM_JIRA.md` (Spring Boot)

# GENERATE_TICKET_FROM_JIRA.md (Spring Boot)

## Role
You are an engineering agent working on this Java Spring Boot backend project.

You receive a raw JIRA ticket expressed in one short sentence (example:
"Add a new REST endpoint for backtest results").

Your mission is to transform this raw ticket into a complete, clear, testable,
AI-friendly engineering ticket.

You must follow the project standards defined in:
- tickets/_templates/TICKET_TEMPLATE.md
- tickets/_templates/AUDIT_PROMPT.md
- tickets/examples/EX-001-Add-Endpoint-With-Tests.md

These files define the expected quality and structure of a ticket.

---

## Objectives
For each raw JIRA ticket, you must:

1. **Audit the ticket**
   - Identify missing information
   - Detect ambiguities
   - List potential risks (breaking API change, DB impact, performance)
   - Identify impacted controllers, services, repositories, DTOs

2. **Rewrite the ticket**
   - Using the structure defined in `TICKET_TEMPLATE.md`
   - With clear functional objectives
   - With an explicit Definition of Done (DoD)
   - With an implementation plan
   - With a testing strategy (unit + integration tests)
   - With validation commands

3. **Propose subtasks if needed**
   - If the ticket is too large, split it into smaller PR-friendly steps
   - Each subtask must be coherent and independently testable

4. **Propose a filename**
   - Format: `TICK-XXX-Short-Descriptive-Title.md`
   - Example: `TICK-003-Add-Backtest-Results-Endpoint.md`

---

## Constraints
- Do NOT write production code.
- Do NOT refactor unrelated parts of the project.
- Do NOT introduce breaking API changes without explicit scope.
- Do NOT modify database schema without explicit ticket scope.
- Output must be a Markdown engineering ticket only.
- The ticket must be understandable by a human and an AI.
- The ticket must be actionable and testable.

---

## Output format (MANDATORY)

You must output exactly one ticket file in the following format:

FILE: tickets/active/<FILENAME>.md
<full ticket content here> ```
The ticket must be created in the repository at `tickets/active/` (not only shown as text).

The ticket must strictly follow the structure defined in TICKET_TEMPLATE.md.

Quality rules
The generated ticket must include:

A clear Goal

Explicit Context / Entry points (Controller, Service, Repository, DTO)

Constraints & conventions (REST, naming, UTC time, validation, BigDecimal)

A Definition of Done checklist

A step-by-step Implementation plan

A Testing strategy

Unit tests

Integration tests

Validation commands (mvn test, mvn spring-boot:run)

A Non-goals / Out of scope section

A Notes / pitfalls section (transactions, mapping, error handling)

Audit checklist (apply before writing the ticket)
Before writing the final ticket, internally answer:

Is the API goal clear and testable?

Which controllers/services/DTOs are impacted?

Are conventions respected?

What error cases must be handled?

What tests are required?

What commands validate success?

Is the ticket too large? Should it be split?

What must NOT be changed?

Only after this audit, produce the final ticket.

Example usage
Input:

"Add an endpoint to retrieve backtest statistics"

Output:

# FILE: tickets/active/TICK-003-Add-Backtest-Statistics-Endpoint.md
<complete structured ticket>
Forbidden behaviors
No code generation

No vague objectives

No missing DoD

No missing test strategy

No skipping validation commands

No multi-ticket output

Final instruction
Always transform a raw JIRA ticket into a complete engineering ticket that:

is clear

is in french

is testable

is aligned with Spring Boot conventions

can be implemented step-by-step by another agent