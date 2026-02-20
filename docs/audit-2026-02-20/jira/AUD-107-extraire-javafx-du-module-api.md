# AUD-107 - Extraire/supprimer JavaFX du module API

## Type
Implementation

## Objectif
Retirer la dependance UI desktop (JavaFX) du service backend Spring pour reduire le classpath et clarifier le perimetre.

## Scope
- `TradingChartApp` sorti vers module/outillage dedie ou supprime.
- Dependances JavaFX retirees de `pom.xml`.
- `javafx-maven-plugin` retire.

## DoD
- `pom.xml` backend ne contient plus JavaFX.
- Build backend reste vert.
- Documentation mise a jour sur le statut de l'outil graphique.

## Hors scope
- Developpement d'un nouveau frontend.
