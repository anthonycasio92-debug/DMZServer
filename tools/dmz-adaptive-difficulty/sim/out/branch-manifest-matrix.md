# Branch manifest matrix

Owner-request markers from [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).
✓ = present at branch tip · ✗ = missing or regressed

| Branch | LM ver | prestige-held-need | prestige-veteran-gate | prestige-gui-need | prestige-no-cap-chest | skillcheck-no-bypass | skillcheck-plugin-gate | rival-challenge-pending | rival-challenge-backend | rival-chest-challenge-pending | rival-declare-decide | spar-pending-decide | spar-dojo-war-decide | spar-no-end-session | cnpc-defer-reopen | manifest-doc | deploy-gate | build-prestige-jar-overlay | manifest-javap-prestige | skillcheck-no-redundant-ui | admin-no-cnpc-import-chest | admin-no-cnpc-import-cnpc | cnpc-flash-widget-ids | rival-tp-copy | gui-tp-audit-script | score |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **main** | 4.5.77 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 24/24 |
| main | 4.5.74 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | 19/24 |
| cursor/admin-remove-cnpc-import-c766 | 4.5.77 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 24/24 |
| cursor/prestige-jar-overlay-c766 | 4.5.75 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | 21/24 |
| cursor/skillcheck-redundant-button-c766 | 4.5.76 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ | ✓ | ✓ | 22/24 |

## Interpretation

- **Best tip:** `main` (24/24, LM 4.5.77)
- **`main` matches best** — safe canonical for deploy.

## Branch regressions (vs best tip)

- `origin/cursor/prestige-jar-overlay-c766` regressed: missing ['skillcheck-no-redundant-ui', 'admin-no-cnpc-import-chest', 'admin-no-cnpc-import-cnpc'] (present on `main`)
- `origin/cursor/skillcheck-redundant-button-c766` regressed: missing ['admin-no-cnpc-import-chest', 'admin-no-cnpc-import-cnpc'] (present on `main`)
- `origin/main` regressed: missing ['build-prestige-jar-overlay', 'manifest-javap-prestige', 'skillcheck-no-redundant-ui', 'admin-no-cnpc-import-chest', 'admin-no-cnpc-import-cnpc'] (present on `main`)

## Extra markers on feature branches (not on main)

- None — merge feature branches into `main` if version is ahead.
