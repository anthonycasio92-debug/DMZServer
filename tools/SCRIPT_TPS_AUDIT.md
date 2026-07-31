# Script TPS audit (KubeJS + CustomNPCs)

Date: 2026-07-31  
Branch work: disable duplicate KubeJS purges; fix End Strength dragon scan order.

## Critical (fixed this pass)

### 1. KubeJS overlapping capsule purges
Four scripts were all loading from `kubejs/server_scripts/`:

| Script | Cadence | Cost |
|--------|---------|------|
| `capsule_disable.js` | every 5s / player | slot-safe inv scan (OK to keep) |
| `disable_capsule_blueprints.js` | every 1s | **4×** `clear`/`kill` NBT commands |
| `disable_overpowered_capsules.js` | every 1s | inv scan + **4×** more commands |
| `remove_existing_blueprints.js` | every 1s / player | full inv+ender scan |

**Fix:** moved the three duplicates (+ example + redundant Apotheosis writers) to
`kubejs/server_scripts/disabled/*.disabled`.

### 2. `End Dimension Strength.js` — dragon scan before throttle
`tickDragonExtraAttacks()` called `findDragons()` (`getAllEntities(-1)` + 1200-block AABB)
**every player tick in The End**, then checked a 3.2s lock.

**Fix:** check the world/player lock **before** scanning. Also eased mob scan to
3s / radius 64 (was 1.5s / 96).

### 3. `Attr Fabled Multi bonus.js` — reflection + sync spam
Every second: `getMethods()` reflection, clear all bonuses, re-apply, `NetworkHandler` sync.

**Fix:** cache Fabled/`getData` handles; skip work when attribute signature unchanged;
interval 2s.

---

## High (still enabled — watch in Spark)

| Script | Cadence | Risk |
|--------|---------|------|
| `End Dimension Strength.js` | 3s nearby scan in End | Still scans nearby entities while players are in The End |
| `Rival System.js` | ~1s (throttled) | Large script; multi-world player walks for proximity |
| `Sparring Tp System.js` | 250ms when active | Fine idle; heavier in spars |
| `flight suppression.js` + `Fly.js` + `ViltrumiteFly.js` | **every tick** | Motion clamps — expected cost while flying |
| `Prestige Sync Fabled.js` / `Universal Fabled Value Cleaner.js` / `Fabled Prestige Faction Sync.js` | ~1s each | Similar Fabled reflection pattern (not yet cached) |

`player_scripts.json` loads many global player tabs. Prefer consolidating Fabled sync
into `Fabled Sync.js` alone (file exists but is **not** listed in `player_scripts.json`).

---

## Medium / Low

| Script | Notes |
|--------|------|
| `Meditation new.js` | Throttled ms interval — OK |
| `ShadowDummyLimiter.js` | Idle/active interval — OK |
| `Global TP Boost.js` | Throttled; mostly idle when boost off |
| `DMZ RACE LOCK.js` / `DMZ Class Permission.js` | Throttled permission sync |
| `damageovertime.js` | Event-only — fine |
| KubeJS Apotheosis balance/spawner | Recipe/data only — fine |

---

## How to confirm with Spark

1. Install `mods/spark-1.10.53-forge.jar` (see `tools/SPARK.md`)
2. `/spark profiler start` under load → fight / End / fly
3. `/spark profiler stop`
4. Search report for: `kubejs`, `customnpcs`, `End Dimension`, `capsule`, `adaptivedifficulty`

## Deploy notes

- After pulling: restart so KubeJS drops the disabled files
- CNPC scripts: re-import / ensure server uses updated `customnpcs/scripts/` copies
- Do **not** re-enable the `.disabled` capsule purge scripts
