# AUD-103 - Decommission du package `filters/old`

## Type
Implementation

## Objectif
Supprimer le package legacy `filters/old` (non reference) pour clarifier le chemin de filtrage officiel.

## Scope
- `src/main/java/finance/project/api/filters/old/MarketConditionsFilter.java`
- `src/main/java/finance/project/api/filters/old/TrendFilter.java`
- `src/main/java/finance/project/api/filters/old/VolumeFilter.java`

## DoD
- Fichiers legacy supprimes.
- Aucune regression dans `TradeFilterService`.
- Documentation courte "filtres actifs" mise a jour.

## Hors scope
- Refacto des filtres actifs actuels.
