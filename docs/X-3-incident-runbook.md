# X-3 Incident Runbook (Java Canonical Proxy)

## Symptoms
- Sudden increase of `5xx` on `/api/runs*`.
- High p95/p99 latency on proxy endpoints.
- Timeout spike to Python upstream.
- Drop in submit success rate.

## Quick checks (5 minutes)
1. Verify mode and target upstream:
   - find startup log `run_engine_mode_active`.
2. Check service health:
   - `/actuator/health`.
3. Check proxy metrics:
   - `runs_canonical_proxy_calls_total`
   - `runs_canonical_proxy_latency_ms`
   - `runs_canonical_proxy_timeouts_total`
   - `runs_canonical_proxy_outcomes_total`
4. Check recent structured logs:
   - fields `correlationId`, `requestId`, `endpoint`, `status`, `latency_ms`.

## Diagnostics commands
```powershell
curl.exe -s http://localhost:8080/actuator/health
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_calls_total
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_latency_ms
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_timeouts_total
curl.exe -s http://localhost:8080/actuator/metrics/runs_canonical_proxy_outcomes_total

Get-ChildItem .\logs\LOG_*.log |
  Sort-Object LastWriteTime -Descending |
  Select-Object -First 1 |
  % { Select-String -Path $_.FullName -Pattern "run_proxy|run_engine_mode_active|http_request" }
```

## Mitigation actions
1. Validate Python upstream reachability and latency from Java host.
2. Increase temporary timeout values if upstream is degraded but responsive:
   - `PYTHON_DISPATCH_CONNECT_TIMEOUT_MS`
   - `PYTHON_DISPATCH_READ_TIMEOUT_MS`
3. Reduce traffic (canary-only route) if errors exceed threshold.

## Rollback procedure (immediate)
1. Set `RUN_ENGINE_MODE=LEGACY`.
2. Restart Java service.
3. Confirm startup log contains:
   - `run_engine_mode_active mode=LEGACY`.
4. Run smoke test on `POST /api/runs`.

## Escalation
- Escalate to Python/on-call when:
  - timeout ratio or 5xx remains above threshold > 10 minutes,
  - repeated correlation IDs show upstream contract failures.
