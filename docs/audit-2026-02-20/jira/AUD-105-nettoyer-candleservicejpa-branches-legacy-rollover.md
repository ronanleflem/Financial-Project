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

## Resolution (2026-03-05)
- Suppression des blocs commentes legacy autour de l'ancien chemin `VolumeBasedRolloverService`.
- Conservation du seul flux execute: `VolumeBasedRolloverNewService#getDynamicRolloverCandlesSessionWithMinuteFallbackGlobalIndexed(...)`.
- Retrait de la dependance `VolumeBasedRolloverService` de `CandleServiceJPA` (devenue inutilisee).
- Ajout d'un commentaire court dans `CandleServiceJPA` pour expliciter le choix du flux actif.
