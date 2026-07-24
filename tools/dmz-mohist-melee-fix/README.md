# dmz_mohist_melee_fix

Forge 1.20.1 mixin mod for Mohist + DragonMineZ.

**v1.0.4** clears stale `strikeLocked` (which made DMZ drop all M1 packets) and applies melee/ki/strike damage via LivingHurt + setHealth.

Jar: `mods/dmz_mohist_melee_fix-1.0.4.jar` (server-side only).

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

## Self-test

Auto-runs on Mohist. Expect:

```text
SELFTEST PASS lockedBefore=true lockedAfter=false ... delta=1.0
```
