# AUD-109 - Rationaliser `pom.xml` (Guava et devtools)

## Type
Implementation

## Objectif
Retirer les dependances non necessaires detectees par l'audit pour reduire la surface technique.

## Scope
- Supprimer Guava si aucun usage confirme.
- Revoir `spring-boot-devtools` (garde locale uniquement ou suppression module runtime).
- Verifier absence d'impact compilation/test.

## DoD
- Dependances retirees ou justifiees explicitement.
- Build/test passent.
- `pom.xml` simplifie.

## Hors scope
- Rationalisation complete de toutes les dependances.

## Resolution (2026-03-05)
- Guava retiree du `pom.xml` (aucun usage direct detecte dans `src/main` et `src/test`).
- Propriete `guava.version` retiree.
- `spring-boot-devtools` conservee en mode local uniquement (`scope=runtime`, `optional=true`) pour eviter la propagation transitive.
- Impact attendu: simplification du `pom.xml` sans changement fonctionnel runtime metier.
