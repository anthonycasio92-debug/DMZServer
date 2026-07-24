# dmz_mohist_melee_fix

Forge 1.20.1 mixin mod for Mohist + DragonMineZ.

**v1.0.5** syncs cleared charge/block flags to the client, flushes stuck upswing state, and soft-recreates the player once on Mohist join so players no longer need to suicide to restore M1.

Jar: `mods/dmz_mohist_melee_fix-1.0.5.jar` (install on **server and clients**).

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

## Self-test

Auto-runs on Mohist. Expect:

```text
SELFTEST PASS lockedBefore=true lockedAfter=false ... delta=1.0
```
