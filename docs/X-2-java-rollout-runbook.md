# X-2 Java Rollout Runbook (PYTHON_CANONICAL)

## Current Java posture
- Feature flag is env-driven:
  - `run.engine.mode: ${RUN_ENGINE_MODE:PYTHON_CANONICAL}`
- Default mode is `PYTHON_CANONICAL`.
- Startup log is explicit:
  - `run_engine_mode_active mode=... pythonBaseUrl=... connectTimeoutMs=... readTimeoutMs=...`

## Immediate rollback
- Set env and restart Java service:
  - `RUN_ENGINE_MODE=LEGACY`
- Expected startup evidence:
  - log line with `run_engine_mode_active mode=LEGACY`
- Functional check after rollback:
  - legacy run behavior active,
  - canonical-only preview disable behavior no longer applies.

## Pre-prod checklist (Java-side gate)
- `RUN_ENGINE_MODE` value is explicitly set per environment.
- `PYTHON_DISPATCH_BASE_URL` points to target Python gateway/service.
- Timeouts configured and reviewed:
  - `PYTHON_DISPATCH_CONNECT_TIMEOUT_MS`
  - `PYTHON_DISPATCH_READ_TIMEOUT_MS`
- Actuator health/metrics exposed:
  - `/actuator/health`
  - `/actuator/metrics`
  - `/actuator/prometheus`
- Log ingestion includes structured fields:
  - `requestId`, `correlationId`, `endpoint`, `status`, `latency_ms`.

## 4-step rollout plan
1. Staging smoke test
- Mode: `PYTHON_CANONICAL`.
- Verify startup log mode + Python URL.
- Run API smoke:
  - submit/status/result/cancel
  - one 422 contract case
  - one timeout/unavailable simulation.
- Go if all checks pass and no unexpected 5xx.

2. Canary prod (limited traffic)
- Keep global mode canonical but route only limited tenant/user segment.
- Monitor for 30-60 min.
- Go criteria:
  - proxy 5xx rate < 1%
  - proxy timeout/unavailable < 0.5%
  - p95 canonical endpoint latency within +30% baseline
  - no contract mismatch reported by FE.

3. Progressive ramp-up
- Increase traffic in steps (e.g. 10% -> 25% -> 50% -> 100%).
- Validate same SLO checks at each step for at least 30 min.
- Stop progression on any No-Go condition.

4. Full cutover
- 100% traffic on canonical.
- Keep rollback path armed (`RUN_ENGINE_MODE=LEGACY`) until stability window closes (e.g. 24-48h).

## Go / No-Go and rollback thresholds
- Go:
  - no sustained upstream 5xx burst,
  - contract errors stable and expected (422 passthrough),
  - no material FE regression.
- No-Go (rollback now):
  - proxy 5xx >= 2% over 5 min,
  - timeout/unavailable >= 1% over 5 min,
  - sustained p95 latency regression > 50% over 15 min,
  - repeated endpoint contract breakages observed by FE.

## Verification commands
- Run targeted Java tests:
```powershell
$env:JAVA_HOME='C:\Users\ronan\.jdks\corretto-21.0.4'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
./mvnw -q -Dtest="RunEngineModeEnvOverrideTest,RunControllerDefaultModeTest,PythonCanonicalRunServiceTest,RunControllerRunsPythonCanonicalTest,RunControllerLifecyclePythonCanonicalTest" test
```

- Runtime checks (examples):
```powershell
curl.exe -s http://localhost:8080/actuator/health
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_calls_total
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_latency_ms
```

- Check startup mode log:
```powershell
Get-ChildItem .\logs\LOG_*.log | Sort-Object LastWriteTime -Descending | Select-Object -First 1 | % { Select-String -Path $_.FullName -Pattern "run_engine_mode_active" }
```
