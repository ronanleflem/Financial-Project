# AUD-105 - Nettoyer `CandleServiceJPA` des branches rollover legacy commentees

## Type
Implementation

## Objectif
Retirer le code commente mort autour de l'ancien chemin rollover pour rendre la classe lisible et univoque.

## Scope
- Nettoyage des blocs commentes legacy dans `CandleServiceJPA`.
- Conserver uniquement le flux rollover effectivement execute.
- Ajouter un commentaire court sur la raison du choix.

## DoD
- `CandleServiceJPA` sans branches mortes commentees.
- Comportement fonctionnel inchange.
- Tests de non-regression sur endpoints impactes.

## Hors scope
- Changement d'algorithme rollover.
