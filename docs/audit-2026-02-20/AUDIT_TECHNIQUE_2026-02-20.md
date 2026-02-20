# Audit Technique - Code inutilise, redondances, derive architecture

Date: 2026-02-20

## 1. Perimetre et methode
- Analyse statique du code source (`src/main/java`, `src/test/java`) et du build (`pom.xml`).
- Verification des references croisees (recherche d'usages par nom de classe).
- Verification des artefacts versionnes non metier.

Limite:
- La compilation Maven n'a pas pu etre executee dans ce sandbox (repo local Maven inaccessible). Les constats sont donc classes par niveau de confiance.

## 2. Resume executif
- Constat majeur: forte dette de nettoyage. `40` classes vides detectees sur `392` classes main Java.
- Constat majeur: coexistence de plusieurs "lignes" fonctionnelles (legacy TA4J, run canonique Python, scaffolding backtest/live) sans decommission explicite.
- Constat majeur: artefacts locaux versionnes dans le depot (logs, DB locales, cache Maven).

## 2.1 Decoupage en tickets Jira (vague 1)
Tickets crees dans `docs/audit-2026-02-20/jira/`.

Couverture par section:
- 3.A Inutilise probable: `AUD-101 [DONE]`, `AUD-102`, `AUD-103`, `AUD-111`
- 3.B Redondances / doublons: `AUD-104`, `AUD-102`
- 3.C Fragilites architecture: `AUD-105`, `AUD-106`, `AUD-107`, `AUD-110`
- 3.D Build/dependances: `AUD-109`, `AUD-107`
- 3.E Hygiene depot: `AUD-108`

Sujets volontairement non couverts dans cette vague:
- Arbitrage global TA4J vs run canonique Python.
- Sortie du `system scope` Maven IBKR.
- Rationalisation complete des connecteurs brokers/sources.

## 3. Constats detailles

### A. Inutilise probable (confiance elevee)
Tickets associes: `AUD-101 [DONE]`, `AUD-102`, `AUD-103`, `AUD-111`

1. Classes vides, sans logique metier.
Exemples:
- `src/main/java/finance/project/api/config/BacktestConfig.java:3`
- `src/main/java/finance/project/api/config/LiveConfig.java:3`
- `src/main/java/finance/project/api/services/backtest/BacktestExecutionService.java:3`
- `src/main/java/finance/project/api/services/backtest/CandleBacktestService.java:3`
- `src/main/java/finance/project/api/services/backtest/ComparisonBacktestService.java:3`
- `src/main/java/finance/project/api/services/backtest/SymbolBacktestService.java:3`
- `src/main/java/finance/project/api/services/live/CandleLiveService.java:3`
- `src/main/java/finance/project/api/services/live/ComparisonLiveService.java:3`
- `src/main/java/finance/project/api/services/live/LiveExecutionService.java:3`
- `src/main/java/finance/project/api/services/live/SymbolLiveService.java:3`
- `src/main/java/finance/project/api/services/RiskManagementService.java:3`
- `src/main/java/finance/project/api/services/TradeExecutionService.java:3`
- `src/main/java/finance/project/api/entities/MarketData.java:3`

2. Classe orpheline non referencee:
- `src/main/java/finance/project/api/services/VolumeBasedRolloverServiceNew.java:3`

3. Package explicitement legacy/non utilise:
- `src/main/java/finance/project/api/filters/old/MarketConditionsFilter.java:1`
- `src/main/java/finance/project/api/filters/old/TrendFilter.java:1`
- `src/main/java/finance/project/api/filters/old/VolumeFilter.java:1`

### B. Redondances / doublons (confiance elevee)
Tickets associes: `AUD-104`, `AUD-102`

1. Doublons de noms de strategies:
- `src/main/java/finance/project/api/strategies/volatility/VolatilityBreakoutStrategy.java:3`
- `src/main/java/finance/project/api/strategies/statisticals/VolatilityBreakoutStrategy.java:3`
- `src/main/java/finance/project/api/strategies/volatility/MeanReversionStrategy.java:3`
- `src/main/java/finance/project/api/strategies/statisticals/MeanReversionStrategy.java:10`

2. Triple lineage "rollover" (legacy + new + new-old):
- `src/main/java/finance/project/api/services/VolumeBasedRolloverService.java:26`
- `src/main/java/finance/project/api/services/VolumeBasedRolloverNewService.java:34`
- `src/main/java/finance/project/api/services/VolumeBasedRolloverServiceNew.java:3`

3. Classe de base IT dupliquee:
- `src/test/java/finance/project/api/AbstractMySqlIntegrationTest.java:5` (vide)
- `src/test/java/finance/project/api/support/AbstractMySqlIntegrationTest.java:10` (implementation Testcontainers)

### C. Points qui cassent/fragilisent l'architecture (confiance elevee)
Tickets associes: `AUD-105`, `AUD-106`, `AUD-107`, `AUD-110`

1. Utilitaire batch place dans `controllers`:
- `src/main/java/finance/project/api/controllers/CsvMonthlySplitter.java:7`

2. UI JavaFX desktop dans un backend Spring API:
- `src/main/java/finance/project/api/frontend/chart/TradingChartApp.java:26`

3. Injection legacy encore presente mais chemin desactive par commentaires (drift):
- `src/main/java/finance/project/api/services/CandleServiceJPA.java:46`
- `src/main/java/finance/project/api/services/CandleServiceJPA.java:47`
- `src/main/java/finance/project/api/services/CandleServiceJPA.java:672`
- `src/main/java/finance/project/api/services/CandleServiceJPA.java:682`

4. Contrat de date incoherent:
- `LocalDateTime` avec `ISO.DATE` dans `src/main/java/finance/project/api/controllers/VolumeBasedRolloverController.java:44`

5. Service mutable potentiellement non thread-safe:
- Champ `ruleAdapters` modifie dans un singleton Spring: `src/main/java/finance/project/api/services/TradeFilterService.java:22`

### D. Build/dependances a rationaliser (confiance elevee)
Tickets associes: `AUD-109`, `AUD-107`

1. Dependance declaree sans usage detecte:
- Guava dans `pom.xml:153` (aucun import `com.google.common` detecte).

2. JavaFX probablement hors scope backend:
- Dependances JavaFX: `pom.xml:268`
- Plugin JavaFX: `pom.xml:418`
- `mainClass` placeholder incoherente: `pom.xml:426` (`com.votrepackage.Main`)

3. Mode `system` Maven (fragile/non portable):
- `pom.xml:114`
- `pom.xml:115`

4. TA4J encore largement utilise dans le code Java:
- Dependance: `pom.xml:100`
- Controller + strategies + filtres TA4J (ex: `src/main/java/finance/project/api/controllers/BacktestController.java:72`, `src/main/java/finance/project/api/strategies/ta4j/TrendFollowingStrategy.java:1`)

### E. Hygiene depot (confiance elevee)
Tickets associes: `AUD-108`

Fichiers/artefacts locaux actuellement versionnes:
- `logs/*.log`
- `sqlite.db`
- `local.duckdb`
- `.m2/repository/...lastUpdated`

## 4. Sections necessitant arbitrage fonctionnel (intervention de ta part)

1. TA4J cote Java Spring
- Decision a prendre: decommission totale, partielle, ou maintien.
- Impact si suppression totale: `BacktestController`, `StrategyManager`, strategies TA4J, `TA4JService`, filtres TA4J, endpoint `/all-name-strategies`.

2. Coexistence "run canonique Python" vs "backtest Java legacy"
- Decision a prendre: garder 2 chemins d'execution ou converger vers un seul.
- Impact: simplification massive des controllers/services legacy et des DTO associes.

3. Brokers/sources a conserver
- IBKR, Dukascopy, Binance/Bitget/Mexc/Bybit/Okx, Yahoo.
- Decision a prendre pour reduire les connecteurs non prioritaires et leurs dependances.

4. JavaFX local chart
- Decision a prendre: sortir dans un module outillage separe, ou supprimer.

5. Scaffolding strategies vides
- Decision a prendre: conserver comme backlog technique explicite (avec ticket), ou supprimer maintenant.

## 4.1 Avancement realise
- `AUD-101 [DONE]` execute le 2026-02-20 sur la vague "safe":
  - Supprime: `BacktestConfig`, `LiveConfig`
  - Supprime: `BacktestExecutionService`, `CandleBacktestService`, `ComparisonBacktestService`, `SymbolBacktestService`
  - Supprime: `CandleLiveService`, `ComparisonLiveService`, `LiveExecutionService`, `SymbolLiveService`
  - Supprime: `RiskManagementService`, `TradeExecutionService`
- Decoupage complementaire cree: `AUD-111` pour retirer proprement `MarketData` des signatures legacy avant suppression de l'entite.

## 5. Plan de nettoyage recommande

### Lot 1 (safe, immediate)
- Supprimer classes vides non referencees.
- Supprimer `VolumeBasedRolloverServiceNew`.
- Nettoyer `filters/old` si non requis.
- Retirer artefacts versionnes (`logs`, DB locales, `.m2`) et renforcer `.gitignore`.

### Lot 2 (architecture)
- Deplacer `CsvMonthlySplitter` vers `scripts/` ou module tooling.
- Isoler/supprimer JavaFX du module API.
- Corriger incoherences de contrats REST (dates, routes, conventions).

### Lot 3 (fonctionnel)
- Apres arbitrage: decommission TA4J ou stabilisation officielle.
- Retirer le chemin legacy non retenu (controllers/services/tests/doc).

## 6. Priorites de risque
- Haute: classes vides et chemins legacy ambigus (maintenance, comprehension, regressions).
- Haute: artefacts locaux versionnes (pollution depot, CI instable).
- Moyenne: dependances inutiles et `system scope` (portabilite/dev onboarding).
- Moyenne: incoherences d'API (contrats de date, conventions de endpoints).
