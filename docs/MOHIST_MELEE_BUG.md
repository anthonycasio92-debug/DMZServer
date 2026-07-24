# Mohist M1: works in spawn world, broken in other worlds

## What players see

Melee works in the **spawn / starting world**, but after going to a **different world**
(raid arena, Namek, nether, portal, etc.) swings play and **nothing dies**.

Often empty-hand / fist is hit first; holding a non-DMZ item may still work.

## Why

1. Empty-hand damage uses `getMeleeDamage()` → `Stats.getStrength()` →
   `dragonminez:strength` attribute base.
2. Held items with no WeaponRegistry entry keep **vanilla** damage (so they still work).
3. On Mohist, **changing dimensions** can wipe custom attribute bases to 0 *after* the
   dim-change event — so a one-shot repair on the event is not enough.
4. Fist `attack_range = 2` also fails first when `ENTITY_REACH` collapses.
5. `ki_damage` is not this player melee path — do not rewrite it.

## Fix v2.7.0

`mods/dmz_mohist_melee_fix-2.7.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.7.0.jar

- Snapshot primaries in spawn world; restore after any world change
- **Read-side fallback** on `Stats.getStrength()` so fist damage still uses the snapshot
  even if Mohist re-wipes the attribute
- Delayed follow-ups at +5 / +20 / +60 / +100 ticks after dim/teleport
- Hook `changeDimension` + `m_8999_` + Forge dim event
- Fist range floor; reach / strikeLocked / NaN fixes from earlier versions
- **Does not write** `ki_damage`

### Install

1. Put **2.7.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — look for `v2.7.0` and `SELFTEST PASS`
