# Create kinetics + TPS (live deploy)

## Files
- `create-server.toml` → `AdventureWorld/serverconfig/create-server.toml` and `build/serverconfig/create-server.toml`
  - `disableStress = true` — networks no longer freeze when overstressed
  - `kineticValidationFrequency = 1200` — reduces Mohist kinetic detach (was resetting to 60)
- Also copy to `defaultconfigs/create-server.toml` so new worlds inherit it
- `../config/logbegone.toml` → `config/logbegone.toml` (filters `[DMZ-Points]` console spam)
- Disable incompatible: `mods/Create-Better-Storages-*.jar` → `.disabled` (Create 6 mixin miss)

## server.properties (TPS)
- `view-distance=8`
- `simulation-distance=8`

## Requires full server restart
Create serverconfig does not hot-reload.
