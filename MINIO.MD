Lancer MinIO :
Dans un terminal
cd C:\minio
minio.exe server C:\minio\data --console-address ":9090"
Par défaut (si non changé) :
endpoint API : http://localhost:9000
console UI : http://localhost:9090
access key : minioadmin
secret key : minioadmin

Créer ton bucket :
Dans http://localhost:9090 :
login avec minioadmin / minioadmin
crée un bucket : quant-delta-dev
(optionnel) crée un dossier delta/ dedans pour être propre

Créer une console duckdb et requeter:
INSTALL delta;
LOAD delta;
SET s3_endpoint='http://127.0.0.1:9000';
SET s3_access_key_id='minioadmin';
SET s3_secret_access_key='minioadmin';
SET s3_use_ssl=false;
SET s3_url_style='path';
SELECT *
FROM delta_scan('s3://quant-delta-dev/delta/CRYPTO/SOLUSDT')
LIMIT 50;
