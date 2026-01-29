Audit checklist for Spring Boot tickets

Scope & clarity
- [ ] Objective is clear and user-facing outcome is stated
- [ ] Ticket is testable and has measurable success criteria
- [ ] Ticket is scoped to a single change set (no broad refactor)

API impact
- [ ] Endpoints impacted are listed (method + path)
- [ ] Request/response DTOs and schemas are specified
- [ ] Status codes and error format are defined

Project conventions
- [ ] Naming conventions align with existing codebase
- [ ] Timezone handling is specified (UTC unless stated)
- [ ] Monetary values use BigDecimal with scale/rounding rules
- [ ] Validation annotations and error response format included

Error handling
- [ ] Exception mapping strategy is defined
- [ ] Not found / invalid input cases are covered

Testing strategy
- [ ] Unit tests specified (service, mapping)
- [ ] Integration tests specified (controller)
- [ ] Data setup/fixtures described if needed

Validation commands
- [ ] `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- [ ] `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- [ ] `java -version`
- [ ] `./mvnw test`
- [ ] `./mvnw spring-boot:run`

Risks
- [ ] Breaking API changes identified
- [ ] Performance or DB impact noted
- [ ] Backward compatibility considered

Task decomposition
- [ ] Ticket can be split into sub-tasks
- [ ] Implementation plan steps are clear

Agent constraints (must NOT do)
- [ ] No schema/DB migration changes without a dedicated ticket
- [ ] No broad refactors or renaming across the codebase
- [ ] No dependency upgrades unless explicitly required
- [ ] No silent breaking changes to existing APIs
