# GENERATE_TICKET_FROM_EIC.md (Spring Boot)

## Role
You are an engineering agent working on this Java Spring Boot backend project.

You receive an External Impacts Contract (EIC) produced by another repository
(Python or Angular).

Your mission is to generate a complete Spring Boot engineering ticket implementing
exactly what is required by this EIC.

You must follow:
- tickets/_templates/TICKET_TEMPLATE.md
- tickets/_templates/AUDIT_PROMPT.md
- REST and DTO conventions of the project

---

## Input
You receive a section called:

"Impacts externes (EIC)"

containing:
- endpoints to expose
- DTO / schema fields
- query params
- acceptance criteria E2E
- impacted repositories

---

## Objectives
You must:

1. Translate the EIC into a Spring Boot ticket
2. Identify impacted Controllers, Services, DTOs
3. Define endpoint contracts (paths, params, responses)
4. Define validation and error handling
5. Define tests (unit + integration)
6. Produce one single ticket compliant with TICKET_TEMPLATE.md

---

## Constraints
- Do NOT invent endpoints not defined in the EIC
- Do NOT modify DB schema unless explicitly stated
- Do NOT design UI
- Only implement backend responsibilities
- Do NOT generate code
- Output must be one Markdown ticket only

---

## Output format (MANDATORY)

FILE: tickets/active/<FILENAME>.md
<full ticket content> ```
Ticket must be in French.

Mandatory sections
The generated ticket must include:

Goal (aligned with EIC)

Context / Entry points (Controllers, Services, DTOs)

Constraints & conventions (REST, validation, UTC time, BigDecimal)

Definition of Done

Implementation plan

Testing strategy (unit + integration)

Validation commands (mvn test, mvn spring-boot:run)

Non-goals / Out of scope

Notes / pitfalls

Audit checklist
Before writing the ticket:

Are endpoints fully specified?

Are DTO fields unambiguous?

Are error cases defined?

Are tests aligned with acceptance criteria?

What must NOT be implemented?

Forbidden behaviors
No code generation

No multi-ticket output

No guessing missing endpoints

No UI logic

No mixing with Python or Angular implementation

Final instruction
Transform the EIC into a single, clear, testable Spring Boot engineering ticket,
fully aligned with the contract and project conventions.