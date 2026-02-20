# AUD-106 - Sortir `CsvMonthlySplitter` du package `controllers`

## Type
Implementation

## Objectif
Corriger la separation des responsabilites en deplacant l'outil batch hors de la couche REST.

## Scope
- Deplacer `CsvMonthlySplitter` vers `scripts` ou `tools` (Java CLI) selon convention.
- Supprimer toute ambiguite de routage/controller.
- Ajouter note d'usage rapide.

## DoD
- Aucun utilitaire batch dans `controllers`.
- Le script/outillage reste executable localement.
- Architecture couche web plus propre.

## Hors scope
- Refonte du comportement de split CSV.
