## Title
- Stabiliser le contrat backtest Strategy Launcher entre Angular et Python

## Ticket type
- Type B: Implementation

## BMAD Stage
- PM

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
  - endpoint backtest recevant le payload strategy-launcher (path exact a confirmer)
- Service:
  - orchestration de validation/forward vers Python
- Repository:
  - N/A (sauf si persistence d'historique run)
- DTO:
  - DTO de requete backtest et objets imbriques (`data`, `strategy`, `signal`, `filters`)
- Related endpoints:
  - endpoint de lancement backtest + endpoint de lecture statut/resultat associes

## BMAD Handover In
- Initiative/context pack:
  - `C:\Users\ronan\Desktop\Cross-repo-coordination\Cross-repo-coordination\initiatives\INIT-002-strategy-launcher-backtest-not-implemented.md`
  - `C:\Users\ronan\Desktop\Cross-repo-coordination\Cross-repo-coordination\context-packs\CP-002-strategy-launcher-backtest-not-implemented.md`
- Payload de contrainte JIRA avec `catalog_version=2026-02-02`.

## BMAD Handover Out
- Contrat Spring documente: champs acceptes, non supportes, comportement de validation.
- DTO/validation et mapping backend alignes sur la version de contrat.
- Consignes de signalisation exploitables par Angular (message/code).

## Context7 Decision
- Required: No
- Reason: Aucun framework/API externe nouveau a investiguer; logique basee sur code et contrat locaux.

## Constraints & conventions
- Preserve existing REST/error conventions.
- Use BigDecimal for monetary values.
- Use UTC for datetime fields unless specified otherwise.
- Ne pas introduire de breaking change sur les champs backtest deja supportes.
- Toute regle de non-support doit etre observable (code/message metadata).

## Definition of Done
- [ ] Les DTO/validations Spring couvrent explicitement le schema `catalog_version=2026-02-02`.
- [ ] Le comportement pour champ non supporte est defini (rejet explicite ou warning standardise) et documente.
- [ ] Les informations necessaires a l'UI pour afficher `Not implemented yet` sont disponibles.
- [ ] Tests unitaires/integration couvrent au moins un cas supporte et un cas non supporte.
- [ ] Commandes de validation passent.

## Implementation plan
1. Inventorier les champs payload recus par Spring et leur forwarding actuel vers Python.
2. Formaliser la matrice contrat: supporte / non supporte / ignore avec justification.
3. Mettre a jour DTO + validation + mapping de reponse pour exposer un statut exploitable.
4. Ajouter tests de contrat (supporte vs non supporte) et verifier non-regression.

## Tests
- Unit tests:
  - validation DTO / mapping erreurs-messages
- Integration tests:
  - endpoint backtest avec payload supporte et payload incluant champs non supportes

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
- Implémenter la logique quantitative Python manquante.
- Modifier le comportement UI Angular dans ce ticket.
- Changer la version de contrat au-dela de `2026-02-02`.

## Notes / pitfalls
- La decision reject vs warning doit rester coherente avec le flux existant pour ne pas casser Angular.
- Verifier la compatibilite ascendante avec anciens payloads si encore utilises.
- Eviter d'enterrer l'information "non supporte" uniquement en logs serveur.
