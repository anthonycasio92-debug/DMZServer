# Script pack audit — Dragon-Mine-Z `cursor/all-latest-scripts-dd5f`

Pulled from: https://github.com/anthonycasio92-debug/Dragon-Mine-Z/tree/cursor/all-latest-scripts-dd5f  
Compared against: this `DMZServer` branch (`cursor/server-fixes-consolidated-c766`)

Pack claims Rival **v4.7.7** / Sparring **v3.2.8** / End Strength **v2.11.0**.

## Sync result (done on this branch)

### Taken from upstream (now current)
- KubeJS: `apotheosis_balance.js`, `apotheosis_spawner_disable.js`, `lifesteal_cap.js`, `heartstop_*`, `dungeon_clone_ki_fix.js`, `remove_silk_touch.js`, `tinkers_necrotic_disable.js`
- KubeJS startup: `healing_received_cap_hook.js`, `shadow_dummy_protect_hook.js`, `apotheosis_spawner_chunk_hook.js`, `heartstop_disable_hook.js`
- KubeJS data: Apotheosis gem/affix JSON + Tinkers necrotic empty
- CNPC: End Strength **2.11.0**, Rival/Sparring pack, Flight/SprintJump, ShadowDummy*, TP IS SP Fabled, EndDragon* (deprecated forge notes)
- `forge_scripts.json` → **empty + ScriptEnabled 0** (required; stops CNPC forge NPE spam)

### Kept from DMZServer (newer / server-only — do not overwrite)
| File | Why |
|------|-----|
| `customnpcs/scripts/DMZ RACE LOCK.js` | Prestige preview / carousel lock (upstream lacks this) |
| `kubejs/.../race_lock_gui_sync.js` | Pairs with race lock |
| `kubejs/.../dmz_actual_attack_damage.js` | Replaces laggy datapack loop |
| `kubejs/.../dmz_weapon_bonus_scale.js` | Weapon `*` bonuses |
| `kubejs/.../building_tp*.js` | Silent Building TP |
| `kubejs/.../shurui_spawner_bridges.js` | Shurui Iron/Ki/Arrow bridges (was forge_scripts) |
| `kubejs/.../tinkers_silky_disable.js` + silky datapack | Tinkers Silky (different from silk-touch enchant strip) |
| `kubejs/.../capsule_disable.js` | Already matched upstream |

### Disabled as conflicting
- `kubejs/server_scripts/examples/Apotheoisis_nerf.js.disabled` — old duplicate of `apotheosis_balance.js` (both used `highPriorityData`)

---

## Conflicts / double-load risks (live CNPC tabs)

Enable **only one** of each group:

| Keep | Disable / remove from CNPC tabs |
|------|----------------------------------|
| `Flight.js` (combined fly + suppression) | `Fly.js`, `ViltrumiteFly.js`, **`flight suppression.js`** (still in folder; do not also enable) |
| `SprintJump.js` | `JumpSprint.js`, `Jump.js`, `Sprint.js` |
| `Fabled Sync.js` | `Prestige Sync Fabled.js`, `Fabled Prestige Faction Sync.js` (repo root leftovers) |
| KubeJS `shadow_dummy_protect_hook.js` | CNPC forge `ShadowDummyForgeProtect.js` (file kept as deprecated fallback only) |
| KubeJS `dungeon_clone_ki_fix.js` | CNPC forge `Dungeon-Clone-Ki-Fix-Forge.js` |
| End aliases `asOp!` + End Strength player script | CNPC forge `EndDragon-Forge-Trigger.js` (deprecated) |
| **Global Forge Scripts OFF** | Any forge tabs (Shurui bridges are KubeJS now) |

### KubeJS pairs that are OK together
- `remove_silk_touch.js` (vanilla Silk Touch enchant) **+** `tinkers_silky_disable.js` (Tinkers Silky ability) — different systems
- `apotheosis_balance.js` **+** `lifesteal_cap.js` **+** `healing_received_cap_hook.js` — intended stack
- `apotheosis_spawner_disable.js` **+** `apotheosis_spawner_chunk_hook.js`
- `capsule_disable.js` is the real capsule guard; `disable_capsule_blueprints.js` / `disable_overpowered_capsules.js` are recipe-only helpers (OK)

### Mild overlap (review later)
- `apotheosis_armor_health_nerf.js` only nerfs Blessed armor health; `apotheosis_balance.js` already scales many health affixes. Safe if values agree; redundant otherwise.

---

## Apply on live
1. Copy updated `kubejs/` + `customnpcs/scripts/` from this branch  
2. Replace world `forge_scripts.json` with empty disabled copy  
3. **Full restart** (startup hooks)  
4. `/kubejs reload server_scripts` + `/reload`  
5. In CNPC UI: disable forge scripts; disable duplicate player tabs listed above  
6. CMI: reload Rival / Sparring / EndDragon aliases from `uploads/scripts/`

Upstream install notes: `tools/INSTALL-ALL-LATEST-SCRIPTS.md`
