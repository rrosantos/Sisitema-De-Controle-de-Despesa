#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH=""
shopt -s nullglob

for candidate in "$BASE_DIR"/sistema-controle-despesas*-app.jar "$BASE_DIR"/target/sistema-controle-despesas*-app.jar; do
  if [[ -f "$candidate" ]]; then
    JAR_PATH="$candidate"
    break
  fi
done

if [[ -z "$JAR_PATH" ]]; then
  echo "Jar executavel nao encontrado. Gere com: mvn clean package" >&2
  exit 1
fi

cd "$BASE_DIR"
exec java -jar "$JAR_PATH" "$@"
