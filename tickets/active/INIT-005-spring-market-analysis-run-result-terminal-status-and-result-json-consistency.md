# INIT-005 - Spring market-analysis run result terminal status and result_json consistency

## Title
- Fix `/api/market-analysis/runs/{runId}/result` false `409 RESULT_NOT_READY` on succeeded runs

## Ticket type
- Type B: Implementation

## BMAD Stage
- Dev

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-005
- Repo Owner: financial-project-spring
- Upstream Dependencies: None
- Contract Version: local Spring market-analysis contract

## Goal
- Make `GET /api/market-analysis/runs/{runId}/result` return a coherent response for terminal successful runs and remove the contradiction where run detail says `result_json_available=true` while the result endpoint returns `409 RESULT_NOT_READY`.

## Context / Entry points
- Controller: [MarketAnalysisController.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\controllers\MarketAnalysisController.java)
- Service: [MarketAnalysisService.java](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java)
- DTO: `MarketAnalysisRunDetailResponse`, `MarketAnalysisRunResultResponse`, `MarketAnalysisResultMeta`
- Related endpoints:
  - `GET /api/market-analysis/runs`
  - `GET /api/market-analysis/runs/{runId}`
  - `GET /api/market-analysis/runs/{runId}/result`

## BMAD Handover In
- Bug reproduced from Angular `/market-analysis` on a run displayed as `SUCCEEDED`
- Observed UI contradiction:
  - detail view shows `Result JSON Disponible`
  - result endpoint is mapped to `409 RESULT_NOT_READY`

## BMAD Handover Out
- Spring fix committed locally
- Automated tests covering terminal status handling and result-json consistency
- Short validation note for the Angular consumer

## Context7 Decision
- Required: No
- Reason: local codebase + existing Spring tests are sufficient

## Constraints & conventions
- Preserve existing REST and error response conventions.
- Do not change the external endpoint shape unless strictly necessary.
- Keep status handling case-insensitive.
- Prefer a single source of truth for "terminal" and "result available" semantics.

## Audit Findings
1. Root cause 1: terminal status set is incomplete
   - In [MarketAnalysisService.java:32](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:32), `TERMINAL` does not include `succeeded`.
   - In [MarketAnalysisService.java:127](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:127), the job status is normalized to lowercase, so `SUCCEEDED` becomes `succeeded` and is treated as non-terminal.
   - In [MarketAnalysisService.java:129](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:129), this causes `409 RESULT_NOT_READY` whenever parsed `resultJson` is null.

2. Root cause 2: detail/result endpoint use different definitions of "result available"
   - In [MarketAnalysisService.java:114](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:114), `resultJsonAvailable` is based on raw string non-blank.
   - In [MarketAnalysisService.java:126](C:\Users\ronan\Desktop\Projet Finance\spring\Financial-Project\src\main\java\finance\project\api\services\marketanalysis\MarketAnalysisService.java:126), the result endpoint relies on `parseJson(job.getResultJson())`.
   - If `result_json` exists but is malformed or not valid JSON, the detail endpoint says "available" while the result endpoint behaves as if it is absent.

3. Test gap
   - Existing coverage is controller-focused. There is no service-level guardrail proving behavior for:
     - `SUCCEEDED` runs
     - malformed `result_json`
     - consistency between detail and result endpoints

## Definition of Done
- [ ] `SUCCEEDED` and equivalent successful terminal statuses are treated as terminal in `getRunResult`.
- [ ] `resultJsonAvailable` and `getRunResult` use coherent semantics.
- [ ] `GET /runs/{runId}` and `GET /runs/{runId}/result` no longer contradict each other for the same run.
- [ ] Unit tests cover successful terminal statuses, non-terminal statuses, malformed result JSON, and persisted-table fallback behavior.
- [ ] Validation commands pass.

## Implementation plan
1. Introduce a dedicated status-normalization helper in `MarketAnalysisService` and expand terminal success statuses (`succeeded`, optionally `success`) in a case-insensitive way.
2. Align result availability semantics:
   - either compute `resultJsonAvailable` from parsed JSON availability,
   - or adjust `getRunResult` to distinguish "raw result string exists but is malformed" from "result not ready".
3. Add service-level tests for:
   - `SUCCEEDED` + no persisted rows + valid result JSON => returns `200 result_json`
   - `SUCCEEDED` + persisted rows => returns `200 persisted_tables`
   - `RUNNING` + no result JSON => returns `409`
   - malformed `result_json` => deterministic behavior and no detail/result contradiction

## Tests
- Unit tests:
  - add `MarketAnalysisServiceTest` if missing
  - cover terminal status normalization and result-json consistency
- Integration tests:
  - update controller tests only if endpoint contract changes

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test`

## Reviewer Gate
- [ ] Scope limited to Spring `market-analysis` result semantics.
- [ ] No regression on `404`, `409`, and `422` handlers.
- [ ] Tests prove the reproduced `SUCCEEDED` case is fixed.
- [ ] Detail/result endpoint contract is internally coherent.

## Non-goals / Out of scope
- Changing Angular behavior
- Reworking market-analysis pagination/filtering
- Extending symbol extraction for `data.symbols[]` in run catalog filtering

## Notes / pitfalls
- If you keep `resultJsonAvailable` as "raw non-blank string exists", then `getRunResult` must not emit `RESULT_NOT_READY` for malformed-but-present payloads; use a different failure mode or fallback.
- If you redefine `resultJsonAvailable` as "parsable result JSON exists", update any consumer assumptions accordingly.
- Prefer explicit tests for uppercase persisted job statuses coming from `api_jobs.status`.
