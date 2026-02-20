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
