# dmz_mohist_melee_fix

Tiny Forge 1.20.1 mixin mod for Mohist + DragonMineZ.

Redirects `CombatAttackRequestC2S` away from `ServerPlayer.attack` / `LivingEntity.hurt` (Bukkit bridge) to Forge `LivingHurtEvent` + `actuallyHurt`.

Built jar: `mods/dmz_mohist_melee_fix-1.0.1.jar` (server-side only).

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

Requires local `libraries/` (Forge + `server-*-srg.jar`) and `mods/dragonminez-2.1.3.jar` from this pack.

## Optional boot self-test

Add JVM arg `-Ddmz.melee.fix.selftest=true`, then restart. Look for:

```text
[dmz_mohist_melee_fix] SELFTEST PASS ...
```

Leave this flag off on production hosts.
