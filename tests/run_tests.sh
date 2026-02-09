#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/tests/out"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -d "$OUT_DIR" \
  $(find "$ROOT_DIR/src/main/java" -name "*.java") \
  $(find "$ROOT_DIR/tests/java" -name "*.java")

find "$ROOT_DIR/tests/java" -name "*Test.java" | sort | while IFS= read -r test_file; do
  class_name="${test_file#"$ROOT_DIR/tests/java/"}"
  class_name="${class_name%.java}"
  class_name="${class_name//\//.}"

  echo "Running $class_name"
  java -cp "$OUT_DIR" "$class_name"
done
