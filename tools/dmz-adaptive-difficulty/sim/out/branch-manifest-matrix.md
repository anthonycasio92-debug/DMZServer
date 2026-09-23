# Branch manifest matrix

Owner-request markers from [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).
✓ = present at branch tip · ✗ = missing or regressed

| Branch | LM ver | prestige-held-need | prestige-veteran-gate | prestige-gui-need | prestige-no-cap-chest | skillcheck-no-bypass | skillcheck-plugin-gate | rival-challenge-pending | rival-challenge-backend | rival-chest-challenge-pending | rival-declare-decide | spar-pending-decide | spar-dojo-war-decide | spar-no-end-session | cnpc-defer-reopen | manifest-doc | deploy-gate | build-prestige-jar-overlay | manifest-javap-prestige | skillcheck-no-redundant-ui | admin-no-cnpc-import-chest | admin-no-cnpc-import-cnpc | cnpc-flash-widget-ids | rival-tp-copy | gui-tp-audit-script | difficulty-tier-personal-gate | rival-challenge-persist | rival-challenge-reopen | dojo-war-offline | build-backend-ship-overlay | score |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **main** | 4.5.79 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 29/29 |
| main | 4.5.79 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 29/29 |
| cursor/admin-remove-cnpc-import-c766 | 4.5.78 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 25/29 |
| cursor/difficulty-tier-personal-gate-c766 | 4.5.78 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 25/29 |
| cursor/lm-branch-matrix-c766 | 4.5.78 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 25/29 |
| cursor/prestige-jar-overlay-c766 | 4.5.77 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | 24/29 |
| cursor/rival-dojo-gui-polish-c766 | 4.5.79 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 29/29 |
| cursor/skillcheck-redundant-button-c766 | 4.5.77 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | 24/29 |

## Interpretation

- **Best tip:** `main` (29/29, LM 4.5.79)
- **`main` matches best** — safe canonical for deploy.

## Branch regressions (vs best tip)

- `origin/cursor/admin-remove-cnpc-import-c766` regressed: missing ['rival-challenge-persist', 'rival-challenge-reopen', 'dojo-war-offline', 'build-backend-ship-overlay'] (present on `main`)
- `origin/cursor/difficulty-tier-personal-gate-c766` regressed: missing ['rival-challenge-persist', 'rival-challenge-reopen', 'dojo-war-offline', 'build-backend-ship-overlay'] (present on `main`)
- `origin/cursor/lm-branch-matrix-c766` regressed: missing ['rival-challenge-persist', 'rival-challenge-reopen', 'dojo-war-offline', 'build-backend-ship-overlay'] (present on `main`)
- `origin/cursor/prestige-jar-overlay-c766` regressed: missing ['difficulty-tier-personal-gate', 'rival-challenge-persist', 'rival-challenge-reopen', 'dojo-war-offline', 'build-backend-ship-overlay'] (present on `main`)
- `origin/cursor/skillcheck-redundant-button-c766` regressed: missing ['difficulty-tier-personal-gate', 'rival-challenge-persist', 'rival-challenge-reopen', 'dojo-war-offline', 'build-backend-ship-overlay'] (present on `main`)

## Extra markers on feature branches (not on main)

- None — merge feature branches into `main` if version is ahead.
