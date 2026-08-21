# Finding Overworld lag (scheduled function / nbt selectors)

`/schedule list` is **not** a vanilla command. Schedules live in world data.

## Confirmed culprit (level.dat ScheduledEvents)

In `AdventureWorld/level.dat` → `Data` → `ScheduledEvents`:

| Name | Role |
|------|------|
| **`dmz_actual_damage:loop`** | **THIS ONE** — 1-tick self-reschedule; Spark ~28% Server thread via `nbt=` player selectors |
| `darktimer:darktimerwarn1` | DarkTimer warning — not the tick-loop lag |

From Spark: `TimerQueue` → `dmz_actual_damage:loop` → `execute` + `@a`/`@e` **nbt=** → full `ServerPlayer` NBT save (recipe book + DMZ stats caps).

Datapack on disk: `DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip` (namespace `dmz_actual_damage`).

## Replacement (no lag)
Use KubeJS instead of the datapack:

`kubejs/server_scripts/dmz_actual_attack_damage.js`

See `tools/DMZ_ACTUAL_ATTACK_DAMAGE_KUBEJS.md`.

## Kill it (pick one)

### Live (server running)
```
/schedule clear dmz_actual_damage:loop
/datapack disable "file/DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip"
```
If the pack re-schedules on reload, keep it disabled or edit the pack’s `loop.mcfunction` to remove `schedule function dmz_actual_damage:loop 1t`.

### Offline (NBTExplorer)
Delete the `dmz_actual_damage:loop` entry under `ScheduledEvents`, save `level.dat`, then disable/remove that datapack before start so it doesn’t come back.

## What Spark showed
~30% of Server thread was:

`Overworld tick → TimerQueue → scheduled mcfunction → execute @e/@a with nbt= → full player NBT save`

Overworld only had ~49 entities. The lag is **not** a mob farm in Overworld.

## Find it (in order)

### 1) World datapacks (from your Spark report)
On the live server under `AdventureWorld/datapacks/` (or `world/datapacks/`):

- `DMZ_Tinkers_Weapon_Compat_Datapack.zip`
- `DMZ_Legacy_Balance_Datapack_v3_Actual_Damage.zip`
- `DMZ_Actual_Attack_Damage_ItemStack_Datapack_v1.zip` ← owns `dmz_actual_damage:loop`

Test:
```
/datapack list
/datapack disable <name>
```
Disable one at a time, watch `/forge tps` or Spark MSPT.

### 2) Clear scheduled functions (if you know the name)
```
/schedule clear dmz_actual_damage:loop
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
