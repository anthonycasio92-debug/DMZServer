# Branch consolidation

## 2026-10-11 — Legacy Mechanics 4.6.90 main line

The cursor stack through `cursor/drop-dead-mixins-c766` is on **`main`**, together with the uploaded `LM GUI.jar` and the Noea planet-explosion coremod (`cursor/noea-explosion-no-tick-c766`).

| Area | What landed on main |
|------|---------------------|
| LegacyMechanics | 4.6.90 jar. Race-keep prices follow the configured steps. Empty unregistered pool and combat mixins are not packaged. |
| KubeJS | One `PlayerEvents.tick` handler. The spawner scan is every 10 seconds and staggered per player. |
| Noea | Planet explosions are discarded instead of ticking. |
| Left open | `cursor/ai-relay-c766` stays. Do not merge or delete it. |

**Work from here:** branch off `main` only (`cursor/<topic>-c766`).

Live Forge jar is already **4.6.90**. A restart loads it. `/lm admin reload` only refreshes JSON.

## 2026-10-11 — open cursor branch audit

| Branch | Verdict |
|--------|---------|
| `cursor/item-spawn-consolidated-34d6` | **STALE — do not merge.** Would reintroduce separate player-tick handlers removed by tick consolidation; also has hardcoded chat colors, raw `guiBackend=cnpc` in player text, and unrate-limited logging. |
| `cursor/lm-kubejs-hotpath-c766` | Fixed on branch (RES clear leak + chat cleanup → 4.6.94). |
| `cursor/ultra-server-only-34d6` | Fixed on branch (`LmScreens` no longer touches server-only `AdaptiveDifficultyMod`). |
| `cursor/kaipath-lm-ultra-bridge-34d6` | Fixed on branch (`pressProxy` gated to OnPress SAMs). |
| `cursor/ai-relay-c766` | Clean (docs-only). |
| `cursor/ultra-bridge-research-brief-34d6` | Clean (docs-only). |

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
