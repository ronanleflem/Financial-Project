# AUD-110 - Rendre `TradeFilterService` stateless/thread-safe

## Type
Implementation

## Objectif
Supprimer l'etat mutable partage (`ruleAdapters`) dans le singleton Spring pour eviter les effets de bord concurrents.

## Scope
- Refactor de `TradeFilterService` pour retourner des donnees locales de methode.
- Suppression du stockage mutable interne.
- Ajuster tests unitaires/services impactes.

## DoD
- `TradeFilterService` sans etat mutable non necessaire.
- Tests de concurrence ou au minimum tests de non-regression ajoutes.
- Comportement fonctionnel conserve.

## Hors scope
- Refonte complete de la pipeline de filtres.
