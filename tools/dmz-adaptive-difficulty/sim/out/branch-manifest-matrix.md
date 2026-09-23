# Branch manifest matrix

Owner-request markers from [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).
✓ = present at branch tip · ✗ = missing or regressed

| Branch | LM ver | prestige-held-need | prestige-veteran-gate | prestige-gui-need | prestige-no-cap-chest | skillcheck-no-bypass | skillcheck-plugin-gate | rival-challenge-pending | rival-challenge-backend | rival-chest-challenge-pending | rival-declare-decide | spar-pending-decide | spar-dojo-war-decide | spar-no-end-session | cnpc-defer-reopen | manifest-doc | deploy-gate | score |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **cursor/rival-spar-merge-deploy-c766** | 4.5.69 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✓ | ✓ | 14/16 |
| main | 4.5.50 | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 3/16 |
| cursor/skillcheck-donator-gates-c766 | 4.5.68 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 9/16 |
| cursor/prestige-menu-need-fix-c766 | 4.5.67 | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 7/16 |
| cursor/rival-challenge-pending-c766 | 4.5.59 | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 7/16 |
| cursor/dojo-war-rankings-fix-c766 | 4.5.63 | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ | 7/16 |
| cursor/difficulty-teams-dragon-c766 | 4.5.58 | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 4/16 |
| cursor/cnpc-menu-polish-c766 | 4.5.56 | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 4/16 |
| cursor/ui-humanize-scroll-c766 | 4.5.51 | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 4/16 |
| cursor/ad-post-overhaul-stats-c766 | 4.5.51 | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | 3/16 |

## Interpretation

- **Highest score:** `origin/cursor/rival-spar-merge-deploy-c766` (14/16)
- **`main` is incomplete** (3/16) — merge `origin/cursor/rival-spar-merge-deploy-c766` (or equivalent) before the next deploy.
- **Canonical branch missing:** spar-no-end-session, cnpc-defer-reopen

## Branches with extra work not in canonical

- `origin/cursor/dojo-war-rankings-fix-c766`: has ['spar-no-end-session', 'cnpc-defer-reopen'] not in canonical (investigate merge)
