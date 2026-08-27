# Shurui Advanced Spawner bridges → KubeJS

Port of the three CustomNPCs **Global Forge Scripts** in `forge_scripts.json` to:

`kubejs/startup_scripts/shurui_spawner_bridges.js`

| Original forge script | KubeJS behavior |
|----------------------|-----------------|
| Iron's Spellbooks mob stat bridge | Shurui mobs get Iron's SPELL_POWER / SUMMON_DAMAGE / MAX_MANA from Ki |
| Native DMZ Ki Damage bridge | Native `DBSagasEntity` (non-SDU) get `setKiBlastDamage` from spawner Ki |
| Direct bow/arrow damage bridge | Arrows from Shurui mobs holding bow/crossbow get base damage scaled by ATTACK_DAMAGE |

## Install
1. Deploy the startup script.
2. **Full server restart** (ForgeEvents only register on first load).
3. **Disable CNPC forge scripts** so nothing double-applies:
   - In `customnpcs` / forge scripts UI: turn Global Forge Scripts off, **or**
   - Set `forge_scripts.json` → `"ScriptEnabled": 0b`
4. Leave Shurui Advanced Spawners unchanged (`sdd_spawner` NBT still required).

## Notes
- Same markers: `sdd_irons_bridge_complete`, `sdd_native_dmz_ki_bridge_complete`
- Native Ki bridge still skips `loadedFromDisk()` (chunk reloads)
- Debug flags default **false** (`DEBUG_IRON`, `DEBUG_NATIVE_KI`, `DEBUG_ARROW`) — set true temporarily if needed
- CNPC wrappers (`IEntity`, `IWorld`, `NpcAPI`) replaced with Forge `getPersistentData()` + `Level.getBlockEntity`
