# Project Overview

## Summary
The service is a Spring Boot backend for ingesting financial market data, enriching it with technical indicators, and exposing analytics for strategy backtests and visualization clients. Candle endpoints stream OHLCV data, currency conversions, CSV imports, and Binance downloads, while also triggering CME aggregation workflows.【F:src/main/java/finance/project/api/controllers/CandleController.java†L45-L323】 Backtesting routes orchestrate TA4J-powered strategies, persist executions, and expose stored trade and performance results for front-end consumers.【F:src/main/java/finance/project/api/controllers/BacktestController.java†L31-L173】【F:src/main/java/finance/project/api/strategies/StrategyManager.java†L24-L99】

## Technology stack
- **Framework**: Spring Boot 3.3.3 with Java 21.【F:pom.xml†L5-L38】
- **Analytics**: TA4J core 0.17 for technical indicators and rule engines.【F:pom.xml†L34-L39】【F:src/main/java/finance/project/api/services/TA4JService.java†L17-L82】
- **Data access**: Spring Data JPA with MySQL runtime connector and Hibernate dialects.【F:pom.xml†L52-L83】【F:src/main/resources/application.properties†L20-L34】
- **Mapping & tooling**: MapStruct for DTO mappers and Lombok for boilerplate reduction.【F:pom.xml†L29-L101】
- **Auxiliary APIs**: REST clients for Binance, Marketstack, Yahoo Finance, Alpha Vantage, and CurrencyLayer to source market data.【F:pom.xml†L63-L138】【F:src/main/java/finance/project/api/services/BinanceService.java†L20-L151】【F:src/main/java/finance/project/api/services/MarketstackService.java†L12-L40】【F:src/main/java/finance/project/api/services/CurrencyLayerService.java†L12-L69】【F:src/main/java/finance/project/api/services/CandleServiceJPA.java†L132-L200】

## Build & run
- Use the bundled Maven wrapper to build or launch the application, e.g. `./mvnw spring-boot:run` (leverages the Spring Boot Maven plugin configured in the project).【F:mvnw†L1-L39】【F:pom.xml†L141-L146】
- Active configuration lives in `application.properties`; no additional Spring profiles are defined. Key settings include the HTTP port, enabled strategy/filter lists, and MySQL datasource credentials.【F:src/main/resources/application.properties†L1-L34】
- External API keys (Marketstack, CurrencyLayer, Nasdaq, Binance URL) are currently stored in properties and should be externalized for production security.【F:src/main/resources/application.properties†L42-L53】

## Repository layout (excerpt)
```
src/main/java/finance/project/api/
├── controllers/        # REST entry points for candles, strategies, rollover, config, etc.
├── services/           # Data ingestion, caching, analytics, persistence, and integrations
├── filters/            # TA4J adapters and custom scoring filters
├── strategies/         # Strategy orchestration and TA4J strategy implementations
├── repositories/       # Spring Data repositories for domain aggregates
├── entities/           # JPA entities for symbols, candles, trades, filters, symbology…
└── model/              # DTOs exchanged with the API
```
_Source directories derived from the repository structure._【e4dbb2†L1-L116】
