# dmz_mohist_melee_fix

Forge 1.20.1 mixin mod for Mohist + DragonMineZ.

Fixes M1 breaking after cancelled ki blasts / no-PvP hits by probing Bukkit cancels, applying damage via LivingHurt + setHealth, and repairing attacker state.

Jar: `mods/dmz_mohist_melee_fix-1.0.3.jar` (server-side only).

## Rebuild

```bash
./tools/dmz-mohist-melee-fix/build.sh
```

## Optional self-test

`-Ddmz.melee.fix.selftest=true` → `SELFTEST PASS`
