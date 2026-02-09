#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/tests/out"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

javac -d "$OUT_DIR" \
  $(find "$ROOT_DIR/src/main/java" -name "*.java") \
  $(find "$ROOT_DIR/tests/java" -name "*.java")

java -cp "$OUT_DIR" comp370.srms.MessageSerializerTest
