# Live ki/stamina reference

The **`ki-pool-2.4.115/`** directory is a **reference extract** from `LegacyMechanics-2.4.115.jar` for diffing and audits only. **`build.sh` does not overlay these `.class` files** — doing so caused Java 17 `VerifyError` on `StatsData#load` and **Invalid player data** disconnects. Runtime ki/stamina behavior comes from the matching Java sources in this repo (kept aligned with 2.4.115 semantics).

Ship line bumps `AdaptiveDifficultyMod.VERSION`, `mods.toml`, and `plugin.yml` together.

## Refresh the slice from recycle/live jar

```bash
JAR="${LM_REFERENCE_JAR:-/path/to/recycle/LegacyMechanics-2.4.115.jar}"
REF="tools/dmz-adaptive-difficulty/reference/ki-pool-2.4.115"
# same class list as build.sh overlay — extract with unzip -p "$JAR" <path> > "$REF/<path>"
```

Do not hand-edit `.class` files; re-extract from the reference jar when live ki behavior changes.
