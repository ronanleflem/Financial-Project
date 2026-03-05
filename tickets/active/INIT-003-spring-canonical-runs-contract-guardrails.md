# INIT-003 - Spring contract guardrails for canonical runs

## Title
- Verrouiller le contrat transport canonical `/api/runs*`

## Ticket type
- Type B: Implementation

## BMAD Stage
- PM

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-003
- Repo Owner: financial-project-spring
- Upstream Dependencies: None
- Contract Version: STRAT-CALC-DECOMMISSION-V1-2026-03-05

## Goal
- Stabiliser le contrat public Spring sur `/api/runs*` pour garantir un pass-through coherent des statuts/body Python et eviter toute derive pendant la decommission ta4j.

## Context / Entry points
- Controller:
  - `RunController`
- Service:
  - `PythonCanonicalRunService`
  - `CanonicalRunAuditService`
- Repository:
  - N/A
- DTO:
  - DTO de request/response runs canonical
- Related endpoints:
  - `POST /api/runs`
  - `GET /api/runs/{requestId}`
  - `GET /api/runs/{requestId}/result`
  - `POST /api/runs/{requestId}/cancel`

## BMAD Handover In
- Consolidation Architect INIT-003 validee.
- Contrat versionne `STRAT-CALC-DECOMMISSION-V1-2026-03-05`.

## BMAD Handover Out
- Tests contractuels Spring verrouillant propagation status/body.
- Regles d'erreur techniques Spring (`400`) vs metier Python (`422`/`FAILED`) documentees.
- Ticket pret pour cutover legacy.

## Context7 Decision
- Required: No
- Reason: implementation basee sur conventions repo et contrat interne deja defini.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.

## Definition of Done
- [ ] Tests integration garantissent pass-through `2xx/422` sur flux canonical.
- [ ] Aucun remapping metier Spring sur erreurs Python non support.
- [ ] Validation `400` technique Spring conservee.
- [ ] Validation commands pass.

## Implementation plan
1. Ajouter/mettre a jour tests transport sur endpoints `/api/runs*`.
2. Verifier et corriger les mappings status/body pour respecter pass-through.
3. Documenter garanties contractuelles dans le ticket.

## Tests
- Unit tests:
  - services de mapping/propagation status
- Integration tests:
  - `RunController` canonical lifecycle

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test`

## Reviewer Gate
- [ ] Scope matches ticket and DoD.
- [ ] Architecture constraints respected.
- [ ] Tests are meaningful and pass.
- [ ] No regression risk left unaddressed.

## Non-goals / Out of scope
- Aucune suppression de classes ta4j.
- Aucune evolution UI Angular.
- Aucune nouvelle capacite calculatoire Python.

## Notes / pitfalls
- Attention aux regressions involontaires sur codes HTTP et payload d'erreur.
- Garder correlation `requestId/correlationId` intacte pour observabilite.
