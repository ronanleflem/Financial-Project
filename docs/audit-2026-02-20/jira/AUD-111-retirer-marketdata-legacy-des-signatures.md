# AUD-111 - Retirer `MarketData` legacy des signatures strategies/filtres

## Type
Implementation

## Objectif
Supprimer la dependance au placeholder `MarketData` encore present dans les interfaces/signatures legacy afin de permettre sa suppression propre.

## Contexte
`MarketData` est vide mais encore utilise dans des signatures:
- `src/main/java/finance/project/api/filters/Filter.java`
- `src/main/java/finance/project/api/strategies/Strategy.java`
- `src/main/java/finance/project/api/strategies/BaseStrategy.java`
- `src/main/java/finance/project/api/services/TradeFilterService.java`

## Scope
- Refactor des signatures impactees pour ne plus dependre de `MarketData`.
- Adaptation des implementations de strategies/filtres concernees.
- Suppression de `src/main/java/finance/project/api/entities/MarketData.java`.

## DoD
- Plus aucune reference compile-time a `MarketData`.
- `MarketData.java` supprime.
- Build/tests verts.
- Impact API nul (pas de changement de contrat REST).

## Hors scope
- Refonte metier des strategies.
- Arbitrage TA4J vs run canonique Python.
