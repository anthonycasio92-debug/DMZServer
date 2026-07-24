# Mohist M1: animation but no damage

## Remaining cause (after 2.9.0)

DMZ sends `MeleeAnimationS2C` **before** iterating the client’s entity ID list.
After a world change the client often sends **empty or stale IDs** that don’t resolve
on the server → swing plays, `ServerPlayer.attack` never runs. Death fixes it because
the client gets a fresh entity/view.

## Fix v2.10.0

`mods/dmz_mohist_melee_fix-2.10.0.jar`

https://github.com/anthonycasio92-debug/DMZServer/raw/cursor/dragonminez-fresh-setup-c766/mods/dmz_mohist_melee_fix-2.10.0.jar

- If a melee packet produces no LivingHurt, **server-scan** for a valid target in front and attack
- Keeps 2.9.0: clamp >64 entity IDs, Shurui raid teleport mixin, respawn-like STR recovery
- Logs `melee fallback HIT` when rescue lands (check latest.log)

### Install

1. Only **2.10.0** in `mods/`
2. Delete older `dmz_mohist_melee_fix-*.jar`
3. Restart — log: `v2.10.0` + `SELFTEST PASS`
