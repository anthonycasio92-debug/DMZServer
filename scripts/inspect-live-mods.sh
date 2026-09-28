#!/usr/bin/env bash
# Summarize jars from pull-live-mods.sh output: mod id, name, script/editor hints.
set -euo pipefail
DIR="${1:-}"
if [[ -z "$DIR" || ! -d "$DIR/mods" ]]; then
  echo "Usage: $0 /path/to/uploads/live-mods-YYYY-MM-DD" >&2
  exit 1
fi
OUT="$DIR/MOD-AUDIT.txt"
{
  echo "# Live mod audit $(date -u +%Y-%m-%dT%H:%MZ)"
  echo ""
  for jar in "$DIR/mods"/*.jar; do
    [[ -f "$jar" ]] || continue
    base="$(basename "$jar")"
    echo "## $base"
    jar tf "$jar" 2>/dev/null | rg -i 'mods\.toml|MANIFEST\.MF' | head -3 || true
    if jar tf "$jar" 2>/dev/null | rg -q 'META-INF/mods.toml'; then
      unzip -p "$jar" META-INF/mods.toml 2>/dev/null | rg -i 'modId|displayName|description|version' | head -20 || true
    fi
    echo "### classes (script|editor|prof|module|gui)"
    jar tf "$jar" 2>/dev/null | rg -i 'script|editor|prof|module|navigation|persist' | head -25 || true
    echo ""
  done
} | tee "$OUT"
echo "Full report: $OUT"
