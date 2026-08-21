# Finding Overworld lag (scheduled function / nbt selectors)

`/schedule list` is **not** a vanilla command. Schedules live in world data.

## What Spark showed
~30% of Server thread was:

`Overworld tick → TimerQueue → scheduled mcfunction → execute @e/@a with nbt= → full player NBT save`

Overworld only had ~49 entities. The lag is **not** a mob farm in Overworld.

## Find it (in order)

### 1) World datapacks (from your Spark report)
On the live server under `AdventureWorld/datapacks/` (or `world/datapacks/`):

- `DMZ_Tinkers_Weapon_Compat_Datapack.zip`
- `DMZ_Legacy_Balance_Datapack_v3_Actual_Damage.zip`
- `DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip`

Test:
```
/datapack list
/datapack disable <name>
```
Disable one at a time, watch `/forge tps` or Spark MSPT.

### 2) Clear scheduled functions (if you know the name)
```
/schedule clear namespace:function_name
```
Names come from datapack `data/*/functions/*.mcfunction` lines like:
`schedule function namespace:foo 1t`

### 3) Search files for the expensive pattern
In every datapack zip / folder, search for:
- `schedule function`
- `nbt=`
- `@e[` / `@a[` with nbt

### 4) level.dat scheduled events
Schedules are stored in **`AdventureWorld/level.dat`** (NBT field for scheduled events / TimerQueue), not a `/schedule list` UI.
Open with NBTExplorer / Diggui and look for scheduled function resource locations.

### 5) Command blocks at spawn
`enable-command-block=true` on this server. Check spawn / force-loaded chunks for repeating command blocks using `@e[nbt=`.

## Also fixed in repo
KubeJS used to run every second:
```
kill @e[type=item,nbt={Item:{id:"capsule:capsule",...}}]
```
Those files are now recipe-only; safe purge stays in `capsule_disable.js`.
