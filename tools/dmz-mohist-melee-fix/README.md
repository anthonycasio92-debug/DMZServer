# dmz_mohist_melee_fix

Tiny Forge 1.20.1 mixin mod. Redirects DragonMineZ `CombatAttackRequestC2S` from `ServerPlayer.attack` to `LivingEntity.hurt` so Mohist melee works without a death first.

Built jar is committed at `mods/dmz_mohist_melee_fix-1.0.0.jar`.

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

Requires local `libraries/` (Forge + `server-*-srg.jar`) and `mods/dragonminez-2.1.3.jar` from this pack.

## Optional boot self-test

Add JVM arg `-Ddmz.melee.fix.selftest=true`, then restart. On `ServerStarted` the mod:

1. Reflectively invokes the applied mixin redirect on a FakePlayer → zombie
2. Confirms HP drops

Look for:

```text
[dmz_mohist_melee_fix] SELFTEST PASS mixinRedirect=true ...
```

Leave this flag off on production hosts.
