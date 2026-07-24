# dmz_mohist_melee_fix

Forge 1.20.1 mixin mod for Mohist + DragonMineZ.

**v1.0.6** repairs corrupted Forge `ENTITY_REACH` (silent M1 misses until death), syncs charge/block flags, flushes client upswing, and soft-recreates on Mohist join.

Jar: `mods/dmz_mohist_melee_fix-1.0.6.jar` (install on **server and clients**).

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

## Self-test

Auto-runs on Mohist. Expect reach repair fields in the PASS line.
