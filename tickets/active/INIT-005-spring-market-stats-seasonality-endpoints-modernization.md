# INIT-005 - Spring modernization endpoints market stats/saisonnalite

## Title
- Mettre a jour/deprecier endpoints Spring market stats et saisonnalite

## Ticket type
- Type B: Implementation

## BMAD Stage
- Architect

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-005
- Repo Owner: financial-project-spring
- Upstream Dependencies: INIT-005-python-market-stats-seasonality-endpoints-modernization
- Contract Version: MARKET-STATS-SEASONALITY-V1-2026-03-05

## Goal
- Exposer des endpoints cibles fiables pour market stats/saisonnalite et sortir des endpoints legacy relies aux anciennes data/mock.

## Context / Entry points
- Controller:
  - controllers stats/saisonnalite legacy et cibles
- Service:
  - services de mapping et d'appel source data Python
- Repository:
  - N/A (selon implementation)
- DTO:
  - DTO stats/saisonnalite exposes a Angular
- Related endpoints:
  - endpoints legacy a deprecier/supprimer
  - endpoints cibles a stabiliser

## BMAD Handover In
- Source data Python canonical definie.
- Scope Angular de migration confirme.

## BMAD Handover Out
- Endpoints Spring cibles operationnels et testes.
- Endpoints legacy identifies et traites (deprecation/suppression).
- Contrat DTO stable pour Angular.

## Context7 Decision
- Required: No
- Reason: implementation backend interne sans dependance externe incertaine.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.

## Architecture Decisions (Phase Architect)
1. Endpoint authority (public Spring contract)
   - Endpoint source de verite pour lancement market stats/saisonnalite: `POST /api/runs`.
   - Endpoints de cycle de vie associes:
     - `GET /api/runs/{requestId}`
     - `GET /api/runs/{requestId}/result`
     - `POST /api/runs/{requestId}/cancel`
   - Endpoint de capacites contractuelles:
     - `GET /api/runs/capabilities?spec_type=market_stats|seasonality`
2. Contrat cible (boundary Spring vs Python)
   - Spring est couche d'orchestration/transport/audit; Python est source de verite metier.
   - En mode `PYTHON_CANONICAL`, Spring ne reinterprete pas metierement les champs `stats`/`seasonality`.
   - Les champs datetime restent en UTC; montants/performance monetaires en `BigDecimal`.
3. Endpoints legacy et deprecation
   - `POST /api/specs/preview` et `POST /api/runs/specs/preview` restent hors contrat cible:
     - `410 LEGACY_PREVIEW_DISABLED` en canonical
     - fallback legacy temporaire seulement si `run.engine.mode=LEGACY`
   - Objectif migration Angular: ne plus dependre des endpoints preview.
4. Politique champs non supportes (figee)
   - Proprietaire metier: Python.
   - Cas non supporte attendu: `422 Unprocessable Entity` renvoye par Python.
   - Spring propage `status` + `body` sans remapping metier.
   - Contrat erreur cible pour UI:
     - `field`
     - `code`
     - `message` (affichage possible "Not implemented yet")
   - Erreurs techniques Spring (payload vide/non objet/malforme/trop large): `400 INVALID_REQUEST`.
5. Guardrails compatibilite/versioning
   - Version de reference: `MARKET-STATS-SEASONALITY-V1-2026-03-05`.
   - Aucun breaking change de route/statut/shape sur les endpoints publics deja utilises.
   - Toute evolution de support de champs doit etre publiee cote Python (capabilities), puis consommee en pass-through cote Spring.
6. Rollback/fallback
   - En incident canonical: bascule controlee vers `run.engine.mode=LEGACY` sans changer les routes publiques.
   - Conditions de rollback:
     - derive de contrat recurrente Spring/Python
     - taux 422 non interpretable UI
     - indisponibilite upstream Python impactant SLO
   - Tracabilite minimale requise: `requestId`, `correlationId`, endpoint, status family.

## Unsupported Field Policy Matrix
- Champ supporte (Python):
  - `POST /api/runs` -> reponse `2xx` propagee telle quelle (avec mappings transport existants).
- Champ non supporte (Python):
  - `422` propagee telle quelle avec details `field/code/message`.
- Champ inconnu/extra:
  - canonical: Spring laisse Python arbitrer metierement.
  - legacy: comportement DTO/validation existant conserve (hors contrat cible).
- Erreur technique payload cote Spring:
  - `400 INVALID_REQUEST`.

## Definition of Done
- [ ] Endpoints cibles stats/saisonnalite exposes.
- [ ] Endpoints legacy depreciees/supprimes selon plan.
- [ ] DTO validation et mapping couverts.
- [ ] Unit and integration tests added or updated.
- [ ] Validation commands pass.

## Implementation plan
1. Cartographier endpoints legacy et definir contrat cible.
2. Implementer/adapter controllers/services/DTO.
3. Ajouter tests et documentation de migration/deprecation.

## Tests
- Unit tests:
  - mapping service/DTO
- Integration tests:
  - endpoints stats/saisonnalite (legacy et cibles)

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
- Refonte globale architecture backend.
- Ajout de nouvelles families de metriques hors scope.

## Notes / pitfalls
- Maitriser la compatibilite backward pendant la transition Angular.
