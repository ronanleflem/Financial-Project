# Démarrage rapide (A → Z), sans Java/Angular

Ce guide montre comment **installer** le moteur Python, **lancer des runs** (stats & saisonnalité) et **inspecter les résultats**, **sans** dépendre de l’API Java Spring ni du front Angular. Il utilise un **CSV synthétique** et des **Specs d’exemple**.

---

## 1) Prérequis

- Python 3.10+ (recommandé)
- [Poetry](https://python-poetry.org/) installé
- (Optionnel) MySQL si vous voulez persister en base (`quant`), **pas nécessaire** pour ce guide

---

## 2) Installation

```bash
# Depuis la racine du repo
poetry install
poetry shell  # ou utiliser "poetry run" devant chaque commande
```

Si le projet utilise `pre-commit` :

```bash
pre-commit install
```

---

## 3) Données : CSV synthétique (fourni)

Un petit dataset **OHLCV M1** est fourni pour test :
`data/examples/eurusd_m1_synth.csv`

Format attendu :

```
ts,symbol,open,high,low,close,volume
2025-01-01T00:00:00Z,EURUSD,1.1000,1.1010,1.0990,1.1005,100
...
```

> ⚠️ Timestamps en **UTC**, colonnes en minuscules.

---

## 4) Exemples de Specs (fichiers fournis)

### a) Stats : `specs/stats_examples_min.json`

Un exemple avec 3 events/conditions/targets minimaux pour produire `stats_summary.parquet`.

Lancer :

```bash
poetry run qe stats run --spec specs/stats_examples_min.json
```

Afficher des extraits :

```bash
poetry run qe stats show --symbol EURUSD --event two_up_candles --target next_bar_up
```

Artefacts :

* `runs/stats_examples_min/stats_summary.parquet`
* (si activé) insertions en DB

### b) Saisonniété : `specs/seasonality_m1_3d_min.json`

Un exemple sur **3 jours** M1 (rapide) qui calcule des profils par heure/jour.

Lancer :

```bash
poetry run qe seasonality run --spec specs/seasonality_m1_3d_min.json
```

Artefacts :

* `runs/seasonality_m1_3d_min/seasonality_profiles.parquet`
* `summary.json` (selon implémentation)

---

## 5) API locale (optionnel)

Démarrer l’API (FastAPI/uvicorn) :

```bash
poetry run uvicorn quant_engine.api.app:app --reload --port 8000
```

Exemples `curl` :

```bash
# Exécuter un run stats (synchrone, simple)
curl -X POST http://localhost:8000/stats/run \
  -H "Content-Type: application/json" \
  --data-binary @specs/stats_examples_min.json

# Liste des stats (filtrées)
curl "http://localhost:8000/stats?symbol=EURUSD&target=up_next_bar&min_n=30&method=freq"
```

> Pour ce guide, **aucune** connexion à l’API Java ni au front Angular n’est nécessaire.

---

## 6) (Optionnel) MySQL : lecture OHLCV et persistance résultats

Vous pouvez lancer sans DB (fichiers Parquet/JSON).
Si vous souhaitez **persister** dans MySQL (schéma `quant`) **ou** **lire** l’OHLCV depuis `marketdata`, configurez :

```bash
# Lecture OHLCV (schema marketdata)
export QE_MARKETDATA_MYSQL_URL='mysql+pymysql://py_user:***@mysql-host:3306/marketdata'

# Persistance résultats (schema quant)
export QE_DB_URL='mysql+pymysql://py_user:***@mysql-host:3306/quant'
```

Exemples de Spec `data.mysql` (lecture directe OHLCV) :

```json
{
  "data": {
    "mysql": {
      "env_var": "QE_MARKETDATA_MYSQL_URL",
      "schema": "marketdata",
      "table": "ohlcv_m1",
      "symbol_col": "symbol",
      "ts_col": "ts_utc",
      "open_col": "open",
      "high_col": "high",
      "low_col": "low",
      "close_col": "close",
      "volume_col": "volume",
      "timeframe_col": null
    },
    "symbols": ["EURUSD"],
    "timeframe": "M1",
    "start": "2025-01-01T00:00:00Z",
    "end": "2025-01-03T23:59:00Z"
  }
}
```

Rappels :

* **CSV prioritaire** si `dataset_path` est fourni.
* **Même serveur** possible : `marketdata` (READ) pour OHLCV, `quant` (WRITE) pour résultats.

---

## 7) Qualité & Tests

Lint/tests :

```bash
poetry run ruff check .
poetry run black --check .
poetry run mypy .
poetry run pytest -q
```

> Les tests MySQL sont **skippés** si `QE_MARKETDATA_MYSQL_URL` n’est pas défini.

---

## 8) Dépannage rapide

* **Aucun résultat** : vérifiez l’intervalle `start/end`, `symbol` et que votre CSV contient des lignes.
* **Timestamps** : doivent être en **UTC** ; sinon bins `hour/dow` seront faux.
* **MySQL** : privilégiez un **user lecture seule** pour `marketdata`; index `(symbol, ts)` recommandés.

---

## 9) Et ensuite ?

* Activez WFA (walk-forward) dans vos Specs pour valider hors-échantillon.
* Testez la **saisonniété** par `session` / `is_month_end` (cf. doc Seasonality).
* Utilisez l’**optimiseur** (Optuna) pour explorer des hyperparams (seasonality ou stratégie).

```
