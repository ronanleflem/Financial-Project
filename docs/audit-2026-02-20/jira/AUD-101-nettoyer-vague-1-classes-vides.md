# AUD-101 [DONE] - Nettoyer la vague 1 de classes vides

## Type
Implementation

## Objectif
Supprimer les classes vides sans usage pour reduire le bruit et la dette de maintenance.

## Scope
- `config`: `BacktestConfig`, `LiveConfig`
- `services/backtest`: `BacktestExecutionService`, `CandleBacktestService`, `ComparisonBacktestService`, `SymbolBacktestService`
- `services/live`: `CandleLiveService`, `ComparisonLiveService`, `LiveExecutionService`, `SymbolLiveService`
- `services`: `RiskManagementService`, `TradeExecutionService`
- `entities`: `MarketData` (si non utilisee apres verification compile)

## DoD
- Classes ciblees supprimees.
- Compilation et tests unitaires verts.
- Aucun bean Spring casse au demarrage.

## Statut
- DONE le 2026-02-20 (sur le scope safe).
- `MarketData` non supprime dans ce ticket car encore reference dans des signatures (`Filter`, `Strategy`, `BaseStrategy`, `TradeFilterService`).
- Le traitement de `MarketData` est decoupe dans `AUD-111`.

## Hors scope
- Suppression des strategies placeholders.
- Arbitrage fonctionnel TA4J/Python.
