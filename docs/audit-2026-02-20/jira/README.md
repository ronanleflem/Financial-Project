# Tickets Jira proposes - Audit technique 2026-02-20

## Scope
Ces tickets couvrent une partie seulement de l'audit pour garder un decoupage fin et executable.

## Liste
- `AUD-101 [DONE]` - Nettoyer la vague 1 de classes vides
- `AUD-102 [DONE]` - Supprimer `VolumeBasedRolloverServiceNew` et clarifier le naming rollover
- `AUD-103 [DONE]` - Decommission du package `filters/old`
- `AUD-104 [DONE]` - Resoudre les doublons de strategies homonymes
- `AUD-105 [DONE]` - Nettoyer `CandleServiceJPA` des branches rollover legacy commentees
- `AUD-106` - Sortir `CsvMonthlySplitter` du package `controllers`
- `AUD-107 [DONE]` - Extraire/supprimer JavaFX du module API Spring
- `AUD-108` - Hygiene depot: retirer artefacts locaux versionnes
- `AUD-109` - Rationaliser `pom.xml` (Guava et devtools)
- `AUD-110` - Rendre `TradeFilterService` stateless/thread-safe
- `AUD-111` - Retirer `MarketData` legacy des signatures strategies/filtres

## Non couvert dans cette vague
- Arbitrage global TA4J vs run canonique Python.
- Sortie du `system scope` Maven IBKR.
- Rationalisation complete des connecteurs brokers/sources.
