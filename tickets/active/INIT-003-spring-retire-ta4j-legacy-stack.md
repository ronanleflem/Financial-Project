# INIT-003 - Spring retire ta4j legacy stack

## Title
- Deprecier puis supprimer la pile ta4j cote Spring

## Ticket type
- Type B: Implementation

## BMAD Stage
- PM

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-003
- Repo Owner: financial-project-spring
- Upstream Dependencies: INIT-003-spring-cutover-legacy-backtest-routing, INIT-003-angular-runs-cross-page-regression-tests
- Contract Version: STRAT-CALC-DECOMMISSION-V1-2026-03-05

## Goal
- Retirer progressivement les classes/services ta4j obsoletes une fois cutover canonical valide et UX/front stabilises.

## Context / Entry points
- Controller:
  - `BacktestController` legacy
  - `StrategyController` legacy
- Service:
  - `TA4JService`, `StrategyManager`, services/filtres ta4j
- Repository:
  - N/A
- DTO:
  - DTO legacy uniquement
- Related endpoints:
  - legacy strategy endpoints decommissionnes

## BMAD Handover In
- Cutover legacy routing complete.
- Regression tests front valides sur parcours canonical.

## BMAD Handover Out
- Pile ta4j retiree ou isolee hors runtime actif.
- Endpoints/classes legacy supprimes ou archives selon strategie.
- Documentation decommission mise a jour.

## Context7 Decision
- Required: No
- Reason: suppression de code interne.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.

## Definition of Done
- [ ] Inventaire ta4j final marque supprime/deprecie.
- [ ] Endpoints legacy non utilises retires.
- [ ] Tests regression backend passent apres retrait.
- [ ] Validation commands pass.

## Implementation plan
1. Marquer deprecie ce qui reste transitoire.
2. Supprimer classes/services ta4j plus references.
3. Executer batterie de tests et verifier absence de references mortes.

## Tests
- Unit tests:
  - compilation/reference checks
- Integration tests:
  - parcours canonical sans endpoints legacy

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
- Nouvelles capacites metier.
- Refonte API hors decommission legacy.

## Notes / pitfalls
- Verifier usages indirects/reflection avant suppression definitive.
