# INIT-003 - Audit Spring de decommission ta4j et transfert calculatoire vers Python

## Title
- Auditer les couches Java encore responsables des calculs strategie/filtres

## Ticket type
- Type A: Audit/Discovery

## Status
- [x] Review-ready

## BMAD Stage
- Reviewer (Review-ready)

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-003
- Repo Owner: financial-project-spring
- Upstream Dependencies: None
- Contract Version: STRAT-CALC-DECOMMISSION-V1-2026-03-05

## Goal
- Dresser l'inventaire exhaustif des composants Spring/Java encore relies a ta4j ou a de la logique calculatoire, puis proposer un plan de decommission progressif vers un role d'orchestration/pass-through vers Python.

## Context / Entry points
- Controller:
  - Endpoints de lancement et suivi des runs strategy/backtest
- Service:
  - Services qui preparents les payloads calculatoires ou transforment les resultats
- Repository:
  - N/A a ce stade sauf persistance liee aux meta de runs
- DTO:
  - DTO de request/response strategy et erreurs capabilities
- Related endpoints:
  - APIs Spring exposees a Angular et deleguant a Python

## BMAD Handover In
- INIT-003 et CP-003 disponibles.
- Decision cross-repo: "Python source of truth for computation".
- Liste preliminaire des modules suspectes (ta4j / indicateurs legacy).

## BMAD Handover Out
- Cartographie des points Spring a deprecier/supprimer (par priorite).
- Liste des contrats transport a figer (passthrough statuses/body, erreurs, capabilities).
- Plan de migration Spring en lots avec rollback.

## Architecture Decisions (Phase Architect)
1. Endpoint authority (Spring public contract)
   - Endpoint officiel fige pour le lancement: `POST /api/runs`.
   - Endpoints de cycle de vie confirms: `GET /api/runs/{requestId}`, `GET /api/runs/{requestId}/result`, `POST /api/runs/{requestId}/cancel`.
   - Le frontend ne doit pas dependre des endpoints preview/legacy pour le flux canonique.
2. Contract boundary (Spring vs Python)
   - Spring est responsable du transport HTTP, de la validation technique minimale et de la tracabilite.
   - Python est la source de verite metier pour la classification des champs supportes/non supportes.
   - En mode canonical, Spring ne reclassifie pas metierement les champs du payload.
3. Unsupported fields policy (frozen)
   - Cas non supporte attendu: `422 Unprocessable Entity` retourne par Python.
   - Spring propage `status` + `body` sans remapping metier.
   - Contrat d'erreur cible a preserver pour UI:
     - `field`
     - `code`
     - `message` (permet l'affichage "Not implemented yet")
   - Les erreurs techniques Spring (payload vide/non objet/taille invalide) restent en `400` technique Spring.
4. Compatibility and versioning guardrails
   - Reference de version contractuelle figee pour cette initiative:
     - `STRAT-CALC-DECOMMISSION-V1-2026-03-05`
   - Aucun breaking change de route/statut/shape sur les endpoints publics existants.
   - Datetimes en UTC et montants en `BigDecimal` (conventions de repo).
   - Toute evolution metier de capacite doit etre faite en amont Python puis repercutee en pass-through cote Spring.
5. Rollback/fallback architecture notes
   - Fallback operationnel autorise: bascule controlee de `PYTHON_CANONICAL` vers `LEGACY` via configuration, sans changer les endpoints publics.
   - Conditions de rollback:
     - derive de contrat recurrente entre Spring/Python
     - explosion du taux d'erreurs non supportees non interpretable UI
     - incident upstream Python impactant SLO.
   - Exigence de retour: conserver correlation/audit (`requestId`, `correlationId`, status family) pour diagnostic post-incident.

## Context7 Decision
- Required: No
- Reason: audit interne sur codebase Spring, sans dependance documentaire externe incertaine.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.
- Ne pas modifier le code de production dans ce ticket (audit only).

## Definition of Done
- [x] Inventaire des classes/services/endpoints relies a ta4j ou calcul Java.
- [x] Tableau de classification par item: conserver (orchestration), migrer vers Python, supprimer.
- [x] Contrats Spring <-> Python a verrouiller listes avec impacts.
- [x] Plan de decommission progressif avec fallback/rollback documente.
- [x] Validation commands pass (N/A si aucun changement code).

## Implementation plan
1. Scanner controllers/services/DTO pour localiser toute logique calculatoire legacy.
2. Qualifier chaque zone: transport technique vs calcul metier vs compat legacy.
3. Produire backlog de tickets Architect/Dev Spring ordonnes et relies aux dependances Python/Angular.

## Dev Execution (Review-ready)

### Changes (strict scope local)
- Aucun changement de code de production.
- Consolidation de l'audit INIT-003 dans ce ticket avec inventaire exhaustif et plan de decommission.

### Inventory (classes/services/endpoints relies a ta4j ou calcul Java)

1. Endpoints/API
- `src/main/java/finance/project/api/controllers/RunController.java`
  - `POST /api/runs`, `GET /api/runs/{requestId}`, `GET /api/runs/{requestId}/result`, `POST /api/runs/{requestId}/cancel`, `GET /api/runs/capabilities`
  - Role: orchestration/pass-through canonical vers Python
- `src/main/java/finance/project/api/controllers/BacktestController.java`
  - `/run-strategy`, `/run-strategy-ta4j`, `/trend-following`, `/explosion-grid`, `/run-strategy-by-name`
  - Role: calcul/backtest Java legacy
- `src/main/java/finance/project/api/controllers/StrategyController.java`
  - `/all-name-strategies`
  - Role: exposition classes strategies TA4J legacy

2. Services/strategies de calcul Java (legacy a migrer)
- `src/main/java/finance/project/api/services/TA4JService.java`
- `src/main/java/finance/project/api/services/MarketDataService.java`
- `src/main/java/finance/project/api/strategies/StrategyManager.java`
- `src/main/java/finance/project/api/strategies/ta4j/TrendFollowingStrategy.java`
- `src/main/java/finance/project/api/strategies/ta4j/ExplosionGridStrategy.java`
- `src/main/java/finance/project/api/strategies/ta4j/MacdPredictionStrategy.java`
- `src/main/java/finance/project/api/strategies/volume/EmaVolumeStrategy.java`
- `src/main/java/finance/project/api/strategies/trend/TrendContinuationStrategy.java`
- `src/main/java/finance/project/api/services/TradeFilterService.java` (pont vers filtres)
- `src/main/java/finance/project/api/filters/trend/EMAFilter.java`
- `src/main/java/finance/project/api/filters/rules/VolatilityFilter.java`
- `src/main/java/finance/project/api/filters/ta4j/*` (rules/adapter TA4J)
- `src/main/java/finance/project/api/utils/DynamicStopLossRule.java`

3. Services d'orchestration canonical (a conserver)
- `src/main/java/finance/project/api/services/PythonCanonicalRunService.java`
- `src/main/java/finance/project/api/services/CanonicalRunAuditService.java`
- `src/main/java/finance/project/api/services/RunRequestService.java` (mode legacy, fallback)
- `src/main/java/finance/project/api/services/RunStatusService.java` (mode legacy, fallback)
- `src/main/java/finance/project/api/services/RunResultService.java` (mode legacy, fallback)

### Classification matrix (conserver / migrer / supprimer)

| Item | Decision | Rationale |
| --- | --- | --- |
| `/api/runs*` via `RunController` + `PythonCanonicalRunService` | Conserver (orchestration) | Contrat canonical deja fige, Spring doit rester proxy/audit. |
| `BacktestController` endpoints de calcul (`/trend-following`, `/explosion-grid`, etc.) | Migrer vers Python | Couche fortement couplee TA4J, hors cible orchestration. |
| `StrategyManager` + `strategies/ta4j/*` | Migrer vers Python puis supprimer cote Spring | Source de calcul metier Java a decommissionner. |
| `TA4JService` + filtres TA4J (`filters/ta4j`, `EMAFilter`, `VolatilityFilter`) | Migrer vers Python puis supprimer | Logique indicateurs/regles calculatoires. |
| `StrategyController` + `StrategyDiscoveryService` (`/all-name-strategies`) | Supprimer apres migration FE | Endpoint derive du package TA4J legacy. |
| Endpoints preview legacy (`/specs/preview`, `/runs/specs/preview`) | Conserver deprecie puis retirer | Deja deprecie en canonical; retrait en lot final. |
| Mode `LEGACY` dans `RunController` | Conserver temporairement (fallback) | Necessaire pour rollback controle jusqu'a stabilisation canonical. |

### Locked contract list (Spring <-> Python)
- Endpoint authority: `POST /api/runs` + status/result/cancel sur `/api/runs/{requestId}`.
- Politique champs non supportes:
  - ownership metier Python
  - `422` pass-through par Spring
  - payload erreur attendu `field`, `code`, `message` (UI: "Not implemented yet")
- Erreurs techniques Spring:
  - `400` pour payload vide/non objet/malforme/trop large.
- Datetime UTC, montants monetaires en `BigDecimal`.
- Contract version reference: `STRAT-CALC-DECOMMISSION-V1-2026-03-05`.

### Progressive decommission plan (lots + rollback)
1. Lot A - Contract hardening (no behavior break)
   - Verrouiller tests transport canonical (2xx/422/400) et observabilite contractuelle.
2. Lot B - Feature parity Python
   - Repliquer calculs TA4J critiques cote Python avec conventions d'erreurs stables.
3. Lot C - Spring routing cutover
   - Basculer appels FE des endpoints legacy calculatoires vers `/api/runs*` canonical.
4. Lot D - Legacy retirement
   - Deprecier puis supprimer `BacktestController` calculatoire, `StrategyManager`, `strategies/ta4j`, `TA4JService`, `StrategyController`.
5. Lot E - Cleanup final
   - Retirer `run.engine.mode=LEGACY` quand SLO canonical stabilises et rollback non requis.

Rollback:
- Repli operationnel via `run.engine.mode=LEGACY` sans changer les routes publiques.
- Garde-fous: correlationId/requestId, audit lifecycle, tracking des 422 non supportes.

## Tests
- Unit tests: N/A (audit only)
- Integration tests: N/A (audit only)

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw test` (optionnel, non bloquant pour Type A sans code change)

## Validation summary (Dev phase)
- Inventory/coverage commands executed:
  - `rg -n "org.ta4j|ta4j|..."`
  - `rg -n "@GetMapping|@PostMapping|..."`
  - `rg -n "class .*Service|execute\\(|convertToTimeSeries|..."`
- Resultat: cartographie compilee dans ce ticket (sections Inventory + Classification matrix).
- Test command execute:
  - `./mvnw -q "-Dmaven.repo.local=.m2/repository" -Dtest=RunControllerRunsPythonCanonicalTest test`
  - Statut: echec environnemental hors scope (`com.ib:tws-api-proto:10.40` introuvable).
  - Impact ticket: non bloquant pour Type A (aucun changement de code production).

## Reviewer Gate
- [x] Scope matches ticket and DoD.
- [x] Architecture constraints respected.
- [x] Tests are meaningful and pass (N/A si aucun code change).
- [x] No regression risk left unaddressed.

## Non-goals / Out of scope
- Aucune suppression effective de classes ta4j dans ce ticket.
- Aucune evolution fonctionnelle des strategies.
- Aucune modification frontend.

## Notes / pitfalls
- Attention aux usages indirects via wrappers utilitaires/factories.
- Verifier les transformations implicites request/response qui masquent des differences contractuelles.
- Garder trace des dependances transitoires necessaires pour migration sans downtime.
