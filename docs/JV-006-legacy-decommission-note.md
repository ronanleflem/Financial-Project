# JV-006 Migration Note (Run Legacy Decommission)

## Scope
- `POST /api/runs`, `GET /api/runs/{id}`, `GET /api/runs/{id}/result`, `POST /api/runs/{id}/cancel` remain compatible.
- Java remains thin orchestrator for canonical mode (`run.engine.mode=PYTHON_CANONICAL`).

## Preview Endpoint Decision
- Legacy preview is kept temporarily but explicitly deprecated.
- Supported only in `LEGACY` mode:
  - `POST /api/specs/preview`
  - `POST /api/runs/specs/preview` (alias for transition)
- Responses include:
  - `Deprecation: true`
  - `Sunset: Tue, 30 Jun 2026 23:59:59 GMT`
- In `PYTHON_CANONICAL` mode, preview returns `410 Gone`:
  - code: `LEGACY_PREVIEW_DISABLED`
  - message: submit `/api/runs` directly to Python canonical API.

## FE Impact
- If frontend still calls preview in canonical mode, calls now fail with `410`.
- Frontend should stop relying on Java preview and submit canonical payload directly.

## Ops Impact
- Keep kill-switch available with `RUN_ENGINE_MODE=LEGACY` for temporary rollback.
- Legacy spec-builder beans are now loaded only in `LEGACY` mode.
- Legacy components are marked `@Deprecated` to prepare full removal.
