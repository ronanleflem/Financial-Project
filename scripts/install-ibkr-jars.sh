#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
jar_dir="$repo_root/jar"

mvn install:install-file \
  -Dfile="$jar_dir/protobuf-java-4.29.3.jar" \
  -DgroupId=com.ib \
  -DartifactId=tws-api-proto \
  -Dversion=10.40 \
  -Dpackaging=jar
