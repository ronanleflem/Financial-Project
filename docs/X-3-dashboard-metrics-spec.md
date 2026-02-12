# X-3 Dashboard & Metrics Spec (Java)

## Scope
- Java canonical proxy metrics for `/api/runs*`.
- Python metrics are out of scope in this repo (to be aligned in Python service).

## Metrics exposed
- `runs_canonical_proxy_latency_ms{endpoint}`
  - histogram + percentiles (p95/p99).
- `runs_canonical_proxy_calls_total{endpoint,statusFamily}`
  - status families: `2xx`, `4xx`, `5xx`, `other`.
- `runs_canonical_proxy_timeouts_total{endpoint}`
  - timeout counter only.
- `runs_canonical_proxy_outcomes_total{endpoint,outcome}`
  - `outcome`: `success` (2xx), `error` (non-2xx).

## Dashboard panels
- Canonical traffic by status family (RPS).
- Canonical timeout rate by endpoint.
- Latency p95 by endpoint.
- Latency p99 by endpoint.
- Submit success rate (`POST /runs`).
- Cancel success rate (`POST /runs/{id}/cancel`).
- Result success rate (`GET /runs/{id}/result`).

## Files
- Prometheus config: `observability/prometheus.yml`
- Prometheus alert rules: `observability/alerts-runs.yml`
- Grafana dashboard: `observability/grafana/dashboards/runs-dashboard.json`
