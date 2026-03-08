# INIT-006 - Spring market-analysis seasonality profile `bin` string support

## Title
- Fix `/api/market-analysis/runs/{runId}/result` failure when `seasonality_profiles.bin` contains text labels such as `Asia`

## Ticket type
- Type B: Implementation

## BMAD Stage
- Dev

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-006
- Repo Owner: financial-project-spring
- Upstream Dependencies:
  - Python quant engine now persists `seasonality_profiles.bin` as text for dimensions such as `session`
- Contract Version: local Spring market-analysis contract + Python seasonality persistence

## Goal
- Make Spring market-analysis compatible with seasonality profile bins that are not numeric.
- Remove the `500 Internal Server Error` raised when the quant engine persists bins like `Asia`, `Europe`, or `US`.
- Ensure the underlying Spring/MySQL mapping is fully aligned so text bins are both readable and writable.

## Context / Entry points
- Entity: [SeasonalityProfileEntity.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\entities\quant\SeasonalityProfileEntity.java)
- DTO: [MarketAnalysisSeasonalityProfileRow.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\model\marketanalysis\MarketAnalysisSeasonalityProfileRow.java)
- Repository: [SeasonalityProfileRepository.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\repositories\SeasonalityProfileRepository.java)
- Service: [MarketAnalysisService.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java)
- Existing tests: [MarketAnalysisServiceTest.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\test\java\finance\project\api\services\marketanalysis\MarketAnalysisServiceTest.java)

## BMAD Handover In
- Reproduced on:
  - `GET /api/market-analysis/runs/21724996867946f4a03a2f2520d895c6/result`
- Spring error:
  - `Cannot determine value type from string 'Asia'`
  - `IntegerJdbcType`
  - failure path goes through `MarketAnalysisService.loadSeasonalityProfiles(...)`
- Python side has already been updated to persist text bins for seasonality dimensions such as `by_session`.

## BMAD Handover Out
- Spring entity / DTO / repository aligned to string seasonality bins
- Market-analysis seasonality result endpoint works for both numeric and text bins
- Automated tests cover persisted seasonality rows with text bins

## Context7 Decision
- Required: No
- Reason: local codebase and stacktrace are sufficient

## Constraints & conventions
- Preserve the external `market-analysis` response shape except for widening `bin` from integer to string-compatible handling.
- Maintain compatibility with existing numeric bins such as hour, dow, month, etc.
- Prefer a single Spring-side representation that can carry both numeric and textual bins without lossy conversion.
- Do not reintroduce parsing assumptions that force `bin` through `Integer`.
- Be explicit that JSON `bin` values will now be serialized as strings when returned by Spring.

## Audit Findings
1. Root cause 1: JPA entity maps `bin` as `Integer`
   - In [SeasonalityProfileEntity.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\entities\quant\SeasonalityProfileEntity.java), `bin` is declared as:
     - `private Integer bin;`
   - Hibernate therefore uses `IntegerJdbcType` and calls `ResultSet#getInt(...)`.
   - This fails as soon as MySQL returns a text bin such as `Asia`.

2. Root cause 2: API response model also assumes integer bins
   - In [MarketAnalysisSeasonalityProfileRow.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\model\marketanalysis\MarketAnalysisSeasonalityProfileRow.java), the record field is:
     - `Integer bin`
   - Even if the entity were fixed, the service-to-DTO mapping would still be typed too narrowly.

3. Root cause 3: repository native upsert also constrains `bin` as integer
   - In [SeasonalityProfileRepository.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\repositories\SeasonalityProfileRepository.java), the native `upsert(...)` signature uses:
     - `@Param("bin") Integer bin`
   - This path is not the current failing read path, but it is inconsistent with the new persistence contract and would break or constrain future Spring-side writes/backfills.

4. Root cause 4: database schema type must also accept text bins
   - If MySQL column `seasonality_profiles.bin` is still typed as an integer, changing only Java types will not be sufficient.
   - Spring/JPA and the native upsert must be aligned with the physical schema type, typically `VARCHAR` or equivalent text-compatible type.
   - This must be handled either by an in-repo Flyway migration or by an explicit dependency note if the schema migration is already guaranteed externally.

5. Service impact
   - In [MarketAnalysisService.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:321), `loadSeasonalityProfiles(...)` delegates directly to JPA repository methods.
   - In [MarketAnalysisService.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:394), `toSeasonalityProfileRow(...)` forwards `e.getBin()` into the DTO.
   - Because extraction fails before entity materialization, `/result` returns `500` instead of a seasonality payload.

6. Contract impact
   - Widening `bin` from `Integer` to `String` is externally observable in JSON responses.
   - Existing consumers that deserialize `bin` as a number may require coordination or follow-up changes.
   - Spring should still preserve value fidelity and return the persisted textual label as-is.

7. Test gap
   - Existing `MarketAnalysisServiceTest` covers seasonality result assembly, but not the case where persisted profiles contain non-numeric bins.
   - There is currently no guardrail for text bins like `Asia`.

## Definition of Done
- [ ] `SeasonalityProfileEntity.bin` supports text bins.
- [ ] `MarketAnalysisSeasonalityProfileRow.bin` supports text bins.
- [ ] `SeasonalityProfileRepository.upsert(...)` no longer constrains `bin` to `Integer`.
- [ ] Database schema for `seasonality_profiles.bin` supports text bins, or the external schema dependency is explicitly validated/documented.
- [ ] `/api/market-analysis/runs/{runId}/result` succeeds for persisted seasonality rows with `bin = "Asia"`.
- [ ] Existing numeric bins remain readable.
- [ ] Automated tests cover both text and numeric seasonality bins.

## Implementation plan
1. Widen Spring model types
   - Change `SeasonalityProfileEntity.bin` from `Integer` to `String`.
   - Change `MarketAnalysisSeasonalityProfileRow.bin` from `Integer` to `String`.

2. Align repository contract
   - Change `SeasonalityProfileRepository.upsert(...)` parameter `bin` from `Integer` to `String`.
   - Review any other repository methods, projections, or native queries that may still assume integer bins.

3. Align physical schema
   - Verify the actual MySQL type of `seasonality_profiles.bin`.
   - If needed, add a Flyway migration to convert `bin` from numeric to text-compatible type.
   - Ensure the chosen type preserves existing numeric values while allowing labels such as `Asia`.

4. Keep response compatibility pragmatic
   - Return textual bins as-is.
   - Numeric bins should also serialize as strings if that is the unified type; do not attempt lossy re-parsing at the service boundary.

5. Add tests
   - Extend `MarketAnalysisServiceTest` with a seasonality persisted-table case containing `bin = "Asia"`.
   - Add or extend repository/entity tests if present to cover text-bin persistence/read mapping.
   - Preserve an assertion path for numeric bins to confirm no regression.

## Tests
- Unit tests:
  - update [MarketAnalysisServiceTest.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\test\java\finance\project\api\services\marketanalysis\MarketAnalysisServiceTest.java)
  - add a case where `seasonalityProfileRepository.findBySpecIdAndDatasetIdOrderByCreatedAtAsc(...)` returns a profile row with `bin = "Asia"`
  - assert `response.source() == "persisted_tables"`
  - assert `response.data().seasonalityProfiles().getFirst().bin().equals("Asia")`
- Migration / schema validation:
  - add Flyway coverage or explicit manual validation for `seasonality_profiles.bin` text compatibility
- Optional integration coverage:
  - controller test for `/api/market-analysis/runs/{runId}/result` seasonality response with string bins

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test`

## Reviewer Gate
- [ ] Entity, DTO, and repository types are aligned on text-capable bins.
- [ ] Physical schema for `seasonality_profiles.bin` is text-capable and consistent with Spring mappings.
- [ ] No remaining `IntegerJdbcType` assumptions for seasonality profile bins.
- [ ] Seasonality result endpoint works for `by_session`-style labels.
- [ ] Existing numeric-bin scenarios still pass tests.

## Non-goals / Out of scope
- Changing the Python quant-engine schema again
- Reworking market-analysis endpoint shapes beyond the `bin` field type widening
- Changing unrelated market-stats persistence or DTOs

## Notes / pitfalls
- Do not "fix" this only in the DTO. The entity mapping is the current crash point.
- The repository native `upsert(...)` should be kept consistent even if currently unused in this exact read path.
- Do not forget the database column type: Java-side widening alone does not fix a numeric physical schema.
- If frontend consumers currently assume a numeric `bin`, coordinate separately; Spring should first stop crashing and expose the actual persisted value.
