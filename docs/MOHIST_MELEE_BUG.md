# Mohist M1: works in spawn, broken after world change (death fixes it)

## What players see

Melee works in spawn. After changing worlds it stops. **Dying and respawning fixes it.**

## Why death fixes it

Respawn creates a **new** `ServerPlayer` with a fresh `AttributeMap`, then DMZ
`PlayerEvent.Clone` → `Stats.copyFrom` rewrites STR/etc. onto that map, then
`applyHealthBonus` + `refreshDimensions` (`m_6210_`).

Dimension changes keep the **same** entity, so Mohist-wiped attributes stay broken.

## Fix v2.8.0

`mods/dmz_mohist_melee_fix-2.8.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.8.0.jar

Replays that respawn recovery on the live player after every world change / teleport:

- Rebind stats → player
- Re-apply primary stats (same as `Stats.copyFrom`)
- `StatsEvents.applyHealthBonus`
- `refreshDimensions` (`m_6210_`)
- Reach repair + combat unlock + sync
- Still no soft `PlayerList.respawn`, no `ki_damage` writes, no damage redirects

### Install

1. Only **2.8.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.8.0` + `SELFTEST PASS`
