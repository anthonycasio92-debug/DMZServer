# Branch consolidation

## 2026-09-22 — Legacy Mechanics 4.5.49 main line

All LM/CNPC/GUI work is on **`main`** (formerly `cursor/gui-menu-audit-c766` + open cursor PRs).

| Area | What landed on main |
|------|---------------------|
| Ki/stamina | Consolidated base jar; build must not overwrite base `mixins.json` |
| CNPC menus | Scroll/preview/layout, form preview geo, humanized copy |
| Tooltips | Menu audit, `hub.admin.*`, prestige locked keys, catalog rev 196 |
| Build | Base `LegacyMechanics-4.5.49-consolidated.jar` → output `LegacyMechanics-${VERSION}.jar` |

**Former `cursor/*` branches:** deleted after fast-forward merge (see `scripts/delete-stale-cursor-branches.sh`).

**Work from here:** branch off `main` only (`cursor/<topic>-c766`). Refresh local base after deploy: `bash scripts/pull-lm-base-jar.sh` or `bash scripts/refresh-lm-consolidated-base.sh`.

**Deploy:** live production — [`DEPLOY.md`](DEPLOY.md). Pin: **4.5.49+** forge + GUI.

## 2026-08-30 — server fixes

Historical: `cursor/server-fixes-consolidated-c766` (PR #41).

## 2026-08-27 — earlier consolidation

Historical: merged via PR #108 `cursor/consolidated-c766`.
