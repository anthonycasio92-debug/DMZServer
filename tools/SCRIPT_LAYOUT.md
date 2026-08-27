# Script layout (DMZServer)

## Canonical live paths

```
kubejs/
  startup_scripts/     # Forge-level hooks (full restart required)
  server_scripts/      # /kubejs reload server_scripts
  client_scripts/
  data/                # datapack JSON overrides

customnpcs/scripts/    # Global Player + NPC scripts (CNPC UI)
  player_scripts.json  # recommended Global Player enable list

forge_scripts.json     # MUST be empty + ScriptEnabled 0

uploads/scripts/       # CMI aliases + DMZ JSON configs only (no .js mirrors)
```

## Global Player enable list

`customnpcs/scripts/player_scripts.json` enables the non-conflicting set:

- Rival + Sparring + Meditation + Flight + SprintJump + Yardrat (combined)
- Fabled Sync (not the old Prestige Sync trio)
- DMZ Fabled Bridge (not Energy / Stat Screen split)
- End Dimension Strength (own tab)
- Race Lock, Class Permission, ShadowDummyLimiter, etc.

**Not** in the global list (attach on NPCs / enable only if needed):

- `Prestige NPC.js`, `SkillUnlockNPC.js`, `Disable End Portals.js`

## KubeJS live (no doubles)

| Keep | Do not also run |
|------|-----------------|
| `apotheosis_balance.js` | archived `apotheosis_armor_health_nerf` |
| `capsule_disable.js` | archived blueprint helpers / remove_existing_blueprints |
| `dmz_actual_attack_damage.js` | datapack zip in `archive/reference/` |
| `dmz_weapon_bonus_scale.js` | CNPC `dmzweaponbonusscale.js` |
| `dungeon_clone_ki_fix.js` | CNPC dungeon forge script |
| `shadow_dummy_protect_hook.js` | CNPC ShadowDummyForgeProtect |
| `shurui_spawner_bridges.js` | any forge_scripts content |
| `building_tp.js` + `building_tp_place.js` | OK together |
| `remove_silk_touch.js` + `tinkers_silky_disable.js` | OK (different systems) |
| `lifesteal_cap.js` + `healing_received_cap_hook.js` | OK with apotheosis_balance |

## Deploy

1. Copy `kubejs/` and `customnpcs/scripts/` to the server world
2. Replace world `customnpcs/forge_scripts.json` with repo `forge_scripts.json`
3. Copy `customnpcs/scripts/player_scripts.json` into the world (or match tabs in CNPC UI)
4. Full restart (startup hooks) + `/kubejs reload server_scripts`
5. Reload CMI aliases from `uploads/scripts/*.yml`

Superseded files: `archive/` (see `archive/README.md`).
