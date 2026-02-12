# X-3 Cleanup Status (Java/FE inventory)

## Done (Java)
- Canonical proxy flow is default (`PYTHON_CANONICAL`).
- Legacy spec pipeline limited to `LEGACY` mode via conditional beans.
- Legacy preview endpoint isolated and deprecated:
  - `LegacyRunSpecPreviewController` (`forRemoval=true`, target `2026-06-30`).
- Legacy validators/builders/services remain marked deprecated with removal target.

## Remaining legacy components (Java)
- `RunRequestValidator`
- `PythonSpecService`
- `SpecBuilderFactory`, `PythonSpecBuilder`, `DefaultSpecBuilderFactory`
- Legacy builders (`BacktestSpecBuilder`, `MarketStatsSpecBuilder`, `SeasonalitySpecBuilder`, `StrategyBacktestSpecBuilder`, `StressTestsSpecBuilder`)
- Legacy services (`RunRequestService`, `RunStatusService`, `RunResultService`)

## FE impact / todo
- Remove any FE call path using preview endpoints:
  - `/api/specs/preview`
  - `/api/runs/specs/preview`
- Ensure FE relies only on canonical submit/status/result/cancel contracts.

## Safe-to-remove now
- Nothing removed in X-3 to avoid accidental behavior break.
- Cleanup remains controlled by deprecation and explicit target date.

## Target architecture update
- Java responsibility:
  - proxy orchestration,
  - security/auth,
  - technical validation only,
  - audit + observability.
- Python responsibility:
  - business validation,
  - run lifecycle and execution semantics.
