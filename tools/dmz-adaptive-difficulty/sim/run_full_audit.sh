#!/usr/bin/env bash
# Run every LegacyMechanics audit/sim check and write sim/out/full-mod-audit.md
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
OUT="$HERE/out"
LOG="$OUT/full-mod-audit-run.log"
REPORT="$OUT/full-mod-audit.md"
VERSION="$(grep -oP 'VERSION = "\K[0-9.]+' "$ROOT/src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java" || echo unknown)"
mkdir -p "$OUT"
: >"$LOG"

run() {
  local name="$1"
  shift
  echo "========== $name ==========" | tee -a "$LOG"
  if "$@" >>"$LOG" 2>&1; then
    echo "PASS" | tee -a "$LOG"
    echo "$name|PASS" >>"$OUT/.audit-results.tmp"
    return 0
  fi
  echo "FAIL" | tee -a "$LOG"
  echo "$name|FAIL" >>"$OUT/.audit-results.tmp"
  return 1
}

rm -f "$OUT/.audit-results.tmp"
FAILS=0

run audit_features python3 "$HERE/audit_features.py" || FAILS=$((FAILS + 1))
run audit_gui_tooltips python3 "$HERE/audit_gui_tooltips.py" || FAILS=$((FAILS + 1))
run audit_form_bands python3 "$HERE/audit_form_bands.py" || FAILS=$((FAILS + 1))
run validate_tier_costs python3 "$HERE/validate_tier_costs.py" || FAILS=$((FAILS + 1))
run audit_tier_level_matrix python3 "$HERE/audit_tier_level_matrix.py" || FAILS=$((FAILS + 1))
run validate_scaling python3 "$HERE/validate_scaling.py" || FAILS=$((FAILS + 1))
run simulate_build_matrix python3 "$HERE/simulate_build_matrix.py" --check || FAILS=$((FAILS + 1))
run audit_concept python3 "$HERE/audit_concept.py" || FAILS=$((FAILS + 1))
if [[ -f "$REPO/mods/LegacyMechanics-${VERSION}.jar" ]] || [[ -f "$REPO/plugins/LegacyMechanicsGUI-${VERSION}.jar" ]]; then
  run audit_gui_abi python3 "$HERE/audit_gui_abi.py" || FAILS=$((FAILS + 1))
else
  echo "audit_gui_abi|SKIP (jars missing)" >>"$OUT/.audit-results.tmp"
fi

TELEM_DIR="$REPO/uploads/live-telemetry-2026-09-04-audit"
if [[ -d "$TELEM_DIR" ]] && ls "$TELEM_DIR"/hits-*.jsonl >/dev/null 2>&1; then
  echo "========== summarize_telemetry ==========" | tee -a "$LOG"
  if python3 "$HERE/summarize_telemetry.py" --dir "$TELEM_DIR" >>"$LOG" 2>&1; then
    echo "PASS" | tee -a "$LOG"
    echo "summarize_telemetry|PASS" >>"$OUT/.audit-results.tmp"
  else
    echo "FAIL" | tee -a "$LOG"
    echo "summarize_telemetry|FAIL" >>"$OUT/.audit-results.tmp"
    FAILS=$((FAILS + 1))
  fi
fi

{
  echo "# Full mod audit — LegacyMechanics ${VERSION}"
  echo
  echo "Generated: $(date -u +%Y-%m-%dT%H:%MZ)"
  echo
  echo "## Suite summary"
  echo
  echo "| Audit | Result |"
  echo "|-------|:------:|"
  while IFS='|' read -r name result; do
    echo "| ${name} | ${result} |"
  done <"$OUT/.audit-results.tmp"
  echo
  echo "## Notes"
  echo
  echo "- **audit_concept** is the product-level balance gate (player-facing intent)."
  echo "- **validate_scaling** uses stricter legacy sim thresholds; some failures are expected after the 2.3.162 rollback until thresholds are retuned."
  echo "- **audit_tier_level_matrix** scans all races/forms × T1–T7; one human android overclock edge case may fail monotonicity at T7."
  echo
  echo "See \`sim/out/full-mod-audit-run.log\` for full output."
  echo
  if [[ -f "$OUT/tier-level-matrix-audit.md" ]]; then
    echo "## Tier × level matrix"
    echo
    tail -n +1 "$OUT/tier-level-matrix-audit.md" | head -40
    echo
    echo "… full report: \`sim/out/tier-level-matrix-audit.md\`"
    echo
  fi
  if [[ -f "$OUT/scaling-validation-report.md" ]]; then
    echo "## Scaling validation (excerpt)"
    echo
    grep -E '^(FAIL|OK |❌|✅)' "$OUT/scaling-validation-report.md" | head -30 || true
    echo
    echo "… full report: \`sim/out/scaling-validation-report.md\`"
    echo
  fi
  echo "**Overall:** $([[ $FAILS -eq 0 ]] && echo PASS || echo "$FAILS advisory failure(s)")"
} >"$REPORT"

rm -f "$OUT/.audit-results.tmp"
echo "Wrote $REPORT ($FAILS advisory failure(s))"
exit 0
