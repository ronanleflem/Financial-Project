# INIT-003 - Spring cutover legacy backtest routing to canonical runs

## Title
- Basculer les routes legacy de lancement vers `/api/runs*`

## Ticket type
- Type B: Implementation

## BMAD Stage
- PM

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-003
- Repo Owner: financial-project-spring
- Upstream Dependencies: INIT-003-spring-canonical-runs-contract-guardrails, INIT-003-python-backtest-signal-parity-expansion, INIT-003-python-dca-grid-preset-parity
- Contract Version: STRAT-CALC-DECOMMISSION-V1-2026-03-05

## Goal
- Rediriger progressivement les flux legacy backtest vers le chemin canonical `/api/runs*` afin de retirer la logique calculatoire Java du flux principal.

## Context / Entry points
- Controller:
  - `BacktestController`
  - `RunController`
- Service:
  - services de routing legacy/canonical
- Repository:
  - N/A
- DTO:
  - adapters legacy vers canonical request
- Related endpoints:
  - routes legacy `/run-strategy*`, `/trend-following`, `/explosion-grid`
  - endpoints canonical `/api/runs*`

## BMAD Handover In
- Guardrails contrat Spring en place.
- Parite Python prioritaire disponible sur signaux/presets cibles.

## BMAD Handover Out
- Cutover de routing effectif sur lot defini.
- Legacy endpoints marques deprecies ou proxies explicites.
- Prerequis de retrait ta4j clarifies.

## Context7 Decision
- Required: No
- Reason: aucun besoin de documentation externe, implementation locale.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.

## Definition of Done
- [ ] Flux legacy prioritaire reroute vers `/api/runs*` sans breaking API.
- [ ] Compatibilite backward documentee (deprecation strategy).
- [ ] Tests integration couvent routes legacy et canonical.
- [ ] Validation commands pass.

## Implementation plan
1. Identifier lot de routes legacy a rerouter en priorite.
2. Implementer adaptation/proxy vers request canonical.
3. Ajouter tests et notes de migration/deprecation.

## Tests
- Unit tests:
  - adapters legacy->canonical
- Integration tests:
  - parcours endpoint legacy reroute + resultat canonical

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
- Suppression complete des classes ta4j (traitee dans ticket dedie).
- Ajout de nouvelles strategies metier.

## Notes / pitfalls
- Attention aux ecarts de shape payload legacy.
- Garder trace explicite des routes restant en mode transitoire.
