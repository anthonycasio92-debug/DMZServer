#!/usr/bin/env bash
# Delete remote cursor/* branches after consolidation onto main (requires git push access).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
if [[ "${DELETE_CURSOR_BRANCHES_CONFIRM:-}" != "YES" ]]; then
  echo "Set DELETE_CURSOR_BRANCHES_CONFIRM=YES to delete origin/cursor/* branches." >&2
  exit 1
fi
git fetch origin --prune
mapfile -t BRANCHES < <(git branch -r | sed 's|^[[:space:]]*origin/||' | rg '^cursor/' | sort -u)
if [[ ${#BRANCHES[@]} -eq 0 ]]; then
  echo "No origin/cursor/* branches."
  exit 0
fi
echo "Deleting ${#BRANCHES[@]} remote cursor branch(es)..."
for b in "${BRANCHES[@]}"; do
  git push origin --delete "$b" || echo "WARN: could not delete $b" >&2
done
echo "Done."
