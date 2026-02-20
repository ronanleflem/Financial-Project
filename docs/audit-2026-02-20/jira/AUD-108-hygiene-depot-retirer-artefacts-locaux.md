# AUD-108 - Hygiene depot: retirer artefacts locaux versionnes

## Type
Implementation

## Objectif
Nettoyer le depot des fichiers locaux non metier (logs/DB/cache) pour fiabiliser la CI et les PR.

## Scope
- Retirer du versioning: `logs/*.log`, `sqlite.db`, `local.duckdb`, `.m2/repository/*.lastUpdated`.
- Verifier et ajuster `.gitignore` si necessaire.
- Ajouter garde-fou de contribution si utile.

## DoD
- Arbre git propre sans artefacts locaux.
- `.gitignore` couvre ces cas.
- Documentation contribution alignee.

## Hors scope
- Purge historique Git.
