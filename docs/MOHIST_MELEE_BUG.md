# Mohist M1: animation but no damage

## Remaining cause

DMZ sends `MeleeAnimationS2C` **before** iterating the client’s entity ID list.
After a world change the client often sends **empty or stale IDs** that don’t resolve
(or resolve to the wrong entity) → swing plays, `ServerPlayer.attack` never hits the
mob in front. Death fixes it because the client gets a fresh entity/view.

## Fix v2.11.0

`mods/dmz_mohist_melee_fix-2.11.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.11.0.jar

- If a melee packet produces no LivingHurt, **server-scan** for a valid target in front and attack
- If packet IDs resolve but fail (stale / out of range), **fall through to AABB nearby scan**
- Re-apply STR/reach/strike unlock on the rescue path; loosen front-facing to a hemisphere
- Live `FALLBACK SELFTEST` on Mohist boot (empty IDs + stale far ID)
- Keeps: clamp >64 entity IDs, Shurui raid teleport mixin, respawn-like STR recovery
- Logs `melee fallback HIT` when rescue lands (check `latest.log`)

### Install

1. Only **2.11.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.11.0` + `SELFTEST PASS` + `FALLBACK SELFTEST PASS`
