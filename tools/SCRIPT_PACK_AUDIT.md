# Script pack audit — Dragon-Mine-Z `cursor/all-latest-scripts-dd5f`

Pulled from: https://github.com/anthonycasio92-debug/Dragon-Mine-Z/tree/cursor/all-latest-scripts-dd5f  
Compared against: this `DMZServer` branch (`cursor/server-fixes-consolidated-c766`)

**Layout reference:** `tools/SCRIPT_LAYOUT.md`  
**Archive (superseded / dumps):** `archive/README.md`

Pack claims Rival **v4.7.7** / Sparring **v3.2.8** / End Strength **v2.12.0** (this branch).

## Canonical live trees only

| Path | Role |
|------|------|
| `kubejs/` | All KubeJS (startup / server / client / data) |
| `customnpcs/scripts/` | Live CNPC scripts + `player_scripts.json` |
| `forge_scripts.json` | Empty, `ScriptEnabled: 0` |
| `uploads/scripts/` | CMI aliases + JSON configs (**no `.js` mirrors**) |

Everything else that used to conflict lives under `archive/` and is **not loaded**.

## Sync result

### Taken from upstream (now current)
- KubeJS: `apotheosis_balance.js`, `apotheosis_spawner_disable.js`, `lifesteal_cap.js`, `heartstop_*`, `dungeon_clone_ki_fix.js`, `remove_silk_touch.js`, `tinkers_necrotic_disable.js`
- KubeJS startup: `healing_received_cap_hook.js`, `shadow_dummy_protect_hook.js`, `apotheosis_spawner_chunk_hook.js`, `heartstop_disable_hook.js`
- KubeJS data: Apotheosis gem/affix JSON + Tinkers necrotic empty
- CNPC: End Strength **2.12.0**, Rival/Sparring, Flight, SprintJump, Yardrat, Fabled Sync, ShadowDummyLimiter (player), etc.

### Kept from DMZServer (server-only — do not overwrite from upstream)
| File | Why |
|------|-----|
| `customnpcs/scripts/DMZ RACE LOCK.js` | Prestige preview / carousel lock |
| `kubejs/.../race_lock_gui_sync.js` | Pairs with race lock |
| `kubejs/.../dmz_actual_attack_damage.js` | Replaces laggy datapack loop |
| `kubejs/.../dmz_weapon_bonus_scale.js` | Weapon `*` bonuses |
| `kubejs/.../building_tp*.js` | Silent Building TP |
| `kubejs/.../shurui_spawner_bridges.js` | Shurui Iron/Ki/Arrow bridges |
| `kubejs/.../tinkers_silky_disable.js` + silky datapack | Tinkers Silky |
| `kubejs/.../capsule_disable.js` | Capsule guard (sole purge authority) |

### Removed from live paths (archived)
- CNPC: Fly, ViltrumiteFly, flight suppression, Jump, Sprint, JumpSprint, YardratRace/Skills, DMZ Energy/Stat Screen, Attr Fabled bonus stats, forge EndDragon/Dungeon/Shadow
- Root `*.js` dumps
- Uploads `.js` mirrors + uploads `kubejs/` copies
- KubeJS: `apotheosis_armor_health_nerf` (dup of balance), capsule helper/purge scripts (dup of `capsule_disable`), stock `example.js`
- Attack-damage datapack zip → `archive/reference/`

---

## Conflicts — already resolved in repo layout

| Keep (live) | Archived / do not enable |
|-------------|--------------------------|
| `Flight.js` | Fly / ViltrumiteFly / flight suppression |
| `SprintJump.js` | JumpSprint / Jump / Sprint |
| `Fabled Sync.js` | Prestige Sync / Faction Sync / Value Cleaner |
| `Yardrat.js` | YardratRace / YardratSkills |
| `DMZ Fabled Bridge.js` | DMZ Energy / DMZ Stat Screen |
| `Attr Fabled Multi bonus.js` | Attr Fabled bonus stats |
| KubeJS shadow / dungeon / shurui / weapon / attack-damage | Matching CNPC forge or CNPC weapon / datapack |

### KubeJS pairs that are OK together
- `remove_silk_touch.js` + `tinkers_silky_disable.js` — different systems
- `apotheosis_balance.js` + `lifesteal_cap.js` + `healing_received_cap_hook.js`
- `apotheosis_spawner_disable.js` + `apotheosis_spawner_chunk_hook.js`
- `building_tp.js` + `building_tp_place.js`
- `capsule_disable.js` alone for capsule recipes + inventory purge

---

## Apply on live
1. Copy updated `kubejs/` + `customnpcs/scripts/` from this branch  
2. Replace world `forge_scripts.json` with empty disabled copy  
3. Copy `player_scripts.json` (or match CNPC Global Player tabs to it)  
4. **Full restart** (startup hooks)  
5. `/kubejs reload server_scripts` + `/reload`  
6. CMI: reload aliases from `uploads/scripts/*.yml`  
7. In CNPC UI: confirm no tabs still point at archived script names  

Upstream install notes: `tools/INSTALL-ALL-LATEST-SCRIPTS.md`
