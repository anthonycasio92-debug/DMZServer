# Mohist M1: swing animation but no damage

## What players see

The melee swing / combo animation plays, but the target takes **no damage**.

## Why

DMZ sends `MeleeAnimationS2C` **before** the server range check and `ServerPlayer.attack`.

Then one of these silently drops the hit:

1. **Stale `strikeLocked`** — packet handler returns early (no animation in that path).
2. **Collapsed `ENTITY_REACH`** — animation still sent; distance check fails → no `attack()`.
3. **NaN damage amount** — `FixVanillaEvents` **cancels** `LivingAttack` / `LivingHurt` / `LivingDamage` when amount is NaN, so animation already played but hurt never applies. Common when vanilla `generic.attack_damage` or DMZ `getMeleeDamage()` goes non-finite on Mohist.

## Fix v2.5.0

`mods/dmz_mohist_melee_fix-2.5.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.5.0.jar

- Unlock stale `strikeLocked`; repair Forge reach + NaN `attack_damage` base only
- Sanitize DMZ effective attack range (including absurdly low finite ranges)
- **Do not cancel** NaN attacks in `FixVanillaEvents` — sanitize amounts instead
- Sanitize non-finite amounts after `CombatEvent.onLivingHurt`
- Raid teleport / Shurui `fullHeal` unlock (unchanged)
- **Still does not rewrite** `dragonminez:ki_damage` / melee / strike attrs
- **No damage redirects / setHealth**

### Install

1. Put **2.5.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — look for `v2.5.0` and `SELFTEST PASS`
