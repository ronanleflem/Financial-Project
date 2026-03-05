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

## Resolution (2026-03-05)
- Retire du versioning:
  - `sqlite.db`
  - `local.duckdb`
  - `.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.3.3/spring-boot-starter-parent-3.3.3.pom.lastUpdated`
- `.gitignore` renforce avec `*.lastUpdated` (en plus de `.m2/`, `logs/` et `*.db` deja presents).
- Garde-fou contribution ajoute dans le `README` pour rappeler les artefacts locaux a ne pas committer.
