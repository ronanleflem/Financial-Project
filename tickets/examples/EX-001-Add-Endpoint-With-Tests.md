Title
- Add GET /positions/{id} endpoint with tests

Goal
- Expose a read-only endpoint to fetch a single position by id for downstream analytics.

Context / Entry points
- Controller: `PositionController` (add GET `/positions/{id}`)
- Service: `PositionService` (add `getById` method)
- Repository: `PositionRepository` (reuse `findById`)
- DTO: `PositionResponseDto`
- Related endpoints: `GET /positions` (if exists)

Constraints & conventions
- REST: GET `/positions/{id}` -> 200 with body; 404 when not found; 400 on invalid id
- Naming: DTOs end with `Dto`, methods use `getById`
- Timezones: UTC for any datetime fields in response
- Numbers: BigDecimal for monetary fields, consistent scale
- Validation: `@Positive` on path id, return standard error format

Definition of Done
- [ ] GET `/positions/{id}` implemented and returns `PositionResponseDto`
- [ ] DTO mapping defined (entity -> response DTO)
- [ ] Validation for id and error handling in place
- [ ] Unit tests for service and mapping
- [ ] Integration tests for controller (200, 404, 400)
- [ ] Validation commands pass

Implementation plan
1) Add controller route and method signature for GET `/positions/{id}`
2) Add/confirm `PositionResponseDto` fields + validation rules
3) Implement service method `getById` delegating to repository
4) Map entity to DTO in service or mapper
5) Add unit tests for service/mapping
6) Add integration tests for controller

Tests
- Unit tests:
  - Service returns DTO when entity exists
  - Service throws not found exception when missing
  - Mapper converts entity -> DTO correctly
- Integration tests:
  - GET `/positions/{id}` returns 200 + correct JSON
  - GET `/positions/{id}` returns 404 when missing
  - GET `/positions/{id}` returns 400 on invalid id

Validation commands
- [ ] `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- [ ] `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- [ ] `java -version`
- [ ] `./mvnw test`
- [ ] `./mvnw spring-boot:run`

Non-goals / Out of scope
- No changes to persistence schema
- No changes to existing endpoints
- No pagination, filtering, or sorting

Notes / pitfalls
- Transactions: read-only transaction if required by conventions
- Mapping: avoid exposing internal entity fields
- Error handling: use existing exception handler and error format
