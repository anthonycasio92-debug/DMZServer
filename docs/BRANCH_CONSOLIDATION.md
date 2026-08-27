# Branch consolidation (2026-08-27)

All open feature work lands on **`cursor/legacymechanics-progression-c766`** (PR #28).

## Merged into PR #28

| Former branch / PR | What landed |
|--------------------|-------------|
| `rival-sparring-adaptive` (#27) | Rival/Spar Java + GUIs (superseded by 2.0) |
| `sprintjump-invested-str` (#25) | Invested STR in `SprintJumpProgression` |
| `server-fixes-consolidated` (#24) | Title/Admin/unlock gate + GhostPartyHeal + KubeJS balance pack (weapon bonus, lifesteal, Tinkers, Shurui, etc.) |
| `dmz-dino-food-balance` (#26) | `kubejs/startup_scripts/dmz_food_balance.js` |
| `title-admin-inspect-port-da6e` | Already fast-forwarded into progression |

## Intentionally not active as KubeJS

These are **disabled stubs** — logic lives in LegacyMechanics:

- `kubejs/server_scripts/building_tp.js.disabled`
- `kubejs/startup_scripts/building_tp_place.js.disabled`
- `kubejs/startup_scripts/shadow_dummy_protect_hook.js.disabled`

## Closed / deleted after consolidation

PRs #24–#27 closed as superseded. Remote branches for those + `title-admin-inspect-port-da6e` deleted.
