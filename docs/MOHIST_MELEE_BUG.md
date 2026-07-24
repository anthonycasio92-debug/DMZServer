# Mohist M1: swing animation but no damage

## What players see

The melee swing / combo animation plays, but the target takes **no damage**.

Especially: **empty hand / fist** fails after a **cross-dimension raid teleport**, while
**holding a non-DMZ item** still damages. Fist still works in the world where the raid started.

## Why

DMZ sends `MeleeAnimationS2C` **before** the server range check and `ServerPlayer.attack`.

Then:

1. **Empty hand** goes through `CombatEvent.onLivingHurt` → `getMeleeDamage()` →
   `Stats.getStrength()` (reads `dragonminez:strength` AttributeInstance base).
2. **Held item with no WeaponRegistry entry** early-returns and keeps **vanilla** damage —
   so sticks/tools still work even when STR was wiped.
3. On Mohist, **cross-dimension** teleports can reset custom attribute bases to defaults
   (`strength` default is **0**). Same-dimension raid teleports do not show the bug.
4. Collapsed `ENTITY_REACH` shrinks fist range (`attack_range = 2`) first.
5. Stale `strikeLocked` / NaN cancel in `FixVanillaEvents` can also drop hits.

`ki_damage` is **not** a player melee attribute for this path — do not rewrite it.

## Fix v2.6.0

`mods/dmz_mohist_melee_fix-2.6.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.6.0.jar

- Snapshot / restore primary stats (STR/SKP/RES/VIT/PWR/ENE) after dim/teleport/melee
- Floor effective attack range to at least the weapon’s `attack_range` (fists stay ≥ 2)
- Unlock stale `strikeLocked`; repair Forge reach + NaN `attack_damage` base only
- Skip NaN cancel in `FixVanillaEvents`; sanitize non-finite CombatEvent amounts
- **Does not write** `dragonminez:ki_damage` / melee / strike damage attrs
- **No damage redirects / setHealth**

### Install

1. Put **2.6.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — look for `v2.6.0` and `SELFTEST PASS`
