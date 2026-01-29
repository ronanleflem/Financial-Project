Title
- [Short, action-oriented title, e.g., "Add GET /orders/{id} endpoint"]

Goal
- [One sentence describing user-facing outcome and why it matters]

Context / Entry points
- Controller: [Class name + package or path]
- Service: [Class name + package or path]
- Repository: [Interface name + package or path]
- DTO: [Request/Response DTOs + package or path]
- Related endpoints: [List any existing endpoints impacted]

Constraints & conventions
- REST: [HTTP method, path, status codes, error format]
- Naming: [Endpoint names, DTO naming, method naming]
- Timezones: [Use UTC unless specified; document any conversion]
- Numbers: [Use BigDecimal for monetary values; scale/rounding rules]
- Validation: [Bean Validation annotations + error response format]
- Pagination/sorting (if applicable): [Conventions]

Definition of Done
- [ ] Endpoint added/updated with correct HTTP semantics
- [ ] DTOs added/updated with validation annotations
- [ ] Service logic wired (no business logic changes unless specified)
- [ ] Error handling follows project conventions
- [ ] Unit tests for service/mapping
- [ ] Integration tests for controller (happy + error paths)
- [ ] Documentation updated (if applicable)
- [ ] Validation commands pass

Implementation plan
1) [Step 1: update Controller method signature / routing]
2) [Step 2: DTOs and validation annotations]
3) [Step 3: Service method addition + mapping]
4) [Step 4: Repository call (if needed)]
5) [Step 5: Tests]

Tests
- Unit tests:
  - [Service behavior]
  - [DTO/mapper behavior]
- Integration tests:
  - [Controller endpoint, success case]
  - [Controller endpoint, validation error]
  - [Controller endpoint, not found / error mapping]

Validation commands
- [ ] `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- [ ] `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- [ ] `java -version`
- [ ] `./mvnw test`
- [ ] `./mvnw spring-boot:run`

Non-goals / Out of scope
- [Explicitly list what is not being changed]

Notes / pitfalls
- Transactions: [If needed, clarify boundaries]
- Mapping: [DTO/entity mapping, avoid leaking entities]
- Error handling: [Exception mapping strategy]
- Performance: [Any known risks]
