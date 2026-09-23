# Branch manifest matrix

Owner-request markers from [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).
✓ = present at branch tip · ✗ = missing or regressed

| Branch | LM ver | prestige-held-need | prestige-veteran-gate | prestige-gui-need | prestige-no-cap-chest | skillcheck-no-bypass | skillcheck-plugin-gate | rival-challenge-pending | rival-challenge-backend | rival-chest-challenge-pending | rival-declare-decide | spar-pending-decide | spar-dojo-war-decide | spar-no-end-session | cnpc-defer-reopen | manifest-doc | deploy-gate | build-prestige-jar-overlay | manifest-javap-prestige | skillcheck-no-redundant-ui | admin-no-cnpc-import-chest | admin-no-cnpc-import-cnpc | cnpc-flash-widget-ids | rival-tp-copy | gui-tp-audit-script | difficulty-tier-personal-gate | rival-challenge-persist | rival-challenge-reopen | dojo-war-offline | build-backend-ship-overlay | android-no-saiyan-deny | android-gui-copy | cnpc-preview-live-sync | manifest-android-row | manifest-preview-row | prestige-tier-cnpc-ids | score |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **main** | 4.5.91 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 35/35 |
| main | 4.5.91 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 35/35 |
| cursor/difficulty-menu-readability-c766 | 4.5.91 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 35/35 |
| cursor/prestige-tier-shop-cnpc-ids-c766 | 4.5.85 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 34/35 |

## Interpretation

- **Best tip:** `main` (35/35, LM 4.5.91)
- **`main` matches best** — safe canonical for deploy.

## Branch regressions (vs best tip)

- `origin/cursor/prestige-tier-shop-cnpc-ids-c766` regressed: missing ['difficulty-tier-personal-gate'] (present on `main`)

## Extra markers on feature branches (not on main)

- None — merge feature branches into `main` if version is ahead.
