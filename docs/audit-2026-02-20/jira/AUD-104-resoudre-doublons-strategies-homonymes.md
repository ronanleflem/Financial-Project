# AUD-104 - Resoudre les doublons de strategies homonymes

## Type
Implementation

## Objectif
Eliminer les collisions de noms (`MeanReversionStrategy`, `VolatilityBreakoutStrategy`) entre packages pour eviter les confusions de maintenance.

## Scope
- Identifier la version source de verite par strategie.
- Renommer ou supprimer les doublons vides.
- Verifier l'impact sur l'auto-discovery/injection Spring.

## DoD
- Plus aucun doublon de nom de classe strategie entre packages.
- Build et tests passes.
- Rationale de la version conservee documentee.

## Hors scope
- Refonte metier des strategies.

## Resolution (2026-03-05)
- Source de verite `MeanReversionStrategy`: conservee dans `strategies/statisticals` (classe Spring `@Service`).
- Doublon vide supprime: `strategies/volatility/MeanReversionStrategy.java`.
- Source de verite `VolatilityBreakoutStrategy`: conservee dans `strategies/volatility`.
- Doublon vide supprime: `strategies/statisticals/VolatilityBreakoutStrategy.java`.
- Impact Spring: aucun bean retire pour `VolatilityBreakoutStrategy` (classes vides non annotees), et `MeanReversionStrategy` conserve le bean existant.
