## Title
- Stabiliser le contrat backtest Strategy Launcher entre Angular et Python

## Ticket type
- Type B: Implementation

## Status
- [x] Done

## BMAD Stage
- Reviewer (Done)

## Cross-Repo Coordination
- Cross-Repo Initiative: INIT-002
- Repo Owner: financial-project-spring
- Upstream Dependencies:
  - None (point d'entree contrat)
  - Downstream a synchroniser: ticket Python `INIT-002-python-strategy-launcher-backtest-not-implemented.md`
- Contract Version: catalog_version=2026-02-02

## Goal
- Definir et appliquer cote Spring un contrat backtest explicite pour le payload Strategy Launcher, en distinguant clairement champs supportes et non supportes afin de permettre une signalisation fiable `Not implemented yet`.

## Context / Entry points
- Controller:
  - `POST /api/runs` (`RunController.submit`) = point d'entree unique Strategy Launcher backtest
  - `GET /api/runs/{requestId}` et `GET /api/runs/{requestId}/result` pour lecture statut/resultat
- Service:
  - `PythonCanonicalRunService` pour proxy HTTP vers Python (`/runs`, `/runs/{id}`, `/runs/{id}/result`)
  - `CanonicalRunAuditService` pour audit submit/lifecycle
- Repository:
  - N/A pour la decision contrat (audit deja gere par service dedie)
- DTO:
  - `RunRequestInput` et blocs imbriques utilises en mode `LEGACY` uniquement
- Related endpoints:
  - `POST /api/runs/{requestId}/cancel` (meme politique proxy en mode canonical)
  - `POST /api/specs/preview` et `POST /api/runs/specs/preview` (legacy migration, hors source de verite)

## BMAD Handover In
- Initiative/context pack:
  - `C:\Users\ronan\Desktop\Cross-repo-coordination\Cross-repo-coordination\initiatives\INIT-002-strategy-launcher-backtest-not-implemented.md`
  - `C:\Users\ronan\Desktop\Cross-repo-coordination\Cross-repo-coordination\context-packs\CP-002-strategy-launcher-backtest-not-implemented.md`
- Payload de contrainte JIRA avec `catalog_version=2026-02-02`.

## BMAD Handover Out
- Endpoint source de verite fige: `POST /api/runs` en mode `PYTHON_CANONICAL`.
- Politique non support figee: classification fonctionnelle delegatee a Python, transport et status preserves par Spring.
- Contrat de signalisation UI fige (status/code/message/field) pour `Not implemented yet`.
- Matrice supporte/non-supporte/ignoree documentee pour passage phase Dev.

## Context7 Decision
- Required: No
- Reason: Aucun framework/API externe nouveau a investiguer; logique basee sur code et contrat locaux.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.
- Ne pas introduire de breaking change sur les champs backtest deja supportes.
- Toute regle de non-support doit etre observable (code/message metadata).

## Architecture Decisions (Phase Architect)
1. Endpoint source de verite
   - Spring expose `POST /api/runs` comme endpoint officiel de lancement backtest Strategy Launcher.
   - En `run.engine.mode=PYTHON_CANONICAL`, Spring ne reconstruit pas le payload metier et le transmet brut a Python `/runs` apres validation technique minimale (payload non vide, JSON objet, taille max).
2. Autorite contrat
   - La verite metier "champ supporte vs non supporte" est detenue par Python pour `catalog_version=2026-02-02`.
   - Spring agit comme couche de transport/tracabilite, sans reinterpretation metier des champs en mode canonical.
3. Politique champs non supportes
   - Cas attendu: Python retourne `422 Unprocessable Entity` avec details par champ.
   - Spring propage status + body JSON sans remapping metier (sauf normalisation existante `run_id` -> `requestId` sur success body).
   - Contrat cible pour UI: chaque erreur de champ non supporte doit inclure `field`, `code`, `message`; `message` doit permettre l'affichage `Not implemented yet`.
4. Compatibilite mode LEGACY
   - Le mode `LEGACY` conserve ses DTO/validations strictes et n'est pas la source de verite de ce ticket.
   - Aucune decision Architect de ce ticket ne doit casser le comportement legacy existant.

## Unsupported Field Policy Matrix (source de verite Spring)
- Supported field par Python:
  - Requete `POST /api/runs` transmise telle quelle; reponse 2xx propagee.
- Unsupported field par Python:
  - Reponse attendue 422 + erreurs structurees (field/code/message); Spring propage sans transformation metier.
- Champ inconnu/extra dans payload:
  - En mode canonical: Spring ne bloque pas au niveau metier; Python decide (reject explicite prefere).
  - En mode legacy: comportement DTO actuel conserve (hors scope de ce ticket).
- Erreur technique payload cote Spring:
  - Reponse 400 `INVALID_REQUEST` (JSON malforme, payload vide, payload non-objet, payload trop large).

## Definition of Done
- [x] Endpoint source de verite `POST /api/runs` confirme et documente.
- [x] Politique non support figee: 422 Python propage par Spring, sans reinterpretation metier.
- [x] Contrat de signalisation UI documente pour `Not implemented yet` (`field` + `code` + `message`).
- [x] Matrice supporte/non-supporte/inconnu validee avec ticket Python downstream.
- [x] Strategie de risque/rollback documentee et acceptee avant phase Dev.

## Implementation plan
1. Verifier dans code/tests que `POST /api/runs` est bien le seul chemin canonical de submit backtest.
2. Aligner avec ticket Python la convention d'erreur 422 pour champs non supportes (`field`, `code`, `message`).
3. En phase Dev, limiter Spring au role proxy/audit en mode canonical et eviter toute logique metier de classification locale.
4. Couvrir tests de contrat transport: 2xx supporte, 422 non supporte, 400 technique Spring.

## Tests
- Unit tests:
  - validation DTO / mapping erreurs-messages
- Integration tests:
  - endpoint backtest avec payload supporte et payload incluant champs non supportes

## Validation commands
- `$env:JAVA_HOME="C:\Users\ronan\.jdks\corretto-21.0.4"`
- `$env:Path="$env:JAVA_HOME\bin;$env:Path"`
- `./mvnw -Dtest=RunControllerRunsPythonCanonicalTest test`

## Reviewer Gate
- [x] Scope matches ticket and DoD.
- [x] Architecture constraints respected.
- [x] Tests are meaningful and pass.
- [x] No regression risk left unaddressed.

## Non-goals / Out of scope
- Implementer la logique quantitative Python manquante.
- Modifier le comportement UI Angular dans ce ticket.
- Changer la version de contrat au-dela de `2026-02-02`.

## Notes / pitfalls
- La decision reject vs warning doit rester coherente avec le flux existant pour ne pas casser Angular.
- Verifier la compatibilite ascendante avec anciens payloads si encore utilises.
- Eviter d'enterrer l'information "non supporte" uniquement en logs serveur.

## Risks & rollback strategy (Architect gate)
- Risque: Python ne retourne pas un `code` stable pour les champs non supportes.
  - Mitigation: fixer dans ticket Python un code explicite (ex: `NOT_IMPLEMENTED_YET` ou `UNSUPPORTED_FIELD`) et conserver fallback UI sur `message`.
- Risque: derive de contrat entre repos sur `catalog_version`.
  - Mitigation: garder `catalog_version=2026-02-02` comme reference unique dans les 3 tickets.
- Risque: regression si Spring recommence a filtrer le payload canonical.
  - Mitigation: tests de non-regression sur pass-through canonical + revue architecturale bloquante.
- Rollback:
  - Si incident en canonical, bascule controlee vers `run.engine.mode=LEGACY` (temporaire), sans changer les endpoints publics.

## Dev Execution (Review-ready)
- Code changes:
  - `src/test/java/finance/project/api/controllers/RunControllerRunsPythonCanonicalTest.java`
- Added tests (scope ticket):
  - `returnsUnsupportedFieldAsNotImplementedYetFromPython`
  - `returnsTechnicalErrorWhenPayloadIsNotJsonObject`
  - `returnsTechnicalErrorWhenPayloadIsTooLarge`
  - `forwardsUnknownFieldsToPythonInCanonicalMode`
- Validation summary:
  - Targeted: `./mvnw -Dtest=RunControllerRunsPythonCanonicalTest test` => PASS (`Tests run: 11, Failures: 0, Errors: 0`).
  - Full suite: non executee dans ce gate car hors scope du ticket.
  - Impact ticket: le comportement canonical `/api/runs` cible INIT-002 est couvert et valide par tests passes.

## Final Summary
- Changements:
  - Contrat canonical `/api/runs` fige et documente comme source de verite Spring pour INIT-002.
  - Politique champs non supportes confirmee en pass-through `422` (`field`, `code`, `message`) pour `Not implemented yet`.
  - Tests du scope ticket renforces dans `RunControllerRunsPythonCanonicalTest` (unsupported, unknown field pass-through, validations techniques non-objet et payload trop large).
- Validations:
  - `./mvnw -Dtest=RunControllerRunsPythonCanonicalTest test` => PASS (`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`).
- DoD:
  - Verifie complet (toutes les cases DoD cochees).

## Residual Risks
- Aucun risque residuel bloquant dans le scope du ticket.

