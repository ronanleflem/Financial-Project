AI workflow for Spring Boot tickets

Workflow stages
1) Audit the ticket
   - Use `tickets/_templates/AUDIT_PROMPT.md`
   - Clarify missing details before planning
2) Plan the implementation
   - Break into small, testable steps
   - Identify files to touch (Controller, Service, Repository, DTO)
3) Implement step by step
   - Keep changes minimal and aligned with the ticket
   - Avoid refactors unless explicitly requested
4) Validate
   - Run tests and the application
   - Confirm behavior for success and error cases

Spring rules
- Tests are mandatory for new or changed behavior
- No silent breaking changes to existing endpoints
- Follow existing error handling conventions
- Use BigDecimal for monetary values
- Use UTC for datetime fields unless explicitly stated

Ticket lifecycle
- Active: currently being worked on
- Done: all DoD items satisfied and validation commands pass

Branch and PR conventions
- Branch name: `ticket/<ticket-id>-short-description`
- PR title: `[<ticket-id>] <short description>`
- PR body: include summary, tests run, and any risks

Templates usage
- Use `tickets/_templates/TICKET_TEMPLATE.md` for new tickets
- Use `tickets/_templates/AUDIT_PROMPT.md` before planning
- Store examples in `tickets/examples`
