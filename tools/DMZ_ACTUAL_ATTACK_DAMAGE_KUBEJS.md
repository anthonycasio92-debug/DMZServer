# DMZ Actual Attack Damage → KubeJS

Port of `DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1` to:

`kubejs/server_scripts/dmz_actual_attack_damage.js`

## Why
The datapack’s `dmz_actual_damage:loop` used `execute as @a` + **`nbt=`** selectors every 10 ticks. Spark showed that as ~28% of the Server thread (full player NBT save / recipe book / DMZ caps).

KubeJS does the same once-per-stack patch by reading mainhand/offhand via Java — no commands, no NBT selectors.

## Behavior (same as datapack)
- DMZ weapons listed → displayed Attack Damage **×0.25**
- Simply Swords / Simply More (unique, runic, Mimicry) → **×10** of installed defaults
- Writes `AttributeModifiers` + `DMZActualDamageV1` onto the ItemStack when first held
- Does not touch Tinkers or DMZ ki projectiles

## Install on live
1. Deploy `kubejs/server_scripts/dmz_actual_attack_damage.js`
2. Clear the lagging schedule and disable the datapack:
   ```
   /schedule clear dmz_actual_damage:loop
   /datapack disable "file/DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip"
   ```
   Or delete that zip from `AdventureWorld/datapacks/` and restart.
3. `/kubejs reload server_scripts` (or restart)

**Do not leave the datapack enabled** alongside this script.

## Source zip
`DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1 (1).zip` in the repo root (reference only).
