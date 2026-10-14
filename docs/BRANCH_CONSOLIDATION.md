# Branch consolidation

## 2026-10-14 — KaiPath 1.3.17 / LM 4.6.155 Ultra bridge

Merged into **`main`** and deleted:

| Branch | What landed |
|--------|-------------|
| `cursor/ultra-bridge-fixedfooter-fac2` | Kit `UltraBridgeFooter` + `FIXED_FOOTER.md`, advertise `fixedFooter`/`portraitFrame`, fail-open chrome fixtures |
| `cursor/ultra-fixedfooter-1317-fac2` | LM **4.6.155**: `FEATURE_FIXED_FOOTER` shipped on KaiPath **1.3.17**, `PENDING_NEXT` empty, recommend 1.3.17 |

Live already has `LegacyMechanics-4.6.155.jar` (restart if not yet loaded). No remaining `origin/cursor/*` after this consolidation.

**Work from here:** branch off `main` only (`cursor/<topic>-fac2`).

## 2026-10-11 — Legacy Mechanics 4.6.93 main line

Native KubeJS hot-path ports and the Combat / `hot-path.json` staff tab are on **`main`** as **LegacyMechanics 4.6.93**.

| Area | What landed on main |
|------|---------------------|
| LegacyMechanics | 4.6.93 jar. Weapon bonus, heal caps, capsules, silk/silky, heartstop, pothala, spawners, and race-lock clear run in-mod. RES is cleared with other weapon bonus stats. |
| KubeJS | Former tick / item-spawn hot-path scripts removed from `kubejs/` (owned by LM). |
| Config | `config/legacymechanics/hot-path.json` + Combat tab in the config editor. |
| Left open | `cursor/ai-relay-c766` stays. Do not merge or delete it. |

**Work from here:** branch off `main` only (`cursor/<topic>-c766`).

Ship **4.6.93** to the live Forge `mods/` folder and restart. `/lm admin reload` does not load a new jar.

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
