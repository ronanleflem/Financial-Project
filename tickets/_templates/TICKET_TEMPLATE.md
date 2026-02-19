# FILE: tickets/_templates/TICKET_TEMPLATE.md
# Ticket Template (Spring)

## Title
- [Short, actionable title]

## Ticket type
- [Type A: Audit/Discovery | Type B: Implementation]

## BMAD Stage
- [PM | Architect | Dev | Reviewer]

## Cross-Repo Coordination
- Cross-Repo Initiative: [INIT-xxx or N/A]
- Repo Owner: [financial-project-spring]
- Upstream Dependencies: [ticket/PR ids or None]
- Contract Version: [version/tag/commit or N/A]

## Goal
- [Observable and testable outcome]

## Context / Entry points
- Controller:
- Service:
- Repository:
- DTO:
- Related endpoints:

## BMAD Handover In
- [Required artifacts from previous stage]

## BMAD Handover Out
- [Artifacts produced for next stage]

## Context7 Decision
- Required: [Yes/No]
- Reason: [One short justification]

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.

## Definition of Done
- [ ] Endpoint behavior implemented per goal.
- [ ] DTO validation and mapping covered.
- [ ] Unit and integration tests added or updated.
- [ ] Validation commands pass.

## Implementation plan
1. [Step 1]
2. [Step 2]
3. [Step 3]

## Tests
- Unit tests:
- Integration tests:

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test`

## Reviewer Gate
- [ ] Scope matches ticket and DoD.
- [ ] Architecture constraints respected.
- [ ] Tests are meaningful and pass.
- [ ] No regression risk left unaddressed.

## Non-goals / Out of scope
- [Explicit list]

## Notes / pitfalls
- [Error mapping, transaction boundaries, compatibility]
