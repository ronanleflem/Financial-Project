# AUD-102 - Supprimer `VolumeBasedRolloverServiceNew` et clarifier le naming rollover

## Type
Implementation

## Objectif
Retirer la classe orpheline `VolumeBasedRolloverServiceNew` et eviter l'ambiguite avec `VolumeBasedRolloverNewService`.

## Scope
- Supprimer `src/main/java/finance/project/api/services/VolumeBasedRolloverServiceNew.java`
- Verifier qu'aucune reference compile/runtime ne subsiste.
- Ajouter une note d'architecture courte dans le code/documentation sur le service rollover cible.

## DoD
- Classe orpheline retiree.
- Nommage/intent du service actif explicite.
- Tests existants rollover inchanges et verts.

## Hors scope
- Refonte algorithmique du rollover.
